package com.azure.PosteModel;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PosteDiagnostics {
    @JsonProperty("message")
    private String message;

    @JsonProperty("stack_trace")
    private String stackTraces;

    public PosteDiagnostics() {}

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getStackTraces() {
        return stackTraces;
    }

    public void setStackTraces(String stackTraces) {
        this.stackTraces = stackTraces;
    }
}
