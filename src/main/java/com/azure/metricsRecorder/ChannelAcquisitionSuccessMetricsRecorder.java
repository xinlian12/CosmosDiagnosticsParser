package com.azure.metricsRecorder;

import com.azure.models.Diagnostics;
import com.azure.models.StoreResult;
import com.azure.models.TransportTimelineEventName;
import org.apache.commons.lang3.StringUtils;

import java.io.FileNotFoundException;
import java.util.List;
import java.util.stream.Collectors;

public class ChannelAcquisitionSuccessMetricsRecorder extends LatencyMetricsRecorder {
    private static final TransportTimelineEventName eventName = TransportTimelineEventName.CHANNEL_ACQUISITION;

    public ChannelAcquisitionSuccessMetricsRecorder(String logFilePathPrefix) throws FileNotFoundException {
        super(eventName.getDescription() + "SuccessLatency", logFilePathPrefix);
    }

    @Override
    List<Double> getRecordValues(Diagnostics diagnostics) {

        return diagnostics
                .getResponseStatisticsList()
                .stream()
                .map(storeResultWrapper -> storeResultWrapper.getStoreResult())
                .filter(storeResult -> StringUtils.isEmpty(storeResult.getExceptionMessage()))
                .map(storeResult -> getTransportEventLatency(storeResult))
                .collect(Collectors.toList());
    }

    private double getTransportEventLatency(StoreResult storeResult) {

        return storeResult
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
}
