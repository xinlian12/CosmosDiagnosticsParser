package com.azure.models;

public enum ExceptionCategory {
    NONE("None"),
    CONNECTION_TIMEOUT_EXCEPTION("ConnectTimeoutException"),
    ACQUISITION_TIMEOUT_EXCEPTION("AcquisitionTimeoutException"),
    TRANSIT_TIMEOUT("TransitTimeout"),
    SERVER_410("Server410"),
    SERVER_429("Server429"),
    SERVER_404("Server404"),
    SERVER_412("Server412"),
    SERVER_409("Server409"),
    SERVER_449("Server409"),
    REQUEST_CANCELLED("RequestCancelled"),
    SERVER_408("Server408");

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

