package com.azure.metricsRecorder;

import com.azure.models.Diagnostics;
import com.azure.models.StoreResult;

import java.io.FileNotFoundException;
import java.util.List;
import java.util.stream.Collectors;

public class InflightRequestsMetricsRecorder extends MetricsRecorderBase {
    private static final String METRICS_NAME = "inflightRequests";
    public static final int METRICS_PRECISION = 2;
    public static final int METRICS_MAX = Integer.MAX_VALUE;

    public InflightRequestsMetricsRecorder(String logFilePathPrefix) throws FileNotFoundException {
        super(METRICS_NAME, logFilePathPrefix, METRICS_PRECISION, METRICS_MAX);
    }

    @Override
    List<Double> getRecordValues(Diagnostics diagnostics) {
        return diagnostics.getResponseStatisticsList()
                .stream().map(storeResultWrapper ->
                        Double.valueOf(storeResultWrapper.getStoreResult().getServiceEndpointStatistics().getInflightRequests()))
                .collect(Collectors.toList());
    }
}
