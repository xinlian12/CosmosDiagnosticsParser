package com.azure;

import com.azure.metricsRecorder.IMetricsRecorder;
import com.azure.diagnosticsValidator.IDiagnosticsValidator;
import com.azure.models.Diagnostics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static io.netty.util.internal.ObjectUtil.checkNotNull;

public class DiagnosticsHandler implements AutoCloseable {
    private static Logger logger = LoggerFactory.getLogger(DiagnosticsHandler.class);

    private final List<IMetricsRecorder> metricsRecorderList;
    private final List<IDiagnosticsValidator> diagnosticsValidatorList;
    private final long recordIntervalInMillis;
    private Instant recordTimestamp;

    public DiagnosticsHandler(Duration recordInterval) {
        this.recordIntervalInMillis = recordInterval.toMillis();
        this.metricsRecorderList = new ArrayList<>();
        this.diagnosticsValidatorList = new ArrayList<>();
        this.recordTimestamp = null;
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
        if (this.recordTimestamp == null) {
            this.recordTimestamp = Instant.parse(diagnostics.getRequestStartTimeUTC()).plusMillis(this.recordIntervalInMillis);
        }

        if (Instant.parse(diagnostics.getRequestStartTimeUTC()).isBefore(this.recordTimestamp.plusMillis(this.recordIntervalInMillis))) {
            if (this.diagnosticsValidatorList.stream().anyMatch(validator -> !validator.validateDiagnostics(diagnostics))) {
                logger.warn("Validate diagnostics failed, going to skip it. Diagnostics: {}", diagnostics.getLogLine());
                return;
            }

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
        }
    }

    public void flush() {
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
