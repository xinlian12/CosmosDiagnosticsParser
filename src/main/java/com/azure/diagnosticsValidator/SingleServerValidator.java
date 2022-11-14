package com.azure.diagnosticsValidator;

import com.azure.models.Diagnostics;

import java.util.concurrent.atomic.AtomicInteger;

public class SingleServerValidator implements IDiagnosticsValidator{
    private final String serverKey;

    public SingleServerValidator(String serverKey) {
        this.serverKey = serverKey;
    }
    @Override
    public boolean validateDiagnostics(Diagnostics diagnostics) {
        return diagnostics
                .getResponseStatisticsList()
                .stream()
                .anyMatch(
                        storeResultWrapper ->
                                storeResultWrapper.getStoreResult().getStorePhysicalAddress().contains(this.serverKey));
    }
}
