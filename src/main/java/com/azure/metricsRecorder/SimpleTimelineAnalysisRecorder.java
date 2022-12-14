package com.azure.metricsRecorder;

import com.azure.ISummaryRecorder;
import com.azure.common.DiagnosticsHelper;
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

public class SimpleTimelineAnalysisRecorder implements IMetricsRecorder {
    private final ConcurrentHashMap<String, List<ActionTimeline>> actionTimelineHashMap;
    private final String logPrefix;
    private final String serverFilter;

    public SimpleTimelineAnalysisRecorder(String logPrefix) throws FileNotFoundException {
        this(logPrefix, null);
    }

    public SimpleTimelineAnalysisRecorder(String logPrefix, String serverFilter) throws FileNotFoundException {
        this.actionTimelineHashMap = new ConcurrentHashMap<String, List<ActionTimeline>>();
        this.logPrefix = logPrefix;
        this.serverFilter = serverFilter;
    }


    @Override
    public void recordValue(Diagnostics diagnostics) {
        String partitionKeyRangeId = diagnostics.getResponseStatisticsList()
                .stream().filter(storeResultWrapper -> storeResultWrapper.getStoreResult().getPartitionKeyRangeId() != null)
                .findFirst()
                .get()
                .getStoreResult()
                .getPartitionKeyRangeId();

        for (StoreResultWrapper storeResultWrapper : diagnostics.getResponseStatisticsList()) {
            String serverKey = DiagnosticsHelper.getServerKey(storeResultWrapper);

            if (StringUtils.isEmpty(serverKey)) {
                continue;
            }

            if (StringUtils.isNotEmpty(this.serverFilter) &&
                    !storeResultWrapper.getStoreResult().getStorePhysicalAddress().contains(this.serverFilter)) {
                continue;
            }

            List<ActionTimeline> eventsByServerKey = this.actionTimelineHashMap.compute(serverKey, (key, actions) -> {
                if (actions == null) {
                    actions = new ArrayList<>();
                }
                return actions;
            });

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

            eventsByServerKey.add(
                    ActionTimeline.createNewRequestActionTimeline(
                            requestStartTime,
                            requestEndTime,
                            Arrays.asList(
                                    Duration.between(Instant.parse(requestStartTime), Instant.parse(requestEndTime)).toMillis(),
                                    diagnostics.getActivityId(),
                                    storeResultWrapper.getStoreResult().getStatusCode() + ":" + storeResultWrapper.getStoreResult().getSubStatusCode(),
                                    "pkRangeId:" + partitionKeyRangeId,
                                    diagnostics.getLogLine()),
                            exceptionCategory,
                            diagnostics.getLogLine()));


            if (storeResultWrapper.getStoreResult().getServiceEndpointStatistics().getCerMetrics() != null) {

                String lastActionContext = storeResultWrapper.getStoreResult().getServiceEndpointStatistics().getCerMetrics().get("lastActionableContext");
                if (StringUtils.isNotEmpty(lastActionContext)) {
                    String lastActionContextTimestamp = lastActionContext.split(",")[0].substring(1);
                    String lastUpdatedAddresses = lastActionContext.split(",")[1];
                    String lastUpdateAddressesCount = lastUpdatedAddresses.substring(0, lastUpdatedAddresses.length()-1);
                    if (Integer.parseInt(lastUpdateAddressesCount) > 0) {
                        eventsByServerKey.add(
                                ActionTimeline.createConnectionStateListenerActTimeline(
                                        lastActionContextTimestamp,
                                        lastActionContextTimestamp,
                                        Arrays.asList(lastUpdateAddressesCount, diagnostics.getLogLine()),
                                        diagnostics.getLogLine()));
                    }
                }
            }
        }

//        if (diagnostics.getAddressResolutionStatistics() != null) {
//            for (String activityId : diagnostics.getAddressResolutionStatistics().keySet()) {
//                AddressResolutionDiagnostics addressResolutionDiagnostics = diagnostics.getAddressResolutionStatistics().get(activityId);
//                addressResolutionDiagnostics.setActivityId(activityId);
//                eventsByServerKey.add(
//                        ActionTimeline.createAddressRefreshTimeline(
//                                addressResolutionDiagnostics.getStartTimeUTC(),
//                                addressResolutionDiagnostics.getEndTimeUTC(),
//                                Arrays.asList(
//                                        addressResolutionDiagnostics.isForceRefresh(),
//                                        addressResolutionDiagnostics.isForceCollectionRoutingMapRefresh(),
//                                        addressResolutionDiagnostics.isInflightRequest(),
//                                        diagnostics.getActivityId(),
//                                        diagnostics.getLogLine()),
//                                diagnostics.getLogLine()
//                        ));
//            }
//        }

    }

    // TODO: duplicate logic, extract this exception out
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
                            .filter(transportEvent -> transportEvent.getEventName().equals(TransportTimelineEventName.RECEIVED.getDescription()))
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
        for (String serverKey : Collections.list(this.actionTimelineHashMap.keys())) {
            List<ActionTimeline> actionsByPartition = this.actionTimelineHashMap.get(serverKey)
                    .stream().sorted(new Comparator<ActionTimeline>() {
                        @Override
                        public int compare(ActionTimeline o1, ActionTimeline o2) {
                            return Instant.parse(o1.getStartTime()).compareTo(Instant.parse(o2.getStartTime()));
                        }
                    }).collect(Collectors.toList());

            String fileName = this.logPrefix + serverKey + "_timeline.log";
            try(PrintWriter printWriter  = new PrintWriter(fileName)) {
                for (ActionTimeline actionTimeline : actionsByPartition) {
                    printWriter.println(
                            String.format(
                                    "%s|%s|%s|%s",
                                    actionTimeline.getEventName(),
                                    actionTimeline.getStartTime(),
                                    actionTimeline.getEndTime(),
                                    actionTimeline.getDetails()));
                }
                printWriter.flush();
            }
        }
    }
}
