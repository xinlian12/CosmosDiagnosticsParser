package com.azure.models;

import com.azure.cosmos.implementation.OperationType;
import com.azure.cosmos.implementation.ResourceType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GatewayStatistics {
    private String sessionToken;
    private OperationType operationType;
    private ResourceType resourceType;
    private int statusCode;
    private int subStatusCode;
    private double requestCharge;
    private List<TransportEvent> requestTimeline;
    private String partitionKeyRangeId;
    private String exceptionMessage;
    private String exceptionResponseHeaders;

    public String getSessionToken() {
        return sessionToken;
    }

    public void setSessionToken(String sessionToken) {
        this.sessionToken = sessionToken;
    }

    public OperationType getOperationType() {
        return operationType;
    }

    public void setOperationType(OperationType operationType) {
        this.operationType = operationType;
    }

    public ResourceType getResourceType() {
        return resourceType;
    }

    public void setResourceType(ResourceType resourceType) {
        this.resourceType = resourceType;
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

    public double getRequestCharge() {
        return requestCharge;
    }

    public void setRequestCharge(double requestCharge) {
        this.requestCharge = requestCharge;
    }

    public List<TransportEvent> getRequestTimeline() {
        return requestTimeline;
    }

    public void setRequestTimeline(List<TransportEvent> requestTimeline) {
        this.requestTimeline = requestTimeline;
    }

    public String getPartitionKeyRangeId() {
        return partitionKeyRangeId;
    }

    public void setPartitionKeyRangeId(String partitionKeyRangeId) {
        this.partitionKeyRangeId = partitionKeyRangeId;
    }

    public String getExceptionMessage() {
        return exceptionMessage;
    }

    public void setExceptionMessage(String exceptionMessage) {
        this.exceptionMessage = exceptionMessage;
    }

    public String getExceptionResponseHeaders() {
        return exceptionResponseHeaders;
    }

    public void setExceptionResponseHeaders(String exceptionResponseHeaders) {
        this.exceptionResponseHeaders = exceptionResponseHeaders;
    }
}
