package com.azure.metricsRecorder;

import com.azure.ISummaryRecorder;
import com.azure.models.ActionTimeline;
import com.azure.models.AddressResolutionDiagnostics;
import com.azure.models.Diagnostics;
import com.azure.models.ExceptionCategory;
import com.azure.models.StoreResultWrapper;
import com.azure.models.TransportTimelineEventName;
import org.apache.commons.lang3.StringUtils;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class TimelineAnalysisRecorder implements IMetricsRecorder{

    private final ConcurrentHashMap<String, List<ActionTimeline>> actionTimelineHashMap;
    private final String logPrefix;

    public TimelineAnalysisRecorder(String logPrefix) throws FileNotFoundException {
        this.actionTimelineHashMap = new ConcurrentHashMap<String, List<ActionTimeline>>();
        this.logPrefix = logPrefix;
    }

    @Override
    public void recordValue(Diagnostics diagnostics) {
        String partitionKeyRangeId = diagnostics.getResponseStatisticsList()
                .stream().filter(storeResultWrapper -> storeResultWrapper.getStoreResult().getPartitionKeyRangeId() != null)
                .findFirst()
                .get()
                .getStoreResult()
                .getPartitionKeyRangeId();

        if (StringUtils.isEmpty(partitionKeyRangeId)) {
            return;
        }

        List<ActionTimeline> eventsByPkRangeId = this.actionTimelineHashMap.compute(partitionKeyRangeId, (key, actions) -> {
            if (actions == null) {
                actions = new ArrayList<>();
            }
            return actions;
        });

        for (StoreResultWrapper storeResultWrapper : diagnostics.getResponseStatisticsList()) {
            ExceptionCategory exceptionCategory = this.parseExceptionCategory(storeResultWrapper);

            String requestStartTime = storeResultWrapper.getStoreResult().getTransportRequestTimeline()
                    .stream().filter(transportEvent -> transportEvent.getEventName().equals(TransportTimelineEventName.CREATED.getDescription()))
                    .findFirst()
                    .get()
                    .getStartTimeUTC();

            String requestEndTime = storeResultWrapper.getStoreResult().getTransportRequestTimeline()
                    .stream().filter(transportEvent -> transportEvent.getEventName().equals(TransportTimelineEventName.COMPLETED.getDescription()))
                    .findFirst()
                    .get()
                    .getStartTimeUTC();

            eventsByPkRangeId.add(
                    ActionTimeline.createNewRequestActionTimeline(
                            requestStartTime,
                            requestEndTime,
                            Arrays.asList(
                                    Duration.between(Instant.parse(requestStartTime), Instant.parse(requestEndTime)).toMillis(),
                                    diagnostics.getActivityId(),
                                    storeResultWrapper.getStoreResult().getStatusCode() + ":" + storeResultWrapper.getStoreResult().getSubStatusCode(),
                                    storeResultWrapper.getStoreResult().getStorePhysicalAddress()),
                            exceptionCategory,
                            diagnostics.getLogLine()));


            if (storeResultWrapper.getStoreResult().getServiceEndpointStatistics().getCerMetrics() != null) {

                String lastActionContext = storeResultWrapper.getStoreResult().getServiceEndpointStatistics().getCerMetrics().get("lastActionableContext");
                if (StringUtils.isNotEmpty(lastActionContext)) {
                    String lastActionContextTimestamp = lastActionContext.split(",")[0].substring(1);
                    String lastUpdatedAddresses = lastActionContext.split(",")[1];
                    String lastUpdateAddressesCount = lastUpdatedAddresses.substring(0, lastUpdatedAddresses.length()-1);
                    if (Integer.parseInt(lastUpdateAddressesCount) > 0) {
                        eventsByPkRangeId.add(
                                ActionTimeline.createConnectionStateListenerActTimeline(
                                        lastActionContextTimestamp,
                                        lastActionContextTimestamp,
                                        Arrays.asList(lastUpdateAddressesCount),
                                        diagnostics.getLogLine()));
                    }
                }
            }
        }

        if (diagnostics.getAddressResolutionStatistics() != null) {
            for (String activityId : diagnostics.getAddressResolutionStatistics().keySet()) {
                AddressResolutionDiagnostics addressResolutionDiagnostics = diagnostics.getAddressResolutionStatistics().get(activityId);
                addressResolutionDiagnostics.setActivityId(activityId);
                eventsByPkRangeId.add(
                        ActionTimeline.createAddressRefreshTimeline(
                                addressResolutionDiagnostics.getStartTimeUTC(),
                                addressResolutionDiagnostics.getEndTimeUTC(),
                                Arrays.asList(
                                        addressResolutionDiagnostics.isForceRefresh(),
                                        addressResolutionDiagnostics.isForceCollectionRoutingMapRefresh(),
                                        addressResolutionDiagnostics.isInflightRequest(),
                                        diagnostics.getActivityId()),
                                diagnostics.getLogLine()
                        ));
            }
        }

    }

    private ExceptionCategory parseExceptionCategory(StoreResultWrapper storeResultWrapper) {
        String exceptionMessage = storeResultWrapper.getStoreResult().getExceptionMessage();
        if (StringUtils.isNotEmpty(exceptionMessage)) {
            ExceptionCategory errorCategory;
            if (exceptionMessage.contains("ConnectTimeoutException")) {
                errorCategory = ExceptionCategory.CONNECTION_TIMEOUT_EXCEPTION;
            } else if (exceptionMessage.contains("acquisition took longer than the configured maximum time")) {
                errorCategory = ExceptionCategory.ACQUISITION_TIMEOUT_EXCEPTION;
            } else if (
                    storeResultWrapper
                            .getStoreResult()
                            .getTransportRequestTimeline()
                            .stream()
                            .filter(transportEvent -> transportEvent.getEventName().equals(TransportTimelineEventName.DECODE.getDescription()))
                            .findFirst()
                            .get()
                            .getStartTimeUTC() == null) {
                errorCategory = ExceptionCategory.TRANSIT_TIMEOUT;

            } else if (storeResultWrapper.getStoreResult().getStatusCode() == 410) {
                errorCategory = ExceptionCategory.SERVER_410;
            } else if (storeResultWrapper.getStoreResult().getStatusCode() == 429) {
                errorCategory = ExceptionCategory.SERVER_429;
            } else {
                throw new IllegalStateException("what kind exception is this: " + exceptionMessage);
            }

            return errorCategory;
        }

        return ExceptionCategory.NONE;
    }

    @Override
    public void recordHistogramSnapshot(Instant recordTimestamp) {
        // no-op
    }

    @Override
    public void reportStatistics(ISummaryRecorder summaryRecorder) {

    }

    @Override
    public void close() throws Exception {
        for (String partitionKeyRangeId : Collections.list(this.actionTimelineHashMap.keys())) {
            List<ActionTimeline> actionsByPartition = this.actionTimelineHashMap.get(partitionKeyRangeId)
                    .stream().sorted(new Comparator<ActionTimeline>() {
                        @Override
                        public int compare(ActionTimeline o1, ActionTimeline o2) {
                            return Instant.parse(o1.getStartTime()).compareTo(Instant.parse(o2.getStartTime()));
                        }
                    }).collect(Collectors.toList());

            boolean shouldLog = false;
            ActionTimeline lastAddressRefreshTimeline = null;
            ActionTimeline lastRequestWithExceptionTimeline = null;
            for (ActionTimeline actionTimeline : actionsByPartition) {
                if (actionTimeline.getEventName().equals(ActionTimeline.REQUEST_TIMESTAMP)) {
                    if (actionTimeline.getExceptionCategory() == ExceptionCategory.ACQUISITION_TIMEOUT_EXCEPTION
                            || actionTimeline.getExceptionCategory() == ExceptionCategory.CONNECTION_TIMEOUT_EXCEPTION) {

                        if (lastRequestWithExceptionTimeline == null) {
                            lastRequestWithExceptionTimeline = actionTimeline;
                            continue;
                        }
                        if (lastAddressRefreshTimeline != null && lastAddressRefreshTimeline.getDetails().get(0) == Boolean.TRUE) {

                            //System.out.println(lastRequestWithExceptionTimeline.getDetails().get(3));
                            if (lastRequestWithExceptionTimeline.getDetails().get(3).equals(actionTimeline.getDetails().get(3))) {
                                System.out.println(lastAddressRefreshTimeline.getLogLine());
                                System.out.println(actionTimeline.getLogLine());
                                shouldLog = true;
                                break;
                            }

                        }
                    }
                } else if (actionTimeline.getEventName().equals(ActionTimeline.ADDRESS_REFRESH)) {
                    lastAddressRefreshTimeline = actionTimeline;
                } else if (actionTimeline.getEventName().equals(ActionTimeline.CONNECTION_STATE_LISTENER_ACT_TIMESTAMP)) {
                    lastAddressRefreshTimeline = null;
                }
            }

            if (shouldLog) {
                System.out.println(partitionKeyRangeId + " pattern match: " + shouldLog);
                String fileName = this.logPrefix + partitionKeyRangeId + "_timeline.log";
                try(PrintWriter printWriter  = new PrintWriter(fileName)) {
                    for (ActionTimeline actionTimeline : actionsByPartition) {
                        printWriter.write(
                                String.format(
                                        "%s|%s|%s|%s",
                                        actionTimeline.getEventName(),
                                        actionTimeline.getStartTime(),
                                        actionTimeline.getEndTime(),
                                        actionTimeline.getDetails().subList(0, Math.min(actionTimeline.getDetails().size(), 2))));
                        printWriter.write("\n");
                    }
                    printWriter.flush();
                }
            }
        }
    }
}
