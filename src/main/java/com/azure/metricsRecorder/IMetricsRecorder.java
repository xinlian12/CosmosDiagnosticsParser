package com.azure.metricsRecorder;

import com.azure.ISummaryRecorder;
import com.azure.models.Diagnostics;

import java.io.PrintWriter;
import java.time.Instant;

public interface IMetricsRecorder extends AutoCloseable{
    void recordValue(Diagnostics diagnostics);
    void recordHistogramSnapshot(Instant recordTimestamp);
    void reportStatistics(ISummaryRecorder summaryRecorder);
}
