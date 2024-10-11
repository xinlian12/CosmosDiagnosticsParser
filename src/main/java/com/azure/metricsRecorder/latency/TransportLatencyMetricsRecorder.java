package com.azure.metricsRecorder.latency;

import com.azure.ISummaryRecorder;
import com.azure.models.Diagnostics;
import com.azure.models.TransportEvent;
import com.azure.models.TransportTimelineEventName;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class TransportLatencyMetricsRecorder extends LatencyMetricsRecorder {
    private final TransportTimelineEventName eventName;
    private final ISummaryRecorder summaryRecorder;
    private final boolean useGateway;

    public TransportLatencyMetricsRecorder(
            TransportTimelineEventName eventName,
            String logFilePathPrefix,
            ISummaryRecorder summaryRecorder,
            boolean useGateway) throws FileNotFoundException {
        super(eventName.getDescription() + "Latency", logFilePathPrefix);
        this.eventName = eventName;
        this.summaryRecorder = summaryRecorder;
        this.useGateway = useGateway;
    }

    @Override
    public List<Double> getRecordValues(Diagnostics diagnostics) {
        if (useGateway) {
            return getRecordValuesForGateway(diagnostics);
        }

        return getRecordValuesForDirect(diagnostics);
    }

    public List<Double> getRecordValuesForDirect(Diagnostics diagnostics) {
        List<Double> latencies = new ArrayList<>();
        double totalLatency = diagnostics.getResponseStatisticsList()
            .stream().map(storeResultWrapper -> {
                return storeResultWrapper
                    .getStoreResult()
                    .getTransportRequestTimeline()
                    .stream()
                    .filter(transportEvent ->
                        transportEvent.getEventName().equals(this.eventName.getDescription()) && transportEvent.getDurationInMilliSecs() != null)
                    .collect(Collectors.toList())
                    .stream()
                    .findFirst()
                    .get()
                    .getDurationInMilliSecs();
            })
            .reduce((x, y) -> x + y)
            .get();

        latencies.add(totalLatency);
        if (this.eventName == TransportTimelineEventName.CHANNEL_ACQUISITION) {
            this.summaryRecorder.recordChannelAcquisition(totalLatency);
        }
        if (this.eventName == TransportTimelineEventName.TRANSIT) {
            this.summaryRecorder.recordTransitLatency(totalLatency);
        }

        return latencies;
    }

    public List<Double> getRecordValuesForGateway(Diagnostics diagnostics) {
        List<Double> latencies = new ArrayList<>();
        double totalLatency = diagnostics.getGatewayStatisticsList()
            .stream().map(gatewayStatistics -> {
                Optional<TransportEvent> event =
                    gatewayStatistics
                        .getRequestTimeline()
                        .stream()
                        .filter(transportEvent ->
                            transportEvent.getEventName().equals(this.eventName.getDescription()) && transportEvent.getDurationInMilliSecs() != null)
                        .collect(Collectors.toList())
                        .stream()
                        .findFirst();
                return event.isPresent() ? event.get().getDurationInMilliSecs() : 0;
            })
            .reduce((x, y) -> x + y)
            .get();

        latencies.add(totalLatency);
        if (this.eventName == TransportTimelineEventName.CONNECTION_ACQUIRED) {
            this.summaryRecorder.recordConnectionAcquired(totalLatency);
        }

        if (this.eventName == TransportTimelineEventName.CONNECTION_CREATED) {
            this.summaryRecorder.recordConnectionCreated(totalLatency);
        }

        if (this.eventName == TransportTimelineEventName.CONNECTION_CONFIGURED) {
            this.summaryRecorder.recordConnectionConfigured(totalLatency);
        }
        if (this.eventName == TransportTimelineEventName.TRANSIT) {
            this.summaryRecorder.recordTransitLatency(totalLatency);
        }

        return latencies;
    }

    @Override
    public void reportStatistics(ISummaryRecorder summaryRecorder) {

    }
}
