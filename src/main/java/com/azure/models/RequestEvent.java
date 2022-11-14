package com.azure.models;

public class RequestEvent {
    private final RequestType requestType;
    private final String timestamp;
    private final String activityId;

    public RequestEvent(RequestType requestType, String timestamp, String activityId) {
        this.requestType = requestType;
        this.timestamp = timestamp;
        this.activityId = activityId;
    }

    public static RequestEvent createRequestStartEvent(String timestamp, String activityId) {
        return new RequestEvent(RequestType.START, timestamp, activityId);
    }

    public static RequestEvent createRequestEndEvent(String timestamp, String activityId) {
        return new RequestEvent(RequestType.END, timestamp, activityId);
    }

    public RequestType getRequestType() {
        return requestType;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getActivityId() {
        return activityId;
    }

    public static enum RequestType {
        START,
        END
    }
}
