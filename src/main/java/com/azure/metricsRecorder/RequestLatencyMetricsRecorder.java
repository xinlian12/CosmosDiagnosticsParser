package com.azure.metricsRecorder;

import com.azure.models.Diagnostics;

import java.io.FileNotFoundException;
import java.time.Duration;
import java.time.Instant;

public class RequestLatencyMetricsRecorder extends LatencyMetricsRecorder{
    private static final String METER_NAME = "requestLatency";

    public RequestLatencyMetricsRecorder(String logFilePathPrefix) throws FileNotFoundException {
        super(METER_NAME, logFilePathPrefix);
    }

    @Override
    double getRecordValue(Diagnostics diagnostics) {
        // Do not use requestLatencyInMs directly as it is not accurate enough
        return Duration.between(Instant.parse(diagnostics.getRequestStartTimeUTC()), Instant.parse(diagnostics.getRequestEndTimeUTC())).toNanos() / 1000d;
    }
}
