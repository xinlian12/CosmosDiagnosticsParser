package com.azure.models;

import com.azure.cosmos.CosmosDiagnostics;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CosmosExceptionsModel {
    private Diagnostics cosmosDiagnostics;

    public Diagnostics getCosmosDiagnostics() {
        return cosmosDiagnostics;
    }

    public void setCosmosDiagnostics(Diagnostics cosmosDiagnostics) {
        this.cosmosDiagnostics = cosmosDiagnostics;
    }
}
