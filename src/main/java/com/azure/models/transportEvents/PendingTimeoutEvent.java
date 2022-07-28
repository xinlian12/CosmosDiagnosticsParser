package com.azure.models.transportEvents;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonDeserialize(using = JsonDeserializer.None.class)
public class PendingTimeoutEvent extends ChannelAcquisitionContextEventBase{
    private String pendingTimeout;

    public String getPendingTimeout() {
        return pendingTimeout;
    }

    public void setPendingTimeout(String pendingTimeout) {
        this.pendingTimeout = pendingTimeout;
    }

    @Override
    public String getCreatedTime() {
        return pendingTimeout;
    }
}

