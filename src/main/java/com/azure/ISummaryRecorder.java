package com.azure;

import com.azure.models.Diagnostics;
import com.azure.models.ExceptionCategory;

import java.io.PrintWriter;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public interface ISummaryRecorder {
    PrintWriter getPrintWriter();

    void setPrintWriter(PrintWriter printWriter);

    void recordErrors(ConcurrentHashMap<ExceptionCategory, AtomicInteger> errors);
    void recordServerErrors(ConcurrentHashMap<String, Integer> errorByServer);

    void recordRetries(int retryOnce, int retryTwice, int retryMoreThanTwo, int retriesOnTheSameEndpoint);

    void recordMaxRequestLatency(double requestLatency, String log);

    void recordTotalRequests(int totalRequests);

    void recordMaxBackendRequestLatency(double backendRequestLatency);

    void recordPartitionLog(Diagnostics diagnostics);
    void recordServerLog(Diagnostics diagnostics);

    int getTotalRequests();
    double getMaxRequestLatency();
    String getMaxRequestLatencyLog();
    double getMaxBackendLatency();
    ConcurrentHashMap<ExceptionCategory, AtomicInteger> getErrors();
    ConcurrentHashMap<String, Integer> getServerErrors();
    int getRetryOnce();
    int getRetryTwice();
    int getRetryMoreThanTwo();
    int getRetryOnSameEndpoint();

    void startNewDiagnostics(Diagnostics diagnostics);
    void recordTransitLatency(double latency);
    void recordBackendLatency(double latency);

    void recordBackoff429Latency(double latency);

    void recordBackoff410Latency(double latency);

    void recordChannelAcquisition(double latency);
    void recordAddressResolution(double latency);
    void recordConnectionTimeoutOnUnknown(Map<String, Integer> countByServer);
    void recordConnectionTimeoutOnConnected(Map<String, Integer> countByServer);
    void recordConnectionTimeoutOnConnected0(Map<String, Integer> countByServer);
    void recordConnectionTimeoutOnOthers(Map<String, Integer> countByServer);
    ConcurrentHashMap<String, Integer> getConnectionTimeoutOnUnknown();
    ConcurrentHashMap<String, Integer> getConnectionTimeoutOnConnected();
    ConcurrentHashMap<String, Integer> getConnectionTimeoutOnConnected0();
    ConcurrentHashMap<String, Integer> getConnectionTimeoutOnOthers();

    ConcurrentHashMap<String, Integer> getHighLatencyMap();
    Set<String> getHighTransientTimeEndpoints();

    void close();
    void clearServerLogFolder();
}
