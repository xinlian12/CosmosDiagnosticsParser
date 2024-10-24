package com.azure.utils;

import org.HdrHistogram.ConcurrentDoubleHistogram;

import java.io.PrintWriter;
import java.time.Instant;
import java.util.List;

public class CsvFileUtils {
    private static final String TIME = "t";
    private static final String COUNT = "count";
    private static final String MAX = "max";
    private static final String MEAN = "mean";
    private static final String MIN = "min";
    private static final String STDDEV = "stddev";
    private static final String P50 = "p50";
    private static final String P75 = "p75";
    private static final String P95 = "p95";
    private static final String P99 = "p99";
    private static final String P999 = "p999";
    private static final String P9999 = "p9999";
    private static final String PK_RANGE_ID = "pkRangeId";
    private static final String METRIC_NAME = "metricName";

    public static void appendSimpleCsvFileHeader(PrintWriter printWriter) {
        StringBuilder sb = new StringBuilder();
        sb.append(TIME);
        sb.append(',');
        sb.append(COUNT);
        sb.append(',');
        sb.append(MAX);
        sb.append(',');
        sb.append(MEAN);
        sb.append(',');
        sb.append(MIN);
        sb.append(',');
        sb.append(STDDEV);
        sb.append(',');
        sb.append(P50);
        sb.append(',');
        sb.append(P75);
        sb.append(',');
        sb.append(P95);
        sb.append(',');
        sb.append(P99);
        sb.append(',');
        sb.append(P999);
        sb.append(',');
        sb.append(P9999);
        sb.append(',');
        sb.append(METRIC_NAME);
        sb.append(',');
        sb.append(PK_RANGE_ID);
        sb.append('\n');
        printWriter.write(sb.toString());
    }

    public static void appendHistogramSnapshot(
            ConcurrentDoubleHistogram histogram,
            List<PrintWriter> printWriters,
            Instant recordTime,
            String metricsName,
            String pkRangeId) {
        StringBuilder sb = new StringBuilder();

        sb.append(recordTime);
        sb.append(',');
        sb.append(histogram.getTotalCount());
        sb.append(',');
        sb.append(histogram.getMaxValue());
        sb.append(',');
        sb.append(histogram.getMean());
        sb.append(',');
        sb.append(histogram.getMinValue());
        sb.append(',');
        sb.append(histogram.getStdDeviation());
        sb.append(',');
        sb.append(histogram.getValueAtPercentile(50.0));
        sb.append(',');
        sb.append(histogram.getValueAtPercentile(75.0));
        sb.append(',');
        sb.append(histogram.getValueAtPercentile(95.0));
        sb.append(',');
        sb.append(histogram.getValueAtPercentile(99.0));
        sb.append(',');
        sb.append(histogram.getValueAtPercentile(999.0));
        sb.append(',');
        sb.append(histogram.getValueAtPercentile(9999.0));
        sb.append(',');
        sb.append(metricsName);
        sb.append(',');
        sb.append(pkRangeId);
        sb.append('\n');

        for (PrintWriter printWriter : printWriters) {
            printWriter.write(sb.toString());
            printWriter.flush();
        }
    }
}
