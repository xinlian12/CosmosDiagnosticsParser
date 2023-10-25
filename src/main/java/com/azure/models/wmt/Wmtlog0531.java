package com.azure.models.wmt;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Wmtlog0531 {
    private Log0531 log;

    public Log0531 getLog() {
        return log;
    }

    public void setLog(Log0531 log) {
        this.log = log;
    }
}
