package com.azure.models;

import java.util.List;

public class RetryContext {
    private int retryLatency;
    private int retryCount;
    private List<Integer[]> statusAndSubStatusCodes;

    public int getRetryLatency() {
        return retryLatency;
    }

    public void setRetryLatency(int retryLatency) {
        this.retryLatency = retryLatency;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    public List<Integer[]> getStatusAndSubStatusCodes() {
        return statusAndSubStatusCodes;
    }

    public void setStatusAndSubStatusCodes(List<Integer[]> statusAndSubStatusCodes) {
        this.statusAndSubStatusCodes = statusAndSubStatusCodes;
    }
}
