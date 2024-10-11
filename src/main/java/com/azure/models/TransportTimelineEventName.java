package com.azure.models;

public enum TransportTimelineEventName {
    CREATED("created"),
    QUEUED("queued"),
    CHANNEL_ACQUISITION("channelAcquisitionStarted"),
    PIPELINED("pipelined"),
    TRANSIT("transitTime"),
    DECODE("decodeTime"),
    RECEIVED("received"),
    COMPLETED("completed"),
    CONNECTION_CREATED("connectionCreated"),
    CONNECTION_ACQUIRED("connectionAcquired"),
    CONNECTION_CONFIGURED("connectionConfigured"),
    REQUEST_SENT("requestSent");

    private final String description;

    TransportTimelineEventName(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}