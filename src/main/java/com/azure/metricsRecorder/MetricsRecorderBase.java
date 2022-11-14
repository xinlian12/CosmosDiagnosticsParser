package com.azure.metricsRecorder;

import com.azure.models.Diagnostics;
import com.azure.utils.CsvFileUtils;
import org.HdrHistogram.ConcurrentDoubleHistogram;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class MetricsRecorderBase implements IMetricsRecorder, AutoCloseable {
    private final int metricsPrecision;
    private final int metricsMaxValue;
    private final String metricsName;
    private final PrintWriter printWriter;
    private final AtomicInteger integer = new AtomicInteger(0);
    private final ConcurrentHashMap<String, ConcurrentDoubleHistogram> concurrentDoubleHistogramMapByPkRangeId;
    private final ConcurrentDoubleHistogram concurrentDoubleHistogram;

    public MetricsRecorderBase(
            String metricsName,
            String logFilePathPrefix,
            int metricsPrecision,
            int metricsMax) throws FileNotFoundException {
        this.metricsName = metricsName;
        this.metricsPrecision = metricsPrecision;
        this.metricsMaxValue = metricsMax;
        this.concurrentDoubleHistogramMapByPkRangeId = new ConcurrentHashMap<>();
        this.concurrentDoubleHistogram = new ConcurrentDoubleHistogram(this.metricsMaxValue, this.metricsPrecision);

        String logFilePath = logFilePathPrefix + metricsName + ".csv";

        this.printWriter = new PrintWriter(logFilePath);

        CsvFileUtils.appendSimpleCsvFileHeader(this.printWriter);
    }

    public ConcurrentHashMap<String, ConcurrentDoubleHistogram> getConcurrentDoubleHistogramMapByPkRangeId() {
        return concurrentDoubleHistogramMapByPkRangeId;
    }

    public abstract List<Double> getRecordValues(Diagnostics diagnostics);

    @Override
    public void close() {
        if (this.printWriter != null) {
            this.printWriter.flush();
            this.printWriter.close();
        }
    }

    @Override
    public void recordValue(Diagnostics diagnostics) {
        List<Double> recordValues = this.getRecordValues(diagnostics);
        String pkRangeId = diagnostics.getResponseStatisticsList().get(0).getStoreResult().getPartitionKeyRangeId();

        try {
//            this.getConcurrentDoubleHistogramMapByPkRangeId().compute(pkRangeId, (key, histogram) -> {
//                if(histogram == null) {
//                    histogram = new ConcurrentDoubleHistogram(this.metricsMaxValue, this.metricsPrecision);
//                }
//
//                for (double recordValue : recordValues) {
//                    histogram.recordValue(recordValue);
//                }
//                return histogram;
//            });

            for (double recordValue : recordValues) {
                this.concurrentDoubleHistogram.recordValue(recordValue);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public void recordHistogramSnapshot(Instant recordTimestamp) {
//        for (String pkRangeId : this.concurrentDoubleHistogramMapByPkRangeId.keySet()) {
//
//            FileUtils.appendHistogramSnapshot(
//                    this.concurrentDoubleHistogramMapByPkRangeId.get(pkRangeId),
//                    Arrays.asList(this.printWriter),
//                    recordTimestamp,
//                    this.metricsName,
//                    pkRangeId
//            );
//            this.concurrentDoubleHistogramMapByPkRangeId.get(pkRangeId).reset();
//        }

        CsvFileUtils.appendHistogramSnapshot(
                this.concurrentDoubleHistogram,
                Arrays.asList(this.printWriter),
                recordTimestamp,
                this.metricsName,
                "None"
        );

        this.concurrentDoubleHistogram.reset();
    }
}
