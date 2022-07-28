package com.azure.metricsRecorder;

import java.io.FileNotFoundException;

public abstract class LatencyMetricsRecorder extends MetricsRecorderBase {
    public static final int METRICS_PRECISION = 4;
    public static final int METRICS_MAX_MILLI_SEC = 300000;

    public LatencyMetricsRecorder(
            String metricsName,
            String logFilePathPrefix) throws FileNotFoundException {
        super(metricsName, logFilePathPrefix, METRICS_PRECISION, METRICS_MAX_MILLI_SEC);
    }
}
