package com.azure.models.transportEvents;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonDeserialize(using = JsonDeserializer.None.class)
public class StartNewEvent extends ChannelAcquisitionContextEventBase{
    private String startNew;

    public String getStartNew() {
        return startNew;
    }

    public void setStartNew(String startNew) {
        this.startNew = startNew;
    }

    @Override
    public String getCreatedTime() {
        return startNew;
    }
}
