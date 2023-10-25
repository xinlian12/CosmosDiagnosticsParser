package com.azure.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ChannelStatistics {
    private String channelId;
    private boolean waitForConnectionInit;
    private int pendingRequestsCount;
    private String lastReadTime;

    public String getChannelId() {
        return channelId;
    }

    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }

    public boolean isWaitForConnectionInit() {
        return waitForConnectionInit;
    }

    public void setWaitForConnectionInit(boolean waitForConnectionInit) {
        this.waitForConnectionInit = waitForConnectionInit;
    }

    public int getPendingRequestsCount() {
        return pendingRequestsCount;
    }

    public void setPendingRequestsCount(int pendingRequestsCount) {
        this.pendingRequestsCount = pendingRequestsCount;
    }

    public String getLastReadTime() {
        return lastReadTime;
    }

    public void setLastReadTime(String lastReadTime) {
        this.lastReadTime = lastReadTime;
    }
}
