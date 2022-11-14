package com.azure.metricsRecorder;

import com.azure.ISummaryRecorder;
import com.azure.metricsRecorder.latency.LatencyMetricsRecorder;
import com.azure.models.Diagnostics;
import com.azure.models.StoreResult;
import com.azure.models.TransportTimelineEventName;
import org.apache.commons.lang3.StringUtils;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.List;
import java.util.stream.Collectors;

public class ChannelAcquisitionFailureMetricsRecorder extends LatencyMetricsRecorder {
    private static final TransportTimelineEventName eventName = TransportTimelineEventName.CHANNEL_ACQUISITION;

    public ChannelAcquisitionFailureMetricsRecorder(String logFilePathPrefix) throws FileNotFoundException {
        super(eventName.getDescription() + "FailureLatency", logFilePathPrefix);
    }

    @Override
    public List<Double> getRecordValues(Diagnostics diagnostics) {

        return diagnostics
                .getResponseStatisticsList()
                .stream()
                .map(storeResultWrapper -> storeResultWrapper.getStoreResult())
                .filter(storeResult -> StringUtils.isNotEmpty(storeResult.getExceptionMessage()))
                .map(storeResult -> getTransportEventLatency(storeResult))
                .collect(Collectors.toList());
    }

    private double getTransportEventLatency(StoreResult storeResult) {

        return
                storeResult
                        .getTransportRequestTimeline()
                        .stream()
                        .filter(transportEvent ->
                                transportEvent.getEventName().equals(this.eventName.getDescription())
                                        && transportEvent.getDurationInMilliSecs() != null)
                        .collect(Collectors.toList())
                        .stream()
                        .findFirst()
                        .get()
                        .getDurationInMilliSecs();
    }

    @Override
    public void reportStatistics(ISummaryRecorder summaryRecorder) {

    }
}
