package com.azure.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ServiceEndpointStatistics {
    private int availableChannels;
    private int acquiredChannels;
    private int executorTaskQueueSize;
    private int inflightRequests;
    private String lastSuccessfulRequestTime;
    private String lastRequestTime;
    private String createdTime;
    private boolean isClosed;
    private Map<String, String> cerMetrics;

    public ServiceEndpointStatistics() {}

    public int getAvailableChannels() {
        return availableChannels;
    }

    public int getAcquiredChannels() {
        return acquiredChannels;
    }

    public int getExecutorTaskQueueSize() {
        return executorTaskQueueSize;
    }

    public int getInflightRequests() {
        return inflightRequests;
    }

    public String getLastSuccessfulRequestTime() {
        return lastSuccessfulRequestTime;
    }

    public String getLastRequestTime() {
        return lastRequestTime;
    }

    public String getCreatedTime() {
        return createdTime;
    }

    public boolean isClosed() {
        return isClosed;
    }

    public Map<String, String> getCerMetrics() {
        return cerMetrics;
    }

    public void setCerMetrics(Map<String, String> cerMetrics) {
        this.cerMetrics = cerMetrics;
    }
}
