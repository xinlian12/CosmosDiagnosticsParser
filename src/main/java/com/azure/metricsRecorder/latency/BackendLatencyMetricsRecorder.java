package com.azure.metricsRecorder.latency;

import com.azure.ISummaryRecorder;
import com.azure.models.Diagnostics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class BackendLatencyMetricsRecorder extends LatencyMetricsRecorder {
    private static final Logger logger = LoggerFactory.getLogger(BackendLatencyMetricsRecorder.class);
    private final static String METRICS_NAME = "backendLatency";
    private final AtomicReference<Double> maxLatency = new AtomicReference<>(Double.MIN_VALUE);
    private final ISummaryRecorder summaryRecorder;

    public BackendLatencyMetricsRecorder(String logFilePathPrefix, ISummaryRecorder summaryRecorder) throws FileNotFoundException {
        super(METRICS_NAME, logFilePathPrefix);
        this.summaryRecorder = summaryRecorder;
    }

    @Override
    public List<Double> getRecordValues(Diagnostics diagnostics) {

        double totalLatency = diagnostics
                .getResponseStatisticsList()
                .stream()
                .filter(storeResultWrapper -> storeResultWrapper.getStoreResult().getBackendLatencyInMs() != null)
                .map(storeResultWrapper -> {
                    double backendLatency = storeResultWrapper.getStoreResult().getBackendLatencyInMs();
                    if (this.maxLatency.get() < backendLatency) {
                        this.maxLatency.set(backendLatency);
                    }
                    return backendLatency;
                })
                .reduce((x, y) -> x + y)
                .get();

        this.summaryRecorder.recordBackendLatency(totalLatency);
        return Arrays.asList(totalLatency);
    }

    @Override
    public void reportStatistics(ISummaryRecorder summaryRecorder) {
        System.out.println("Max backendLatency: " + this.maxLatency.get());
        summaryRecorder.getPrintWriter().println("Max backendLatency: " + this.maxLatency.get());
        summaryRecorder.getPrintWriter().flush();
        summaryRecorder.recordMaxBackendRequestLatency(this.maxLatency.get());
    }
}

