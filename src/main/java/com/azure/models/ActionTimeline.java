package com.azure.models;

import java.util.List;

public class ActionTimeline {
    public static final String REQUEST_TIMESTAMP = "Request";
    public static final String CONNECTION_STATE_LISTENER_ACT_TIMESTAMP = "Connection_State_Listener_Act";
    public static final String CONNECTION_STATE_LISTENER_CALLED_TIMESTAMP = "Connection_State_Listener_Called";
    public static final String ADDRESS_REFRESH = "Address_Refresh";

    private final String eventName;
    private final String startTime;
    private final String endTime;
    private final List<Object> details;
    private ExceptionCategory exceptionCategory;
    private final String logLine;

    private final String requestId;

    public ActionTimeline(String eventName, String startTime, String endTime, List<Object> details, String logLine, String requestId) {
        this.eventName = eventName;
        this.startTime = startTime;
        this.endTime = endTime;
        this.details = details;
        this.logLine = logLine;
        this.requestId = requestId;
    }

    public String getEventName() {
        return eventName;
    }

    public String getStartTime() {
        return startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public List<Object> getDetails() {
        return details;
    }

    public static ActionTimeline createNewRequestActionTimeline(String startTime, String endTime, List<Object> details, ExceptionCategory exceptionCategory, String logLine, String requestId) {
        ActionTimeline actionTimeline = new ActionTimeline(REQUEST_TIMESTAMP, startTime, endTime, details, logLine, requestId);
        actionTimeline.setExceptionCategory(exceptionCategory);

        return actionTimeline;
    }

    public static ActionTimeline createConnectionStateListenerActTimeline(String startTime, String endTime, List<Object> details, String logLine, String requestId) {
        return new ActionTimeline(CONNECTION_STATE_LISTENER_ACT_TIMESTAMP, startTime, endTime, details, logLine, requestId);
    }

    public static ActionTimeline createConnectionStateListenerCalledTimeline(String startTime, String endTime, List<Object> details, String logLine, String requestId) {
        return new ActionTimeline(CONNECTION_STATE_LISTENER_CALLED_TIMESTAMP, startTime, endTime, details, logLine, requestId);
    }

    public static ActionTimeline createAddressRefreshTimeline(String startTime, String endTime, List<Object> details, String logLine, String requestId) {
        return new ActionTimeline(ADDRESS_REFRESH, startTime, endTime, details, logLine, requestId);
    }

    public ExceptionCategory getExceptionCategory() {
        return exceptionCategory;
    }

    public void setExceptionCategory(ExceptionCategory exceptionCategory) {
        this.exceptionCategory = exceptionCategory;
    }

    public String getLogLine() {
        return logLine;
    }

    public String getRequestId() {
        return this.requestId;
    }
}
