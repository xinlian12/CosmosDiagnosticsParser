package com.azure.models;

public class MetadataDiagnostic {
    private String metaDataName;
    private String startTimeUTC;
    private String endTimeUTC;
    private String durationinMS;

    public String getMetaDataName() {
        return metaDataName;
    }

    public void setMetaDataName(String metaDataName) {
        this.metaDataName = metaDataName;
    }

    public String getStartTimeUTC() {
        return startTimeUTC;
    }

    public void setStartTimeUTC(String startTimeUTC) {
        this.startTimeUTC = startTimeUTC;
    }

    public String getEndTimeUTC() {
        return endTimeUTC;
    }

    public void setEndTimeUTC(String endTimeUTC) {
        this.endTimeUTC = endTimeUTC;
    }

    public String getDurationinMS() {
        return durationinMS;
    }

    public void setDurationinMS(String durationinMS) {
        this.durationinMS = durationinMS;
    }
}
