package com.azure.models.transportEvents;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonDeserialize(using = JsonDeserializer.None.class)
public class ChannelAcquisitionStartedEvent extends ChannelAcquisitionContextEventBase{
    private String acquisitionStarted;

    public String getAcquisitionStarted() {
        return acquisitionStarted;
    }

    public void setAcquisitionStarted(String acquisitionStarted) {
        this.acquisitionStarted = acquisitionStarted;
    }

    @Override
    public String getCreatedTime() {
        return this.acquisitionStarted;
    }
}
