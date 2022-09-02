package com.azure;

import com.azure.diagnosticsValidator.IDiagnosticsValidator;
import com.azure.metricsRecorder.IMetricsRecorder;
import com.azure.models.Diagnostics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
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

    public DiagnosticsHandler(Duration recordInterval, String logPrefix) {
        this.recordIntervalInMillis = recordInterval.toMillis();
        this.metricsRecorderList = new ArrayList<>();
        this.diagnosticsValidatorList = new ArrayList<>();
        this.recordTimestamp = null;
        this.logPrefix = logPrefix;
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

        for (Diagnostics diagnostics : orderedDiagnostics) {
            this.processDiagnosticsInternal(diagnostics);
        }

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
        for (IMetricsRecorder metricsRecorder : metricsRecorderList) {
            metricsRecorder.recordHistogramSnapshot(this.recordTimestamp);
        }
    }

    @Override
    public void close() throws Exception {
        for (IMetricsRecorder metricsRecorder : metricsRecorderList) {
            metricsRecorder.close();
        }
    }
}
