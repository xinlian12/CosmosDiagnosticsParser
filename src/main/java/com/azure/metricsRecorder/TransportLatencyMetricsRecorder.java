package com.azure.metricsRecorder;

import com.azure.models.Diagnostics;
import com.azure.models.StoreResultWrapper;
import com.azure.models.TransportTimelineEventName;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
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
    List<Double> getRecordValues(Diagnostics diagnostics) {

        List<Double> latencies = new ArrayList<>();
        for (StoreResultWrapper storeResultWrapper : diagnostics.getResponseStatisticsList()) {

            double latency =
                    (storeResultWrapper
                        .getStoreResult()
                        .getTransportRequestTimeline()
                        .stream()
                        .filter(transportEvent ->
                                transportEvent.getEventName().equals(this.eventName.getDescription()) && transportEvent.getDurationInMilliSecs() != null)
                        .collect(Collectors.toList())
                        .stream()
                        .findFirst()
                        .get()
                        .getDurationInMilliSecs());

            latencies.add(latency);
        }

        return latencies;

//        double totalLatency = diagnostics.getResponseStatisticsList()
//                .stream().map(storeResultWrapper -> {
//                    return storeResultWrapper
//                            .getStoreResult()
//                            .getTransportRequestTimeline()
//                            .stream()
//                            .filter(transportEvent ->
//                                    transportEvent.getEventName().equals(this.eventName.getDescription()) && transportEvent.getDurationInMilliSecs() != null)
//                            .collect(Collectors.toList())
//                            .stream()
//                            .findFirst()
//                            .get()
//                            .getDurationInMilliSecs();
//                })
//                .reduce((x, y) -> x + y)
//                .get();
//
//        latencies.add(totalLatency);

    //    return latencies;
    }
}
