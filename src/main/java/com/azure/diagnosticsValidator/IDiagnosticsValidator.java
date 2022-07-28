package com.azure.diagnosticsValidator;

import com.azure.models.Diagnostics;

public interface IDiagnosticsValidator {
    boolean validateDiagnostics(Diagnostics diagnostics);
}
