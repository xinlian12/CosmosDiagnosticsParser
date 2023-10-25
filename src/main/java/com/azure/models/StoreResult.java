package com.azure.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class StoreResult {
    private String storePhysicalAddress;
    private long lsn;
    private String partitionKeyRangeId;
    private boolean isValid;
    private int statusCode;
    private int subStatusCode;
    private boolean isGone;
    private boolean isNotFound;
    private boolean isValidPartition;
    private boolean isThroughputControlRequestRateTooLarge;
    private double requestCharge;
    private long itemLSN;
    private String sessionToken;
    private Double backendLatencyInMs;
    private String exceptionMessage;
    private String exceptionResponseHeaders;
    private List<TransportEvent> transportRequestTimeline;
    private int channelTaskQueueSize;
    private ChannelAcquisitionContextEvent transportRequestChannelAcquisitionContext;
    private ServiceEndpointStatistics serviceEndpointStatistics;
    private ChannelStatistics channelStatistics;
    private List<String> replicaStatusList;

    public StoreResult() {}
    public String getStorePhysicalAddress() {
        return storePhysicalAddress;
    }

    public void setStorePhysicalAddress(String storePhysicalAddress) {
        this.storePhysicalAddress = storePhysicalAddress;
    }

    public long getLsn() {
        return lsn;
    }

    public void setLsn(long lsn) {
        this.lsn = lsn;
    }

    public String getPartitionKeyRangeId() {
        return partitionKeyRangeId;
    }

    public void setPartitionKeyRangeId(String partitionKeyRangeId) {
        this.partitionKeyRangeId = partitionKeyRangeId;
    }

    public boolean isValid() {
        return isValid;
    }

    public void setValid(boolean valid) {
        isValid = valid;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public int getSubStatusCode() {
        return subStatusCode;
    }

    public void setSubStatusCode(int subStatusCode) {
        this.subStatusCode = subStatusCode;
    }

    public boolean isGone() {
        return isGone;
    }

    public void setGone(boolean gone) {
        isGone = gone;
    }

    public boolean isNotFound() {
        return isNotFound;
    }

    public void setNotFound(boolean notFound) {
        isNotFound = notFound;
    }

    public boolean isValidPartition() {
        return isValidPartition;
    }

    public void setValidPartition(boolean validPartition) {
        isValidPartition = validPartition;
    }

    public boolean isThroughputControlRequestRateTooLarge() {
        return isThroughputControlRequestRateTooLarge;
    }

    public void setThroughputControlRequestRateTooLarge(boolean throughputControlRequestRateTooLarge) {
        isThroughputControlRequestRateTooLarge = throughputControlRequestRateTooLarge;
    }

    public double getRequestCharge() {
        return requestCharge;
    }

    public void setRequestCharge(double requestCharge) {
        this.requestCharge = requestCharge;
    }

    public long getItemLSN() {
        return itemLSN;
    }

    public void setItemLSN(long itemLSN) {
        this.itemLSN = itemLSN;
    }

    public String getSessionToken() {
        return sessionToken;
    }

    public void setSessionToken(String sessionToken) {
        this.sessionToken = sessionToken;
    }

    public Double getBackendLatencyInMs() {
        return backendLatencyInMs;
    }

    public void setBackendLatencyInMs(Double backendLatencyInMs) {
        this.backendLatencyInMs = backendLatencyInMs;
    }

    public String getExceptionMessage() {
        return exceptionMessage;
    }

    public void setExceptionMessage(String exceptionMessage) {
        this.exceptionMessage = exceptionMessage;
    }

    public List<TransportEvent> getTransportRequestTimeline() {
        return transportRequestTimeline;
    }

    public void setTransportRequestTimeline(List<TransportEvent> transportRequestTimeline) {
        this.transportRequestTimeline = transportRequestTimeline;
    }

    public int getChannelTaskQueueSize() {
        return channelTaskQueueSize;
    }

    public void setChannelTaskQueueSize(int channelTaskQueueSize) {
        this.channelTaskQueueSize = channelTaskQueueSize;
    }

    public ChannelAcquisitionContextEvent getTransportRequestChannelAcquisitionContext() {
        return transportRequestChannelAcquisitionContext;
    }

    public void setTransportRequestChannelAcquisitionContext(ChannelAcquisitionContextEvent transportRequestChannelAcquisitionContext) {
        this.transportRequestChannelAcquisitionContext = transportRequestChannelAcquisitionContext;
    }

    public ServiceEndpointStatistics getServiceEndpointStatistics() {
        return serviceEndpointStatistics;
    }

    public ChannelStatistics getChannelStatistics() {
        return this.channelStatistics;
    }

    public List<String> getReplicaStatusList() {
        return replicaStatusList;
    }

    public void setReplicaStatusList(List<String> replicaStatusList) {
        this.replicaStatusList = replicaStatusList;
    }

    public String getExceptionResponseHeaders() {
        return exceptionResponseHeaders;
    }

    public void setExceptionResponseHeaders(String exceptionResponseHeaders) {
        this.exceptionResponseHeaders = exceptionResponseHeaders;
    }
}

