package com.azure.metricsRecorder.latency;

import com.azure.ISummaryRecorder;
import com.azure.models.Diagnostics;

import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class BackoffLatency410ByRetryContextMetricsRecorder extends LatencyMetricsRecorder {
    private final static String METRICS_NAME = "backoff410Latency";
    private final AtomicReference<Double> maxLatency = new AtomicReference<>(Double.MIN_VALUE);
    private final ISummaryRecorder summaryRecorder;

    public BackoffLatency410ByRetryContextMetricsRecorder(String logFilePathPrefix, ISummaryRecorder summaryRecorder) throws FileNotFoundException {
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
        if (diagnostics.getRetryContext() != null
        && diagnostics.getRetryContext().getStatusAndSubStatusCodes() != null) {
            List<Integer[]> retryStatusCodes = diagnostics.getRetryContext().getStatusAndSubStatusCodes();

            for (int i = 0; i < retryStatusCodes.size() - 1; i++) {
                if (retryStatusCodes.get(i)[0] == 410) {
                    if (!findFirst410) {
                        findFirst410 = true;
                    } else {
                        totalBackoff += Math.min(15000, backoffTime);
                        backoffTime *= backoffMultiplier;
                    }
                }
            }

            // handle the last status code
            // if the last response diagnostic match 410, then discard th 410 as it will contribute to the latency
            int statusCode= diagnostics.getResponseStatisticsList().get(diagnostics.getResponseStatisticsList().size() -1)
                    .getStoreResult().getStatusCode();
            if (statusCode == 410) {
                // no-op
            } else {
                totalBackoff += Math.min(15000, backoffTime);
                backoffTime *= backoffMultiplier;
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
