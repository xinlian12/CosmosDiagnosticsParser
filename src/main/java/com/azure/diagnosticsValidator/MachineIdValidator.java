package com.azure.diagnosticsValidator;

import com.azure.models.Diagnostics;

public class MachineIdValidator implements IDiagnosticsValidator{

    private final String machineId;

    public MachineIdValidator(String machineId) {
        this.machineId = machineId;
    }
    @Override
    public boolean validateDiagnostics(Diagnostics diagnostics) {
        return diagnostics.getClientCfgs().getMachineId().contains(this.machineId);
    }
}
