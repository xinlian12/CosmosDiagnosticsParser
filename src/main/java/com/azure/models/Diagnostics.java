package com.azure.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Diagnostics {
    private String userAgent;
    private int requestLatencyInMs;
    private String requestStartTimeUTC;
    private String requestEndTimeUTC;
    private List<StoreResultWrapper> responseStatisticsList;
    private String serverEndpoint;
    private String partitionId;
    private String replicaId;
    private String port;
    private String activityId;
    private Map<String, AddressResolutionDiagnostics> addressResolutionStatistics;
    private Map<String, List<MetadataDiagnostic>> metadataDiagnosticsContext;
    private RetryContext retryContext;

    @JsonIgnore
    private String logLine;

    public Diagnostics() {
    }

    public Diagnostics(
            String userAgent,
            int requestLatencyInMs,
            String requestStartTimeUTC,
            String requestEndTimeUTC,
            List<StoreResultWrapper> responseStatisticsList,
            Map<String, AddressResolutionDiagnostics> addressResolutionStatistics) {
        this.userAgent = userAgent;
        this.requestLatencyInMs = requestLatencyInMs;
        this.requestStartTimeUTC = requestStartTimeUTC;
        this.requestEndTimeUTC = requestEndTimeUTC;
        this.responseStatisticsList = responseStatisticsList;
        this.addressResolutionStatistics = addressResolutionStatistics;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public int getRequestLatencyInMs() {
        return requestLatencyInMs;
    }

    public void setRequestLatencyInMs(int requestLatencyInMs) {
        this.requestLatencyInMs = requestLatencyInMs;
    }

    public String getRequestStartTimeUTC() {
        return requestStartTimeUTC;
    }

    public void setRequestStartTimeUTC(String requestStartTimeUTC) {
        this.requestStartTimeUTC = requestStartTimeUTC;
    }

    public String getRequestEndTimeUTC() {
        return requestEndTimeUTC;
    }

    public void setRequestEndTimeUTC(String requestEndTimeUTC) {
        this.requestEndTimeUTC = requestEndTimeUTC;
    }

    public List<StoreResultWrapper> getResponseStatisticsList() {
        return responseStatisticsList;
    }

    public void setResponseStatisticsList(List<StoreResultWrapper> responseStatisticsList) {
        this.responseStatisticsList = responseStatisticsList;
    }

    public String getServerEndpoint() {
        return serverEndpoint;
    }

    public void setServerEndpoint(String serverEndpoint) {
        this.serverEndpoint = serverEndpoint;
    }

    public String getPartitionId() {
        return partitionId;
    }

    public void setPartitionId(String partitionId) {
        this.partitionId = partitionId;
    }

    public String getReplicaId() {
        return replicaId;
    }

    public void setReplicaId(String replicaId) {
        this.replicaId = replicaId;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public Map<String, AddressResolutionDiagnostics> getAddressResolutionStatistics() {
        return addressResolutionStatistics;
    }

    public void setAddressResolutionStatistics(Map<String, AddressResolutionDiagnostics> addressResolutionStatistics) {
        this.addressResolutionStatistics = addressResolutionStatistics;
    }

    public String getActivityId() {
        return activityId;
    }

    public void setActivityId(String activityId) {
        this.activityId = activityId;
    }

    @JsonIgnore
    public String getLogLine() {
        return logLine;
    }

    @JsonIgnore
    public void setLogLine(String logLine) {
        this.logLine = logLine;
    }

    public Map<String, List<MetadataDiagnostic>> getMetadataDiagnosticsContext() {
        return metadataDiagnosticsContext;
    }

    public void setMetadataDiagnosticsContext(Map<String, List<MetadataDiagnostic>> metadataDiagnosticsContext) {
        this.metadataDiagnosticsContext = metadataDiagnosticsContext;
    }

    public RetryContext getRetryContext() {
        return retryContext;
    }

    public void setRetryContext(RetryContext retryContext) {
        this.retryContext = retryContext;
    }
}
