package com.azure.metricsRecorder;

import com.azure.models.Diagnostics;

import java.io.FileNotFoundException;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class RequestLatencyMetricsRecorder extends LatencyMetricsRecorder{
    private static final String METER_NAME = "requestLatency";
    private final AtomicBoolean started = new AtomicBoolean(false);

    public RequestLatencyMetricsRecorder(String logFilePathPrefix) throws FileNotFoundException {
        super(METER_NAME, logFilePathPrefix);
    }

    @Override
    List<Double> getRecordValues(Diagnostics diagnostics) {
        // Do not use requestLatencyInMs directly as it is not accurate enough

        if(started.compareAndSet(false, true)) {
            System.out.println(diagnostics.getLogLine());
        }
        double requestLatency =
                Duration.between(Instant.parse(diagnostics.getRequestStartTimeUTC()), Instant.parse(diagnostics.getRequestEndTimeUTC())).toMillis();

        return Arrays.asList(requestLatency) ;
    }
}
