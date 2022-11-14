package com.azure.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class StoreResultWrapper {
    private StoreResult storeResult;

    public StoreResultWrapper() {}
    public StoreResultWrapper(StoreResult storeResult) {
        this.storeResult = storeResult;
    }

    public StoreResult getStoreResult() {
        return storeResult;
    }

    public void setStoreResult(StoreResult storeResult) {
        this.storeResult = storeResult;
    }
}
