package com.azure.metricsRecorder;

import com.azure.models.Diagnostics;
import com.azure.models.StoreResult;

import java.io.FileNotFoundException;

public class InflightRequestsMetricsRecorder extends MetricsRecorderBase {
    private static final String METRICS_NAME = "inflightRequests";
    public static final int METRICS_PRECISION = 2;
    public static final int METRICS_MAX = Integer.MAX_VALUE;

    public InflightRequestsMetricsRecorder(String logFilePathPrefix) throws FileNotFoundException {
        super(METRICS_NAME, logFilePathPrefix, METRICS_PRECISION, METRICS_MAX);
    }

    @Override
    double getRecordValue(Diagnostics diagnostics) {
        StoreResult storeResult = diagnostics.getResponseStatisticsList().get(0).getStoreResult();
        return storeResult.getServiceEndpointStatistics().getInflightRequests();
    }
}
