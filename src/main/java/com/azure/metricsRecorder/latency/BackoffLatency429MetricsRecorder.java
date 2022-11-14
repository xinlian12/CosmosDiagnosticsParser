package com.azure.metricsRecorder.latency;

import com.azure.ISummaryRecorder;
import com.azure.models.Diagnostics;
import com.azure.models.StoreResult;

import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class BackoffLatency429MetricsRecorder extends LatencyMetricsRecorder {
    private final static String METRICS_NAME = "backoff429Latency";
    private final AtomicReference<Double> maxLatency = new AtomicReference<>(Double.MIN_VALUE);
    private final ISummaryRecorder summaryRecorder;

    public BackoffLatency429MetricsRecorder(String logFilePathPrefix, ISummaryRecorder summaryRecorder) throws FileNotFoundException {
        super(METRICS_NAME, logFilePathPrefix);
        this.summaryRecorder = summaryRecorder;
    }

    @Override
    public List<Double> getRecordValues(Diagnostics diagnostics) {

        // exclude the last 429
        long total429FromDiagnostics = 0;
        double totalBackoff = 0.0;
        for (int i = 0; i < diagnostics.getResponseStatisticsList().size() - 1; i++) {
            StoreResult storeResult = diagnostics.getResponseStatisticsList().get(i).getStoreResult();
            if (storeResult.getStatusCode() == 429) {
                String responseHeaders = storeResult.getExceptionResponseHeaders();
                totalBackoff += this.getBackOffTime(responseHeaders);
                total429FromDiagnostics++;
            }
        }
        if (diagnostics.getResponseStatisticsList().get(diagnostics.getResponseStatisticsList().size() - 1).getStoreResult().getStatusCode() == 429) {
            total429FromDiagnostics++;

        }

        // check whether the retryContext has the same ammount of 429 in the diagnostics
        long total429FromRetryContext = 0;
        if (diagnostics.getRetryContext().getStatusAndSubStatusCodes() != null) {
            total429FromRetryContext = diagnostics.getRetryContext().getStatusAndSubStatusCodes()
                    .stream().filter(statusCodesArray -> statusCodesArray[0] == 429)
                    .count();
        }

        if (total429FromRetryContext != total429FromDiagnostics) {
            System.out.println("oh nooo");
        }

        this.summaryRecorder.recordBackoff429Latency(totalBackoff);
        return Arrays.asList(totalBackoff);
    }

    @Override
    public void reportStatistics(ISummaryRecorder summaryRecorder) {
        //no-op
    }

    double getBackOffTime(String responseHeaders) {
        String retryAfterHeader = Arrays.stream(responseHeaders.split(",")).filter(header -> header.contains("x-ms-retry-after-ms"))
                .findFirst()
                .get()
                .split("=")[1];

        return Math.min(Double.parseDouble(retryAfterHeader), 5000);
    }

}
