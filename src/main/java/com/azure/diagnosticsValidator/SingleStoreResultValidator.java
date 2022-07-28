package com.azure.diagnosticsValidator;

import com.azure.models.Diagnostics;

public class SingleStoreResultValidator implements IDiagnosticsValidator {
    @Override
    public boolean validateDiagnostics(Diagnostics diagnostics) {
        return diagnostics.getResponseStatisticsList().size() == 1;
    }
}
