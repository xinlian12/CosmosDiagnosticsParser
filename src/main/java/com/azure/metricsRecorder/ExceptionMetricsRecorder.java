package com.azure.metricsRecorder;

import com.azure.models.Diagnostics;
import com.azure.models.ExceptionCategory;
import com.azure.models.StoreResultWrapper;
import com.azure.models.TransportTimelineEventName;
import com.azure.utils.FileUtils;
import org.HdrHistogram.ConcurrentDoubleHistogram;
import org.apache.commons.lang3.StringUtils;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.azure.models.ExceptionCategory.ACQUISITION_TIMEOUT_EXCEPTION;
import static com.azure.models.ExceptionCategory.CONNECTION_TIMEOUT_EXCEPTION;

public class ExceptionMetricsRecorder implements IMetricsRecorder {
    public static final int METRICS_PRECISION = 2;
    public static final int METRICS_MAX = Integer.MAX_VALUE;

    private final ConcurrentHashMap<ExceptionCategory, ConcurrentDoubleHistogram> errorsHistograms = new ConcurrentHashMap<>();
    private final PrintWriter printWriter;
    private final Set<String> partitionWithErrorSet = ConcurrentHashMap.newKeySet();

    public ExceptionMetricsRecorder(String logFilePrefix) throws FileNotFoundException {
        String logFilePath = logFilePrefix + "Exceptions.csv";

        this.printWriter = new PrintWriter(logFilePath);
    }

    @Override
    public void recordValue(Diagnostics diagnostics) {
        for (StoreResultWrapper storeResultWrapper : diagnostics.getResponseStatisticsList()) {
            String exceptionMessage = storeResultWrapper.getStoreResult().getExceptionMessage();
            if (StringUtils.isNotEmpty(exceptionMessage)) {
                ExceptionCategory errorCategory;
                if (exceptionMessage.contains("ConnectTimeoutException")) {
                    errorCategory = CONNECTION_TIMEOUT_EXCEPTION;
                } else if (exceptionMessage.contains("acquisition took longer than the configured maximum time")) {
                    errorCategory = ACQUISITION_TIMEOUT_EXCEPTION;
                } else if (
                        storeResultWrapper
                                .getStoreResult()
                                .getTransportRequestTimeline()
                                .stream()
                                .filter(transportEvent -> transportEvent.getEventName().equals(TransportTimelineEventName.DECODE.getDescription()))
                                .findFirst()
                                .get()
                                .getStartTimeUTC() == null) {
                    errorCategory = ExceptionCategory.TRANSIT_TIMEOUT;

                } else if (storeResultWrapper.getStoreResult().getStatusCode() == 410) {
                    errorCategory = ExceptionCategory.SERVER_410;
                } else if (storeResultWrapper.getStoreResult().getStatusCode() == 429) {
                    errorCategory = ExceptionCategory.SERVER_429;
                } else {
                    throw new IllegalStateException("what kind exception is this: " + diagnostics.getLogLine());
                }

                if (errorCategory == CONNECTION_TIMEOUT_EXCEPTION || errorCategory == ACQUISITION_TIMEOUT_EXCEPTION) {
                    String pkRangeId = diagnostics
                            .getResponseStatisticsList()
                            .stream().filter(resultWrapper -> {
                                return resultWrapper.getStoreResult().getPartitionKeyRangeId() != null;
                            })
                            .map(resultWrapper -> resultWrapper.getStoreResult().getPartitionKeyRangeId())
                            .findFirst()
                            .get();

                    if (partitionWithErrorSet.add(pkRangeId)) {
                        System.out.println("Connection Timeout related exception: " + pkRangeId);
                    }
                }

                errorsHistograms.compute(errorCategory, (exception, histogram) -> {
                    if (histogram == null) {
                        histogram = new ConcurrentDoubleHistogram(METRICS_MAX, METRICS_PRECISION);
                    }

                    histogram.recordValue(1);
//                    histogram.recordValue(
//                            storeResultWrapper
//                                    .getStoreResult()
//                                    .getTransportRequestTimeline()
//                                    .stream().filter(transportEvent -> transportEvent.getEventName().equals(TransportTimelineEventName.CHANNEL_ACQUISITION.getDescription()))
//                                    .findFirst()
//                                    .get()
//                                    .getDurationInMilliSecs()
//                    );
                    return histogram;
                });
            }
        }
    }

    @Override
    public void recordHistogramSnapshot(Instant recordTimestamp) {

        for (ExceptionCategory key : Collections.list(errorsHistograms.keys())) {
            FileUtils.appendHistogramSnapshot(
                    this.errorsHistograms.get(key),
                    Arrays.asList(this.printWriter),
                    recordTimestamp,
                    key.getDescription(),
                    "None"
            );

            errorsHistograms.get(key).reset();
        }

    }

    @Override
    public void close() throws Exception {
        this.printWriter.flush();
        this.printWriter.close();
    }
}
