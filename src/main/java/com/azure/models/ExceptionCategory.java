package com.azure.models;

public enum ExceptionCategory {
    NONE("None"),
    CONNECTION_TIMEOUT_EXCEPTION("ConnectTimeoutException"),
    ACQUISITION_TIMEOUT_EXCEPTION("AcquisitionTimeoutException"),
    TRANSIT_TIMEOUT("TransitTimeout"),
    SERVER_410("Server410"),
    SERVER_429("Server429");

    private String description;

    ExceptionCategory(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}

