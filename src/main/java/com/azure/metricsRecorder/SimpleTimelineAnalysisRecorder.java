package com.azure.metricsRecorder;

import com.azure.ISummaryRecorder;
import com.azure.common.DiagnosticsHelper;
import com.azure.models.ActionTimeline;
import com.azure.models.Diagnostics;
import com.azure.models.ExceptionCategory;
import com.azure.models.StoreResultWrapper;
import com.azure.models.TransportEvent;
import com.azure.models.TransportTimelineEventName;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class SimpleTimelineAnalysisRecorder implements IMetricsRecorder {
    private final ConcurrentHashMap<String, List<ActionTimeline>> actionTimelineHashMap;
    private final ConcurrentHashMap<String, Integer> channelsPerEndpoint;
    private final String logPrefix;
    private final String serverFilter;

    private final Map<String, List<String>> recordRequestsMap = new ConcurrentHashMap<>();

    public SimpleTimelineAnalysisRecorder(String logPrefix) throws FileNotFoundException {
        this(logPrefix, null);
    }

    public SimpleTimelineAnalysisRecorder(String logPrefix, String serverFilter) throws FileNotFoundException {
        this.actionTimelineHashMap = new ConcurrentHashMap<String, List<ActionTimeline>>();
        this.channelsPerEndpoint = new ConcurrentHashMap<>();
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

            // If you want to get the total timeline, use fake serverKey "total"
            // If you want to get the total timeline by partition, then use partitionKeyRangeId instead
            // If you want to get the total timeline server, then use severKey instead
            String vmId = diagnostics.getClientCfgs().getMachineId();
            List<ActionTimeline> eventsByServerKey = this.actionTimelineHashMap.compute(vmId, (key, actions) -> {
                if (actions == null) {
                    actions = new ArrayList<>();
                 //   System.out.println(vmId);
                }
                return actions;
            });

            ExceptionCategory exceptionCategory = this.parseExceptionCategory(storeResultWrapper);

            String requestStartTime = storeResultWrapper.getStoreResult().getTransportRequestTimeline()
                    .stream().filter(transportEvent -> transportEvent.getEventName().equals(TransportTimelineEventName.CREATED.getDescription()))
                    .findFirst()
                    .get()
                    .getStartTimeUTC();

            List<TransportEvent> eventsWithStartTime = storeResultWrapper.getStoreResult().getTransportRequestTimeline()
                    .stream()
                    .filter(transportEvent -> transportEvent.getStartTimeUTC() != null)
                    .collect(Collectors.toList());

            //   .filter(transportEvent -> transportEvent.getEventName().equals(TransportTimelineEventName.COMPLETED.getDescription()))
            String requestEndTime =
                    Instant
                            .parse(eventsWithStartTime.get(eventsWithStartTime.size() - 1).getStartTimeUTC())
                            .plusMillis(eventsWithStartTime.get(eventsWithStartTime.size() - 1).getDurationInMilliSecs().longValue())
                            .toString();

            double stageTime = Double.MIN_VALUE;
            String stageName = StringUtils.EMPTY;

            for (TransportEvent transportEvent : storeResultWrapper.getStoreResult().getTransportRequestTimeline()) {
                Double timeDuration = transportEvent.getDurationInMilliSecs();
                if (timeDuration != null) {
                    if (timeDuration > stageTime) {
                        stageTime = timeDuration;
                        stageName = transportEvent.getEventName();
                    }
                }
            }

            if (recordRequestsMap.containsKey(serverKey)
                     && recordRequestsMap.get(serverKey).contains(diagnostics.getActivityId())) {
                return;
            } else {
//                if (!storeResultWrapper.getStoreResult().getStorePhysicalAddress().contains("oasis-cl-prod-cosmos-eastus2.documents.azure.com:1748")) {
//                    return;
//                }

//                boolean waitForConnectionInit = storeResultWrapper.getStoreResult().getChannelStatistics() == null ? false : storeResultWrapper.getStoreResult().getChannelStatistics().isWaitForConnectionInit();
//                if (!waitForConnectionInit) {
//                    return;
//                }

                String channelId =
                        storeResultWrapper.getStoreResult().getChannelStatistics() == null ? "" : storeResultWrapper.getStoreResult().getChannelStatistics().getChannelId();

                int totalChannels =
                        Integer.valueOf(storeResultWrapper.getStoreResult().getServiceEndpointStatistics().getAvailableChannels()) + Integer.valueOf(storeResultWrapper.getStoreResult().getServiceEndpointStatistics().getAcquiredChannels());
                String lastChannelReadTime =
                        storeResultWrapper.getStoreResult().getChannelStatistics() != null? storeResultWrapper.getStoreResult().getChannelStatistics().getLastReadTime() : "";
                eventsByServerKey.add(
                        ActionTimeline.createNewRequestActionTimeline(
                                requestStartTime,
                                requestEndTime,
                                Arrays.asList(
                                        "ChannelId: " + channelId,
                                        "ChannelIdLastReadTime:" + lastChannelReadTime,
                                        Duration.between(Instant.parse(requestStartTime), Instant.parse(requestEndTime)).toMillis(),
                                        diagnostics.getActivityId(),
                                        storeResultWrapper.getStoreResult().getStatusCode() + ":" + storeResultWrapper.getStoreResult().getSubStatusCode(),
                                        "pkRangeId:" + partitionKeyRangeId,
                                        "Stage: " + stageName,
                                        "Stage duration: " + stageTime,
                                        "inflightRequest:" + storeResultWrapper.getStoreResult().getServiceEndpointStatistics().getInflightRequests(),
                                       // "pendingRequests:" + storeResultWrapper.getStoreResult().getChannelStatistics().getPendingRequestsCount(),
                                        //"waitForChannelInit:" + storeResultWrapper.getStoreResult().getChannelStatistics().isWaitForConnectionInit(),
                                        "channelId:" + channelId,
                                        "lastReadTime:" + lastChannelReadTime,
                                        "totalChannels:" + totalChannels,
                                        //"availableChannels:" + storeResultWrapper.getStoreResult().getServiceEndpointStatistics().getAvailableChannels(),
                                      //  "acquiredChannels:" + storeResultWrapper.getStoreResult().getServiceEndpointStatistics().getAcquiredChannels(),
                                        //diagnostics.getSystemInformation().getSystemCpuLoad(),
                                        diagnostics.getLogLine()
                                ),
                                exceptionCategory,
                                diagnostics.getLogLine()));
            }

            channelsPerEndpoint.put(
                    serverKey,
                    storeResultWrapper.getStoreResult().getServiceEndpointStatistics().getAcquiredChannels() + storeResultWrapper.getStoreResult().getServiceEndpointStatistics().getAvailableChannels());


            if (storeResultWrapper.getStoreResult().getServiceEndpointStatistics().getCerMetrics() != null) {

                String lastActionContext = storeResultWrapper.getStoreResult().getServiceEndpointStatistics().getCerMetrics().get("lastActionableContext");
                if (StringUtils.isNotEmpty(lastActionContext)) {
                    String lastActionContextTimestamp = lastActionContext.split(",")[0].substring(1);
                    String lastUpdatedAddresses = lastActionContext.split(",")[1];
                    String lastUpdateAddressesCount = lastUpdatedAddresses.substring(0, lastUpdatedAddresses.length() - 1);
                    if (Integer.parseInt(lastUpdateAddressesCount) > 0) {
//                        eventsByServerKey.add(
//                                ActionTimeline.createConnectionStateListenerActTimeline(
//                                        lastActionContextTimestamp,
//                                        lastActionContextTimestamp,
//                                        Arrays.asList(lastUpdateAddressesCount, diagnostics.getLogLine()),
//                                        diagnostics.getLogLine()));
                    }
                }
            }

//            if (diagnostics.getAddressResolutionStatistics() != null) {
//                for (String activityId : diagnostics.getAddressResolutionStatistics().keySet()) {
//                    AddressResolutionDiagnostics addressResolutionDiagnostics = diagnostics.getAddressResolutionStatistics().get(activityId);
//                    addressResolutionDiagnostics.setActivityId(activityId);
//                    eventsByServerKey.add(
//                            ActionTimeline.createAddressRefreshTimeline(
//                                    addressResolutionDiagnostics.getStartTimeUTC(),
//                                    addressResolutionDiagnostics.getEndTimeUTC(),
//                                    Arrays.asList(
//                                            addressResolutionDiagnostics.isForceRefresh(),
//                                            addressResolutionDiagnostics.isForceCollectionRoutingMapRefresh(),
//                                            addressResolutionDiagnostics.isInflightRequest(),
//                                            diagnostics.getActivityId(),
//                                            diagnostics.getLogLine()),
//                                    diagnostics.getLogLine()
//                            ));
//                }
//            }

        }

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
            } else if (storeResultWrapper.getStoreResult().getSubStatusCode() == 404){
                errorCategory = ExceptionCategory.SERVER_404;
            } else {
                errorCategory = ExceptionCategory.NONE;
               // throw new IllegalStateException("what kind exception is this: " + exceptionMessage);
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

        int totalUniqueRequests = 0;
        int requestOnce = 0;

        System.out.println("Total channels: " +
                this.channelsPerEndpoint.values().stream().reduce((x,y) -> x + y));
        for (String serverKey : Collections.list(this.actionTimelineHashMap.keys())) {
            List<ActionTimeline> actionsByPartition = this.actionTimelineHashMap.get(serverKey)
                    .stream().sorted(new Comparator<ActionTimeline>() {
                        @Override
                        public int compare(ActionTimeline o1, ActionTimeline o2) {
                            return Instant.parse(o1.getStartTime()).compareTo(Instant.parse(o2.getStartTime()));
                        }
                    }).collect(Collectors.toList());

          //  totalUniqueRequests += this.recordRequestsMap.get(serverKey).size();

            if (actionsByPartition.size() == 1) {
                requestOnce++;
            }


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

        System.out.println("Total unique request " + totalUniqueRequests);
        System.out.println("Only Request once: " + requestOnce);
    }
}
