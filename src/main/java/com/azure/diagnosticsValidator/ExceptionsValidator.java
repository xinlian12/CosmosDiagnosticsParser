package com.azure.diagnosticsValidator;

import com.azure.models.Diagnostics;

public class ExceptionsValidator implements IDiagnosticsValidator {
    @Override
    public boolean validateDiagnostics(Diagnostics diagnostics) {

//        if (diagnostics.getResponseStatisticsList().get(0).getStoreResult().getStatusCode() == 404) {
//             return true;
//        } else {
//            return false;
//        }

        return diagnostics.getResponseStatisticsList()
                        .stream().anyMatch(storeResultWrapper -> storeResultWrapper.getStoreResult().getStatusCode() == 410);

//        // return only logs has 410
//        if (diagnostics.getRetryContext().getStatusAndSubStatusCodes() == null) {
//            return false;
//        }
//
//        boolean exists410 = false;
//
//        for (Integer[] statusCodes : diagnostics.getRetryContext().getStatusAndSubStatusCodes()) {
//            if (statusCodes[0] == 410) {
//                exists410 = true;
//            } else if (statusCodes[0] == 429) {
//                return false;
//            }
//        }
//
////        if (exists410) {
////            System.out.println(diagnostics.getLogLine());
////        }
//        return exists410;
    }
}