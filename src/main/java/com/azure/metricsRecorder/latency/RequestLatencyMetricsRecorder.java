package com.azure.metricsRecorder.latency;

import com.azure.ISummaryRecorder;
import com.azure.models.Diagnostics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class RequestLatencyMetricsRecorder extends LatencyMetricsRecorder {
    private static final Logger logger = LoggerFactory.getLogger(RequestLatencyMetricsRecorder.class);
    private static final String METER_NAME = "requestLatency";
    private final AtomicBoolean started = new AtomicBoolean(false);
    private final AtomicReference<Double> maxLatency = new AtomicReference<>(Double.MIN_VALUE);
    private String requestLogWithMaxLatency = null;
    private final AtomicInteger totalRequests = new AtomicInteger(0);
    private List<String> log200msLatencylogs = new ArrayList<>();
    private List<String> log500msLatencylogs = new ArrayList<>();
    private List<String> log600msLatencyLogs = new ArrayList<>();
    private List<String> log1sLatencyLogs = new ArrayList<>();


    public RequestLatencyMetricsRecorder(String logFilePathPrefix) throws FileNotFoundException {
        super(METER_NAME, logFilePathPrefix);
    }

    @Override
    public List<Double> getRecordValues(Diagnostics diagnostics) {
        // Do not use requestLatencyInMs directly as it is not accurate enough
        this.totalRequests.incrementAndGet();

        double requestLatency =
                Duration.between(Instant.parse(diagnostics.getRequestStartTimeUTC()), Instant.parse(diagnostics.getRequestEndTimeUTC())).toMillis();
        if (this.maxLatency.get() < requestLatency) {
            this.maxLatency.set(requestLatency);
            this.requestLogWithMaxLatency = diagnostics.getLogLine();
        }

        if (requestLatency > 200) {
            this.log200msLatencylogs.add(diagnostics.getLogLine());
        }
        if (requestLatency > 500) {
            this.log500msLatencylogs.add(diagnostics.getLogLine());
        }
        if (requestLatency > 600) {
            this.log600msLatencyLogs.add(diagnostics.getLogLine());
        }
        if (requestLatency > 1000) {
            this.log1sLatencyLogs.add(diagnostics.getLogLine());
        }

        return Arrays.asList(requestLatency) ;
    }

    @Override
    public void reportStatistics(ISummaryRecorder summaryRecorder) {
        System.out.println("Total Request: " + this.totalRequests.get());
        System.out.println("Max RequestLatency: " + this.maxLatency.get());
        System.out.println("Max RequestLatencyLog: " + this.requestLogWithMaxLatency);
        System.out.println("Total Requests with >200ms latency: " + this.log200msLatencylogs.size());
        System.out.println("Total Requests with >500ms latency: " + this.log500msLatencylogs.size());
        System.out.println("Total Requests with >600ms latency: " + this.log600msLatencyLogs.size());
        System.out.println("Total Requests with >1s latency: " + this.log1sLatencyLogs.size());


        summaryRecorder.getPrintWriter().println("Total Request: " + this.totalRequests.get());;
        summaryRecorder.getPrintWriter().println("Max RequestLatency: " + this.maxLatency.get());
        summaryRecorder.getPrintWriter().println("Max RequestLatencyLog: " + this.requestLogWithMaxLatency);
        summaryRecorder.getPrintWriter().flush();
        summaryRecorder.recordMaxRequestLatency(this.maxLatency.get(), this.requestLogWithMaxLatency);
        summaryRecorder.recordTotalRequests(this.totalRequests.get());
    }
}
