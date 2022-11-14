package com.azure.metricsRecorder.latency;

import com.azure.ISummaryRecorder;
import com.azure.cosmos.implementation.MetadataDiagnosticsContext;
import com.azure.models.AddressResolutionDiagnostics;
import com.azure.models.Diagnostics;
import jdk.jshell.Diag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class AddressResolutionMetricsRecorder extends LatencyMetricsRecorder {
    private static final Logger logger = LoggerFactory.getLogger(AddressResolutionMetricsRecorder.class);
    private final static String METRICS_NAME = "addressResolutionLatency";
    private final ISummaryRecorder summaryRecorder;

    public AddressResolutionMetricsRecorder(String logFilePathPrefix, ISummaryRecorder summaryRecorder) throws FileNotFoundException {
        super(METRICS_NAME, logFilePathPrefix);
        this.summaryRecorder =summaryRecorder;
    }

    @Override
    public List<Double> getRecordValues(Diagnostics diagnostics) {
        List<Double> latencies = new ArrayList<>();

        // get all address lookup diagnostics
        if (this.hasAddressResolutionRequests(diagnostics)) {
            double totalLatency =
                    diagnostics.getMetadataDiagnosticsContext()
                            .values()
                            .stream()
                            .findFirst()
                            .get()
                            .stream().filter(metadataDiagnostic -> metadataDiagnostic.getMetaDataName().equals("SERVER_ADDRESS_LOOKUP"))
                            .map(metadataDiagnostic -> Double.parseDouble(metadataDiagnostic.getDurationinMS()))
                            .reduce((x, y) -> x + y)
                            .get();

            latencies.add(totalLatency);
            summaryRecorder.recordAddressResolution(totalLatency);
        }

        return latencies;
    }

    private boolean hasAddressResolutionRequests(Diagnostics diagnostics) {
        if (diagnostics.getMetadataDiagnosticsContext() != null
                && diagnostics.getMetadataDiagnosticsContext().values() != null
                && diagnostics.getMetadataDiagnosticsContext().containsKey("metadataDiagnosticList")
                && diagnostics.getMetadataDiagnosticsContext().get("metadataDiagnosticList") != null) {
            return true;
        }
        return false;
    }

    private double getAddressResolutionLatency(AddressResolutionDiagnostics addressResolutionDiagnostics, String requestStartTime) {
        String addressRequestStartTime = addressResolutionDiagnostics.getStartTimeUTC();
        String addressRequestEndTime = addressResolutionDiagnostics.getEndTimeUTC();

        return Math.min(
                Duration.between(Instant.parse(addressRequestStartTime), Instant.parse(addressRequestEndTime)).toMillis(),
                Duration.between(Instant.parse(requestStartTime), Instant.parse(addressRequestEndTime)).toMillis());
    }

    @Override
    public void reportStatistics(ISummaryRecorder summaryRecorder) {

    }
}
