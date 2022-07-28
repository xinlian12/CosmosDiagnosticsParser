package com.azure.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TransportEvent {
    private String eventName;
    private String startTimeUTC;
    private int durationInMicroSec;
    public TransportEvent() {}

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public String getStartTimeUTC() {
        return startTimeUTC;
    }

    public void setStartTimeUTC(String startTimeUTC) {
        this.startTimeUTC = startTimeUTC;
    }

    public int getDurationInMicroSec() {
        return durationInMicroSec;
    }

    public void setDurationInMicroSec(int durationInMicroSec) {
        this.durationInMicroSec = durationInMicroSec;
    }
}

