package com.azure.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TransportEvent {
    private String eventName;
    private String startTimeUTC;
    private Double durationInMilliSecs;

    private Double durationInMicroSec;
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

    public Double getDurationInMilliSecs() {
        if (this.durationInMilliSecs != null) {
            return this.durationInMilliSecs;
        }

        return this.durationInMicroSec;
    }

    public void setDurationInMilliSecs(Double durationInMilliSecs) {
        this.durationInMilliSecs = durationInMilliSecs;
    }

    public Double getDurationInMicroSec() {
        return durationInMicroSec;
    }

    public void setDurationInMicroSec(Double durationInMicroSec) {
        this.durationInMicroSec = durationInMicroSec;
        this.durationInMilliSecs = durationInMicroSec / 1000;
    }
}

