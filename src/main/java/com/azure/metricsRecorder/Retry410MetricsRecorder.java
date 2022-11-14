package com.azure.metricsRecorder;

import com.azure.ISummaryRecorder;
import com.azure.common.DiagnosticsHelper;
import com.azure.models.Diagnostics;
import com.azure.models.StoreResultWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class Retry410MetricsRecorder extends MetricsRecorderBase {
    private static final Logger logger = LoggerFactory.getLogger(Retry410MetricsRecorder.class);
    private static final String METRICS_NAME = "retryCount";
    public static final int METRICS_PRECISION = 2;
    public static final int METRICS_MAX = Integer.MAX_VALUE;

    private final AtomicInteger totalRequestsWithOneRetry = new AtomicInteger(0);
    private final AtomicInteger totalRequestsWithTwoRetry = new AtomicInteger(0);
    private final AtomicInteger totalRequestsWithMoreThanTwoRetry = new AtomicInteger(0);
    private final AtomicInteger retriesOnTheSameEndpoint = new AtomicInteger(0);

    private final AtomicInteger totalRequests = new AtomicInteger(0);

    public Retry410MetricsRecorder(String logFilePathPrefix) throws FileNotFoundException {
        super(METRICS_NAME, logFilePathPrefix, METRICS_PRECISION, METRICS_MAX);
    }

    @Override
    public List<Double> getRecordValues(Diagnostics diagnostics) {

        List<StoreResultWrapper> storeResultsWith410 =
                diagnostics
                        .getResponseStatisticsList()
                        .stream().filter(response -> response.getStoreResult().getStatusCode() == 410)
                        .collect(Collectors.toList());

        if (diagnostics.getResponseStatisticsList().size() > 3) {
            this.totalRequestsWithMoreThanTwoRetry.incrementAndGet();
        } else if (diagnostics.getResponseStatisticsList().size() == 3) {
            this.totalRequestsWithTwoRetry.incrementAndGet();
        } else if (diagnostics.getResponseStatisticsList().size() == 2) {
            this.totalRequestsWithOneRetry.incrementAndGet();
        }

        Set<String> serverKeys = new HashSet<>();
        for (StoreResultWrapper storeResultWrapper : storeResultsWith410) {
            String serverKey = DiagnosticsHelper.getServerKey(storeResultWrapper);
            if (!serverKeys.add(serverKey)) {
                this.retriesOnTheSameEndpoint.incrementAndGet();
                break;
            }
        }

        return Arrays.asList(Double.valueOf(diagnostics.getResponseStatisticsList().size()));
    }

    @Override
    public void reportStatistics(ISummaryRecorder summaryRecorder) {
        System.out.println(
                String.format("Request 410 Retry: 1 retry [%d], 2 retries [%d], >=2 retries [%d], retriesOnSameEndpoint [%d]",
                        this.totalRequestsWithOneRetry.get(),
                        this.totalRequestsWithTwoRetry.get(),
                        this.totalRequestsWithMoreThanTwoRetry.get(),
                        this.retriesOnTheSameEndpoint.get())
        );

        summaryRecorder.getPrintWriter().println( String.format("Request 410 Retry: 1 retry [%d], 2 retries [%d], >=2 retries [%d], retriesOnSameEndpoint [%d]",
                this.totalRequestsWithOneRetry.get(),
                this.totalRequestsWithTwoRetry.get(),
                this.totalRequestsWithMoreThanTwoRetry.get(),
                this.retriesOnTheSameEndpoint.get()));
        summaryRecorder.recordRetries(
                this.totalRequestsWithOneRetry.get(),
                this.totalRequestsWithTwoRetry.get(),
                this.totalRequestsWithMoreThanTwoRetry.get(),
                this.retriesOnTheSameEndpoint.get());
        summaryRecorder.getPrintWriter().flush();
    }
}
