package com.azure.diagnosticsValidator;

import com.azure.models.Diagnostics;
import com.azure.models.StoreResult;

public class TransportEventDurationValidator implements IDiagnosticsValidator {
    @Override
    public boolean validateDiagnostics(Diagnostics diagnostics) {
        StoreResult storeResult = diagnostics.getResponseStatisticsList().get(0).getStoreResult();
        if (storeResult.getTransportRequestTimeline().stream().anyMatch(transportEvent -> transportEvent.getDurationInMicroSec() < 0)) {
            return false;
        }
        return true;
    }
}
