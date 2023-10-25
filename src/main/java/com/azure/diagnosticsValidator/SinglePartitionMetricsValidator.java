package com.azure.diagnosticsValidator;

import com.azure.models.Diagnostics;

public class SinglePartitionMetricsValidator implements IDiagnosticsValidator {
    private final String partitionKeyRangeId;

    public SinglePartitionMetricsValidator(String pkRangeId) {
        this.partitionKeyRangeId = pkRangeId;
    }

    @Override
    public boolean validateDiagnostics(Diagnostics diagnostics) {
        return diagnostics
                .getResponseStatisticsList()
                .stream().anyMatch(storeResultWrapper -> {
                    return storeResultWrapper.getStoreResult().getStorePhysicalAddress().contains(partitionKeyRangeId);
//                    return storeResultWrapper.getStoreResult().getPartitionKeyRangeId() != null
//                            && storeResultWrapper.getStoreResult().getPartitionKeyRangeId().equals(this.partitionKeyRangeId);
                });
    }
}
