package com.azure.models.wmt;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class WmtRawLog {
    @JsonProperty("log")
    private String log;


    public String getLog() {
        return log;
    }

    public void setLog(String log) {
        this.log = log;
    }
}
