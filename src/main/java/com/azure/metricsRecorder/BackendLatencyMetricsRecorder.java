package com.azure.metricsRecorder;

import com.azure.models.Diagnostics;
import com.azure.models.StoreResult;

import java.io.FileNotFoundException;
import java.util.concurrent.atomic.AtomicInteger;

public class BackendLatencyMetricsRecorder extends LatencyMetricsRecorder {
    private final static String METRICS_NAME = "backendLatency";

    public BackendLatencyMetricsRecorder(String logFilePathPrefix) throws FileNotFoundException {
        super(METRICS_NAME, logFilePathPrefix);
    }

    @Override
    double getRecordValue(Diagnostics diagnostics) {
        StoreResult storeResult = diagnostics.getResponseStatisticsList().get(0).getStoreResult();
        return storeResult.getBackendLatencyInMs() * 1000;
    }
}

