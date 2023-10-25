package com.azure.models.wmt;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class WmtLog {
    private WmtResult result;
    public WmtLog() {}

    public WmtResult getResult() {
        return result;
    }

    public void setResult(WmtResult result) {
        this.result = result;
    }
}
