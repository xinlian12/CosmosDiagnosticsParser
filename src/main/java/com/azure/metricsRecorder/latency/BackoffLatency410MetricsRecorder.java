package com.azure.metricsRecorder.latency;

import com.azure.ISummaryRecorder;
import com.azure.models.Diagnostics;

import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class BackoffLatency410MetricsRecorder extends LatencyMetricsRecorder {
    private final static String METRICS_NAME = "backoff410Latency";
    private final AtomicReference<Double> maxLatency = new AtomicReference<>(Double.MIN_VALUE);
    private final ISummaryRecorder summaryRecorder;

    public BackoffLatency410MetricsRecorder(String logFilePathPrefix, ISummaryRecorder summaryRecorder) throws FileNotFoundException {
        super(METRICS_NAME, logFilePathPrefix);
        this.summaryRecorder = summaryRecorder;
    }

    @Override
    public List<Double> getRecordValues(Diagnostics diagnostics) {
        // SDK does not add backoff for the first retry
        double totalBackoff = 0.0;
        int initialBackoffTime = 1000;
        int backoffTime = initialBackoffTime;
        int backoffMultiplier = 2;

        boolean findFirst410 = false;
        for (int i = 0; i < diagnostics.getResponseStatisticsList().size() - 1; i++) {
            if (diagnostics.getResponseStatisticsList().get(i).getStoreResult().getStatusCode() == 410) {
                if (!findFirst410) {
                    findFirst410 = true;
                } else {
                    totalBackoff += Math.min(15000, backoffTime);
                    backoffTime *= backoffMultiplier;
                }
            }
        }

        this.summaryRecorder.recordBackoff410Latency(totalBackoff);
        return Arrays.asList(totalBackoff);
    }

    @Override
    public void reportStatistics(ISummaryRecorder summaryRecorder) {
        //no-op
    }
}
