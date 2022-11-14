package com.azure.diagnosticsValidator;

import com.azure.models.Diagnostics;

import java.time.Duration;
import java.time.Instant;

public class RequestLatencyValidator implements IDiagnosticsValidator{

    private final double lowerBoundThreshold;
    private final double upperBoundThreshold;

    public RequestLatencyValidator(double lowerBoundThreshold, double upperBoundThreshold) {
        this.lowerBoundThreshold = lowerBoundThreshold;
        this.upperBoundThreshold = upperBoundThreshold;
    }

    @Override
    public boolean validateDiagnostics(Diagnostics diagnostics) {
        double requestLatency =
                Duration.between(Instant.parse(diagnostics.getRequestStartTimeUTC()), Instant.parse(diagnostics.getRequestEndTimeUTC())).toMillis();

        return requestLatency >= lowerBoundThreshold && requestLatency < upperBoundThreshold;
    }
}
