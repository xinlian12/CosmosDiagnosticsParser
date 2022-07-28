package com.azure.metricsRecorder;

import com.azure.models.Diagnostics;
import com.azure.models.StoreResult;
import com.azure.models.TransportTimelineEventName;

import java.io.FileNotFoundException;
import java.util.stream.Collectors;

public class TransportLatencyMetricsRecorder extends LatencyMetricsRecorder {
    private final TransportTimelineEventName eventName;

    public TransportLatencyMetricsRecorder(
            TransportTimelineEventName eventName,
            String logFilePathPrefix) throws FileNotFoundException {
        super(eventName.getDescription() + "Latency", logFilePathPrefix);
        this.eventName = eventName;
    }

    @Override
    double getRecordValue(Diagnostics diagnostics) {
        StoreResult storeResult = diagnostics.getResponseStatisticsList().get(0).getStoreResult();

        return storeResult.getTransportRequestTimeline()
                .stream()
                .filter(transportEvent -> transportEvent.getEventName().equals(this.eventName.getDescription()))
                .collect(Collectors.toList())
                .stream()
                .findFirst()
                .get()
                .getDurationInMicroSec();
    }
}
