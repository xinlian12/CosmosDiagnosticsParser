package com.azure.models.wmt;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class WmtResult {

    @JsonProperty("_raw")
    private String rawLog;
    @JsonProperty("_time")
    private String time;
    @JsonProperty("kubernetes.pod_name")
    private String podName;

    private WmtRawLog parsedLog;

    public WmtResult() {}

    public String getRawLog() {
        return rawLog;
    }

    public void setRawLog(String rawLog) {
        this.rawLog = rawLog;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getPodName() {
        return podName;
    }

    public void setPodName(String podName) {
        this.podName = podName;
    }

    public WmtRawLog getParsedLog() {
        return parsedLog;
    }

    public void setParsedLog(WmtRawLog parsedLog) {
        this.parsedLog = parsedLog;
    }
}
