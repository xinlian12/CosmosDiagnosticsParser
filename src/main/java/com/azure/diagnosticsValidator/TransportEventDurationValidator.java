package com.azure.diagnosticsValidator;

import com.azure.models.Diagnostics;
import com.azure.models.StoreResult;

import java.util.List;
import java.util.stream.Collectors;

public class TransportEventDurationValidator implements IDiagnosticsValidator {
    @Override
    public boolean validateDiagnostics(Diagnostics diagnostics) {
        List<StoreResult> storeResultList = diagnostics.getResponseStatisticsList()
                .stream().map(storeResultWrapper -> storeResultWrapper.getStoreResult())
                .collect(Collectors.toList());

        return storeResultList
                .stream()
                .noneMatch(
                    storeResult ->
                            storeResult
                                    .getTransportRequestTimeline()
                                    .stream()
                                    .anyMatch(transportEvent -> transportEvent.getDurationInMilliSecs() < 0));
    }
}
