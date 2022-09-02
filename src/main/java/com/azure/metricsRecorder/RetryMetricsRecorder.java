package com.azure.metricsRecorder;

import com.azure.models.Diagnostics;

import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.List;

public class RetryMetricsRecorder extends MetricsRecorderBase {
    private static final String METRICS_NAME = "retryCount";
    public static final int METRICS_PRECISION = 2;
    public static final int METRICS_MAX = Integer.MAX_VALUE;

    public RetryMetricsRecorder(String logFilePathPrefix) throws FileNotFoundException {
        super(METRICS_NAME, logFilePathPrefix, METRICS_PRECISION, METRICS_MAX);
    }

    @Override
    List<Double> getRecordValues(Diagnostics diagnostics) {
        return Arrays.asList(Double.valueOf(diagnostics.getResponseStatisticsList().size()));
    }
}
