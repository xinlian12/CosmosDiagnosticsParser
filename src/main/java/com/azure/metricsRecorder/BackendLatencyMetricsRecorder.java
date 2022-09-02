package com.azure.metricsRecorder;

import com.azure.models.Diagnostics;

import java.io.FileNotFoundException;
import java.util.List;
import java.util.stream.Collectors;

public class BackendLatencyMetricsRecorder extends LatencyMetricsRecorder {
    private final static String METRICS_NAME = "backendLatency";

    public BackendLatencyMetricsRecorder(String logFilePathPrefix) throws FileNotFoundException {
        super(METRICS_NAME, logFilePathPrefix);
    }

    @Override
    List<Double> getRecordValues(Diagnostics diagnostics) {
        return diagnostics
                .getResponseStatisticsList()
                .stream()
                .filter(storeResultWrapper -> storeResultWrapper.getStoreResult().getBackendLatencyInMs() != null)
                .map(storeResultWrapper -> storeResultWrapper.getStoreResult().getBackendLatencyInMs())
                .collect(Collectors.toList());

    }
}

