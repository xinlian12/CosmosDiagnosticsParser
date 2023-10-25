package com.azure.metricsRecorder;

import com.azure.ISummaryRecorder;
import com.azure.models.Diagnostics;

import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class MachineVMRecorder implements IMetricsRecorder {

    private Set<String> vmIds = ConcurrentHashMap.newKeySet();
    @Override
    public void recordValue(Diagnostics diagnostics) {
        this.vmIds.add(diagnostics.getClientCfgs().getMachineId());
    }

    @Override
    public void recordHistogramSnapshot(Instant recordTimestamp) {

    }

    @Override
    public void reportStatistics(ISummaryRecorder summaryRecorder) {
        System.out.println("VMID: " + this.vmIds);
    }

    @Override
    public void close() throws Exception {
        System.out.println("VMID: " + this.vmIds);
    }
}
