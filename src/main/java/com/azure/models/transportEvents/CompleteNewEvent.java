package com.azure.models.transportEvents;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonDeserialize(using = JsonDeserializer.None.class)
public class CompleteNewEvent extends ChannelAcquisitionContextEventBase{
    private String completeNew;

    public void setCompleteNew(String completeNew) {
        this.completeNew = completeNew;
    }

    public String getCompleteNew() {
        return completeNew;
    }

    @Override
    public String getCreatedTime() {
        return completeNew;
    }
}
