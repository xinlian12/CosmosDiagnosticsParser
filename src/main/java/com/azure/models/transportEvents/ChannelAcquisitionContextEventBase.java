package com.azure.models.transportEvents;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(using = ChannelAcquisitionContextEventDeserializer.class)
public abstract class ChannelAcquisitionContextEventBase {
    private int durationInMicroSec;

    public abstract String getCreatedTime();

    public int getDurationInMicroSec() {
        return durationInMicroSec;
    }
    public void setDurationInMicroSec(int durationInMicroSec) {
        this.durationInMicroSec = durationInMicroSec;
    }
}
