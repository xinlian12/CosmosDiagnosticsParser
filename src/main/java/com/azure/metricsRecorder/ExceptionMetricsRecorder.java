package com.azure.metricsRecorder;

import com.azure.ISummaryRecorder;
import com.azure.common.DiagnosticsHelper;
import com.azure.models.Diagnostics;
import com.azure.models.ExceptionCategory;
import com.azure.models.ServiceEndpointStatistics;
import com.azure.models.StoreResultWrapper;
import com.azure.models.TransportTimelineEventName;
import com.azure.utils.CsvFileUtils;
import org.HdrHistogram.ConcurrentDoubleHistogram;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static com.azure.models.ExceptionCategory.ACQUISITION_TIMEOUT_EXCEPTION;
import static com.azure.models.ExceptionCategory.CONNECTION_TIMEOUT_EXCEPTION;
import static com.azure.models.ExceptionCategory.TRANSIT_TIMEOUT;

public class ExceptionMetricsRecorder implements IMetricsRecorder {
    private static final Logger logger = LoggerFactory.getLogger(ExceptionMetricsRecorder.class);
    public static final int METRICS_PRECISION = 2;
    public static final int METRICS_MAX = Integer.MAX_VALUE;

    private final ConcurrentHashMap<ExceptionCategory, ConcurrentDoubleHistogram> errorsHistograms = new ConcurrentHashMap<>();
    private final PrintWriter printWriter;
    private final ConcurrentHashMap<String, AtomicInteger> partitionWithErrorSet = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<ExceptionCategory, AtomicInteger> errorCountsByCategory = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Integer> connectionTimeoutOnUnknown = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Integer> connectionTimeoutOnConnected0 = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Integer> connectionTimeoutOnConnected = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Integer> connectionTimeoutOnOthers = new ConcurrentHashMap<>();
    private final List<String> connectionTimeoutExceptionLogs = new ArrayList<>();
    private final ConcurrentHashMap<String, Integer> errorsByServer = new ConcurrentHashMap<>();

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
                                .filter(transportEvent -> transportEvent.getEventName().equals(TransportTimelineEventName.RECEIVED.getDescription()))
                                .findFirst()
                                .get()
                                .getStartTimeUTC() == null) {
                    errorCategory = ExceptionCategory.TRANSIT_TIMEOUT;

                } else if (storeResultWrapper.getStoreResult().getStatusCode() == 410) {
                    errorCategory = ExceptionCategory.SERVER_410;
                } else if (storeResultWrapper.getStoreResult().getStatusCode() == 429) {
                    errorCategory = ExceptionCategory.SERVER_429;

                  //  System.out.println("Server 429:" + diagnostics.getLogLine());
                } else {
                    throw new IllegalStateException("what kind exception is this: " + diagnostics.getLogLine());
                }

                this.errorCountsByCategory.compute(errorCategory, (error, count) -> {
                    if (count == null) {
                        count = new AtomicInteger(0);
                    }

                    count.incrementAndGet();
                    return count;
                });

                if(errorCategory == TRANSIT_TIMEOUT || errorCategory == CONNECTION_TIMEOUT_EXCEPTION) {
                    String serverKey = DiagnosticsHelper.getServerKey(storeResultWrapper);
                    this.errorsByServer.compute(serverKey, (key, count) -> {
                        if (count == null) {
                            System.out.println(serverKey);
                            count = 0;
                        }

                        count += 1;
                        return count;
                    });
                }

                // pkRange analysis
                if (errorCategory == CONNECTION_TIMEOUT_EXCEPTION || errorCategory == ACQUISITION_TIMEOUT_EXCEPTION) {
                    connectionTimeoutExceptionLogs.add(diagnostics.getLogLine());
                    String pkRangeId = diagnostics
                            .getResponseStatisticsList()
                            .stream().filter(resultWrapper -> {
                                return resultWrapper.getStoreResult().getPartitionKeyRangeId() != null;
                            })
                            .map(resultWrapper -> resultWrapper.getStoreResult().getPartitionKeyRangeId())
                            .findFirst()
                            .get();

                    ServiceEndpointStatistics serviceEndpointStatistics = storeResultWrapper.getStoreResult().getServiceEndpointStatistics();
                    int totalChannels = serviceEndpointStatistics.getAcquiredChannels() + serviceEndpointStatistics.getAvailableChannels();

                    if (storeResultWrapper.getStoreResult().getReplicaStatusList() != null
                    && storeResultWrapper.getStoreResult().getReplicaStatusList().size() > 0) {
                        String replicaStatus = storeResultWrapper.getStoreResult().getReplicaStatusList().get(0);
                        String serverKey = DiagnosticsHelper.getServerKey(storeResultWrapper);
                        if (replicaStatus.contains("Unknown")) {
                            this.connectionTimeoutOnUnknown.compute(serverKey, (key, count) -> {
                                if (count == null) {
                                    count = 0;
                                }

                                count += 1;
                                return count;
                            });

                        } else if (replicaStatus.contains("Connected")) {
                            if (totalChannels > 0) {
                                this.connectionTimeoutOnConnected.compute(serverKey, (key, count) -> {
                                    if (count == null) {
                                        count = 0;
                                    }

                                    count += 1;
                                    return count;
                                });

                            } else {
                                this.connectionTimeoutOnConnected0.compute(serverKey, (key, count) -> {
                                    if (count == null) {
                                        count = 0;
                                    }

                                    count += 1;

                                    System.out.println("Timeout when there is connected connection " + diagnostics.getLogLine());
                                    return count;
                                });
                            }

                        } else {
                            this.connectionTimeoutOnOthers.compute(serverKey, (key, count) -> {
                                if (count == null) {
                                    count = 0;
                                }

                                count += 1;
                                return count;
                            });
                        }
                    }

                    partitionWithErrorSet.compute(pkRangeId, (partitionKeyRangeId, count) -> {
                        if (count == null) {
                            count = new AtomicInteger(0);
                        }

                        count.incrementAndGet();
                        return count;
                    });
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
            CsvFileUtils.appendHistogramSnapshot(
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
    public void reportStatistics(ISummaryRecorder summaryRecorder) {

        System.out.println(String.format("Exception count by category %s", this.errorCountsByCategory));
        summaryRecorder.getPrintWriter().println(String.format("Exception count by category %s", this.errorCountsByCategory));

//        if (this.connectionTimeoutExceptionLogs.size() > 0) {
//            System.out.println("ConnectionTimeout logs");
//            printWriter.println("ConnectionTimeout logs");
//            for (String logLine : this.connectionTimeoutExceptionLogs) {
//                System.out.println(logLine);
//                printWriter.write(logLine);
//            }
//        }

//        if (this.errorCountsByCategory.contains(CONNECTION_TIMEOUT_EXCEPTION)) {
//            System.out.println(
//                    String.format("ConnectionTimeout:%d failed on UnknownStatus, %d failed on ConnectedStatus, %d failed on others",
//                            this.channelFailedForNonFirst.get(),
//                            this.channelFailOnUnknownStatus.get(),
//                            this.channelFailedOnConnectedStatus.get(),
//                            this.channelFailedOnOtherStatus.get())
//            );
//
//            printWriter.println(String.format("ConnectionTimeout:%d failed on UnknownStatus, %d failed on ConnectedStatus, %d failed on others",
//                    this.channelFailedForNonFirst.get(),
//                    this.channelFailOnUnknownStatus.get(),
//                    this.channelFailedOnConnectedStatus.get(),
//                    this.channelFailedOnOtherStatus.get()));
//        }

        summaryRecorder.recordErrors(this.errorCountsByCategory);
        summaryRecorder.recordServerErrors(this.errorsByServer);
        summaryRecorder.recordConnectionTimeoutOnUnknown(this.connectionTimeoutOnUnknown);
        summaryRecorder.recordConnectionTimeoutOnConnected(this.connectionTimeoutOnConnected);
        summaryRecorder.recordConnectionTimeoutOnConnected0(this.connectionTimeoutOnConnected0);
        summaryRecorder.recordConnectionTimeoutOnOthers(this.connectionTimeoutOnOthers);
        summaryRecorder.getPrintWriter().flush();
    }

    @Override
    public void close() throws Exception {
        this.printWriter.flush();
        this.printWriter.close();
    }
}
