package com.azure.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

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

    @JsonIgnore
    private String logLine;

    public Diagnostics() {
    }

    public Diagnostics(String userAgent, int requestLatencyInMs, String requestStartTimeUTC, String requestEndTimeUTC, List<StoreResultWrapper> responseStatisticsList) {
        this.userAgent = userAgent;
        this.requestLatencyInMs = requestLatencyInMs;
        this.requestStartTimeUTC = requestStartTimeUTC;
        this.requestEndTimeUTC = requestEndTimeUTC;
        this.responseStatisticsList = responseStatisticsList;
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

    @JsonIgnore
    public String getLogLine() {
        return logLine;
    }

    @JsonIgnore
    public void setLogLine(String logLine) {
        this.logLine = logLine;
    }
}
