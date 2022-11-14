package com.azure;

import com.azure.diagnosticsValidator.IDiagnosticsValidator;
import com.azure.metricsRecorder.IMetricsRecorder;
import com.azure.models.Diagnostics;
import com.azure.models.RequestEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static io.netty.util.internal.ObjectUtil.checkNotNull;

public class DiagnosticsHandler implements AutoCloseable {
    private static Logger logger = LoggerFactory.getLogger(DiagnosticsHandler.class);

    private final List<IMetricsRecorder> metricsRecorderList;
    private final List<IDiagnosticsValidator> diagnosticsValidatorList;
    private final long recordIntervalInMillis;
    private Instant recordTimestamp;
    private final List<Diagnostics> validDiagnostics = new ArrayList<>();
    private final String logPrefix;
    private final SummaryRecorder summaryRecorder;
    private int maxConcurrency = Integer.MIN_VALUE;

    public DiagnosticsHandler(Duration recordInterval, String logPrefix, SummaryRecorder summaryRecorder) {
        this.recordIntervalInMillis = recordInterval.toMillis();
        this.metricsRecorderList = new ArrayList<>();
        this.diagnosticsValidatorList = new ArrayList<>();
        this.recordTimestamp = null;
        this.logPrefix = logPrefix;
        this.summaryRecorder = summaryRecorder;
    }

    public void registerMetricsRecorder(IMetricsRecorder metricsRecorder) {
        checkNotNull(metricsRecorder, "Argument 'metricsRecorder' should not be null");
        this.metricsRecorderList.add(metricsRecorder);
    }

    public void registerMetricsValidator(IDiagnosticsValidator metricsValidator) {
        checkNotNull(metricsValidator, "Argument 'metricsValidator' should not be null");
        this.diagnosticsValidatorList.add(metricsValidator);
    }

    public void processDiagnostics(Diagnostics diagnostics) {
        if (this.diagnosticsValidatorList.stream().anyMatch(validator -> !validator.validateDiagnostics(diagnostics))) {
            // logger.warn("Validate diagnostics failed, going to skip it. Diagnostics: {}", diagnostics.getLogLine());
            return;
        }

        this.validDiagnostics.add(diagnostics);
    }

    public void processDiagnosticsInternal(Diagnostics diagnostics) {
        if (this.recordTimestamp == null) {
            this.recordTimestamp = Instant.parse(diagnostics.getRequestStartTimeUTC()).plusMillis(this.recordIntervalInMillis);
        }

        if (this.diagnosticsValidatorList.stream().anyMatch(validator -> !validator.validateDiagnostics(diagnostics))) {
            // logger.warn("Validate diagnostics failed, going to skip it. Diagnostics: {}", diagnostics.getLogLine());
            return;
        }

        if (Instant.parse(diagnostics.getRequestStartTimeUTC()).isBefore(this.recordTimestamp.plusMillis(this.recordIntervalInMillis))) {
            for (IMetricsRecorder metricsRecorder : metricsRecorderList) {
                metricsRecorder.recordValue(diagnostics);
            }
        } else {
            // over the tracking interval
            for (IMetricsRecorder metricsRecorder : metricsRecorderList) {
                metricsRecorder.recordHistogramSnapshot(this.recordTimestamp);
            }
            // reset the recording
            this.recordTimestamp = recordTimestamp.plusMillis(recordIntervalInMillis);
            this.processDiagnosticsInternal(diagnostics);
        }
    }

    public void flush() {
        //order the diagnostics by timestamp
        List<Diagnostics> orderedDiagnostics =
                this.validDiagnostics.stream().sorted(new Comparator<Diagnostics>() {
                    @Override
                    public int compare(Diagnostics o1, Diagnostics o2) {
                        return Instant.parse(o1.getRequestStartTimeUTC()).compareTo(Instant.parse(o2.getRequestStartTimeUTC()));
                    }
                })
                .collect(Collectors.toList());

        int loggedLines = 0;
        List<RequestEvent> requestEvents = new ArrayList<>();
        for (Diagnostics diagnostics : orderedDiagnostics) {
            this.summaryRecorder.startNewDiagnostics(diagnostics);
//            if (loggedLines < 30) {
//                System.out.println(diagnostics.getLogLine());
//            }
//            loggedLines++;
            requestEvents.add(RequestEvent.createRequestStartEvent(diagnostics.getRequestStartTimeUTC(), diagnostics.getActivityId()));
            requestEvents.add(RequestEvent.createRequestEndEvent(diagnostics.getRequestEndTimeUTC(), diagnostics.getActivityId()));

            this.processDiagnosticsInternal(diagnostics);
        }

        this.summaryRecorder.startNewDiagnostics(null);

//        String fileName = logPrefix + "diagnostics.log";
//        try (PrintWriter printWriter = new PrintWriter(fileName)) {
//            for (Diagnostics diagnostics : orderedDiagnostics) {
//               printWriter.write(diagnostics.getLogLine());
//               printWriter.write("\n");
//               printWriter.flush();
//               this.processDiagnosticsInternal(diagnostics);
//            }
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        }

        // flush any left data
        if (orderedDiagnostics.size() > 0) {
            for (IMetricsRecorder metricsRecorder : metricsRecorderList) {
                metricsRecorder.recordHistogramSnapshot(this.recordTimestamp);
                metricsRecorder.reportStatistics(summaryRecorder);
            }
        }
        summaryRecorder.getPrintWriter().flush();
        this.parseConcurrency(requestEvents);

        System.out.println("Max concurrency is: " + this.maxConcurrency);
    }

    private void parseConcurrency(List<RequestEvent> requestEvents) {
        // analysis concurrency
        Collections.sort(requestEvents, new Comparator<RequestEvent>() {
            @Override
            public int compare(RequestEvent o1, RequestEvent o2) {
                return Instant.parse(o1.getTimestamp()).compareTo(Instant.parse(o2.getTimestamp()));
            }
        });

        int recordIntervalInMillis = 1000;
        Set<String> requests = new HashSet<>();
        Instant eventTimestamp = null;

        for (RequestEvent requestEvent : requestEvents) {
            if (eventTimestamp == null) {
                eventTimestamp = Instant.parse(requestEvents.get(0).getTimestamp()).plusMillis(recordIntervalInMillis);
            }

            if (Instant.parse(requestEvent.getTimestamp()).isBefore(eventTimestamp.plusMillis(recordIntervalInMillis))) {
                if (requestEvent.getRequestType() == RequestEvent.RequestType.START) {
                    requests.add(requestEvent.getActivityId());
                } else {
                    requests.remove(requestEvent.getActivityId());
                }

                if (this.maxConcurrency < requests.size()) {
                    this.maxConcurrency = requests.size();
                }
            } else {
               // System.out.println("Concurrency: " + requests.size());

                // reset the recording
                eventTimestamp = eventTimestamp.plusMillis(recordIntervalInMillis);
                if (requestEvent.getRequestType() == RequestEvent.RequestType.START) {
                    requests.add(requestEvent.getActivityId());
                } else {
                    requests.remove(requestEvent.getActivityId());
                }

                if (this.maxConcurrency < requests.size()) {
                    this.maxConcurrency = requests.size();
                }
            }
        }
    }

    @Override
    public void close() throws Exception {
        for (IMetricsRecorder metricsRecorder : metricsRecorderList) {
            metricsRecorder.close();
        }
    }
}
