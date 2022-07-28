package com.azure.models.transportEvents;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonDeserialize(using = JsonDeserializer.None.class)
public class ChannelAcquisitionCompleteEvent extends ChannelAcquisitionContextEventBase{
    private String acquisitionCompleted;


    public String getAcquisitionCompleted() {
        return acquisitionCompleted;
    }

    public void setAcquisitionCompleted(String acquisitionCompleted) {
        this.acquisitionCompleted = acquisitionCompleted;
    }

    @Override
    public String getCreatedTime() {
        return acquisitionCompleted;
    }
}

