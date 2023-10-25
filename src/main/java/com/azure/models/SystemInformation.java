package com.azure.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SystemInformation {
    private String systemCpuLoad;

    public String getSystemCpuLoad() {
        return systemCpuLoad;
    }

    public void setSystemCpuLoad(String systemCpuLoad) {
        this.systemCpuLoad = systemCpuLoad;
    }
}
