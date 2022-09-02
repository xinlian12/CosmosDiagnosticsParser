package com.azure.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AddressResolutionDiagnostics {
    private String activityId;
    private String startTimeUTC;
    private String endTimeUTC;
    private String targetEndpoint;
    private String exceptionMessage;
    private boolean forceRefresh;
    private boolean forceCollectionRoutingMapRefresh;
    private boolean inflightRequest;

    public String getActivityId() {
        return activityId;
    }

    public void setActivityId(String activityId) {
        this.activityId = activityId;
    }

    public String getStartTimeUTC() {
        return startTimeUTC;
    }

    public void setStartTimeUTC(String startTimeUTC) {
        this.startTimeUTC = startTimeUTC;
    }

    public String getEndTimeUTC() {
        return endTimeUTC;
    }

    public void setEndTimeUTC(String endTimeUTC) {
        this.endTimeUTC = endTimeUTC;
    }

    public String getTargetEndpoint() {
        return targetEndpoint;
    }

    public void setTargetEndpoint(String targetEndpoint) {
        this.targetEndpoint = targetEndpoint;
    }

    public String getExceptionMessage() {
        return exceptionMessage;
    }

    public void setExceptionMessage(String exceptionMessage) {
        this.exceptionMessage = exceptionMessage;
    }

    public boolean isForceRefresh() {
        return forceRefresh;
    }

    public void setForceRefresh(boolean forceRefresh) {
        this.forceRefresh = forceRefresh;
    }

    public boolean isForceCollectionRoutingMapRefresh() {
        return forceCollectionRoutingMapRefresh;
    }

    public void setForceCollectionRoutingMapRefresh(boolean forceCollectionRoutingMapRefresh) {
        this.forceCollectionRoutingMapRefresh = forceCollectionRoutingMapRefresh;
    }

    public boolean isInflightRequest() {
        return inflightRequest;
    }

    public void setInflightRequest(boolean inflightRequest) {
        this.inflightRequest = inflightRequest;
    }
}
