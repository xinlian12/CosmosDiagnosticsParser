package com.azure.models;


import com.azure.models.transportEvents.ChannelAcquisitionContextEventBase;

import java.util.List;

public class ChannelAcquisitionContextEvent {
    private List<ChannelAcquisitionContextEventBase> events;

    public List<ChannelAcquisitionContextEventBase> getEvents() {
        return events;
    }

    public void setEvents(List<ChannelAcquisitionContextEventBase> events) {
        this.events = events;
    }
}
