package com.azure.models.transportEvents;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonDeserialize(using = JsonDeserializer.None.class)
public class PollEvent extends ChannelAcquisitionContextEventBase {
    private String poll;
    private int availableChannels;
    private int acquiredChannels;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<Object> details;

    public String getPoll() {
        return poll;
    }

    public void setPoll(String poll) {
        this.poll = poll;
    }

    @Override
    public String getCreatedTime() {
        return poll;
    }

    public int getAvailableChannels() {
        return availableChannels;
    }

    public void setAvailableChannels(int availableChannels) {
        this.availableChannels = availableChannels;
    }

    public int getAcquiredChannels() {
        return acquiredChannels;
    }

    public void setAcquiredChannels(int acquiredChannels) {
        this.acquiredChannels = acquiredChannels;
    }

    public List<Object> getDetails() {
        return details;
    }

    public void setDetails(List<Object> details) {
        this.details = details;
    }
}
