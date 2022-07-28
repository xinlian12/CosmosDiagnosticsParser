package com.azure.metricsRecorder;

import com.azure.models.Diagnostics;

import java.time.Instant;

public interface IMetricsRecorder extends AutoCloseable{
    void recordValue(Diagnostics diagnostics);
    void recordHistogramSnapshot(Instant recordTimestamp);
}
