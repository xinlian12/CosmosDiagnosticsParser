package com.azure.common;

import com.azure.models.Diagnostics;
import com.azure.models.StoreResultWrapper;
import org.apache.commons.lang3.StringUtils;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DiagnosticsHelper {
    private static final String physicalAddressMatchString = "rntbd://(.*)[.]documents[.]azure[.]com[:](.*)/apps/(.*)/services/(.*)/partitions/(.*)/replicas/(.*)/";
   // private static final String physicalAddressMatchString = "rntbd://(.*)[.]documents-test[.]windows-int[.]net[:](.*)/apps/(.*)/services/(.*)/partitions/(.*)/replicas/(.*)/";

    private static final Pattern physicalAddressMatchPattern = Pattern.compile(physicalAddressMatchString);

    public static String getPartitionKeyRangeId(Diagnostics diagnostics) {
        if (diagnostics.getResponseStatisticsList() == null) {
            System.out.println("something is wrong");
        }
        Optional<StoreResultWrapper> storeResultWrapperOpt =  diagnostics
                        .getResponseStatisticsList()
                        .stream()
                        .filter(storeResultWrapper -> StringUtils.isNotEmpty(storeResultWrapper.getStoreResult().getPartitionKeyRangeId()))
                        .findFirst();

        if (storeResultWrapperOpt.isPresent()) {
            return storeResultWrapperOpt.get().getStoreResult().getPartitionKeyRangeId();
        } else {
            return "Unknown";
        }

    }

    public static String getServerKey(StoreResultWrapper storeResultWrapper) {
        Matcher matcher = physicalAddressMatchPattern.matcher(storeResultWrapper.getStoreResult().getStorePhysicalAddress());
        if (matcher.matches()) {
            return matcher.group(1) + "_" + matcher.group(2);
        }

        return "Unknown";
    }

    public static String getPartitionId(StoreResultWrapper storeResultWrapper) {
        Matcher matcher = physicalAddressMatchPattern.matcher(storeResultWrapper.getStoreResult().getStorePhysicalAddress());
        if (matcher.matches()) {
            return matcher.group(5);
        }

        return "Unknown";
    }
}
