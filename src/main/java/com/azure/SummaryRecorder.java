package com.azure;

import com.azure.common.DiagnosticsHelper;
import com.azure.cosmos.implementation.Utils;
import com.azure.models.Diagnostics;
import com.azure.models.ExceptionCategory;
import com.azure.models.StoreResultWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class SummaryRecorder implements ISummaryRecorder{
    private final ConcurrentHashMap<ExceptionCategory, AtomicInteger> errorsByCategory = new ConcurrentHashMap<>();
    private final static String HIGH_TRANSIT_TIME = "Transit";
    private final static String HIGH_CHANNEL_ACQUISITION = "Channel_Acquisition";
    private final static String HIGH_BACKEND_LATENCY = "Backend_Latency";
    private final static String HIGH_ADDRESS_RESOLUTION = "Address_Resolution";
    private final static String HIGH_THROTTLE_BACKOFF_LATENCY = "Throttle_Backoff";
    private final static String HIGH_410_BACKOFF_LATENCY = "410_Backoff";

    private final static String OTHERS = "Others";

    private int totalRequests = 0;
    private double maxRequestLatency = Double.MIN_VALUE;
    private double maxBackendLatency = Double.MIN_VALUE;
    private String maxRequestLatencyLog = null;
    private ConcurrentHashMap<Integer, AtomicInteger> retryCountMap = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, PartitionRecorder> partitionRecorderMap = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, ServerLogRecorder> serverLogRecorderMap = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, Integer> latencyCategoryMap = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, Integer> errorByServer = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, Integer> connectionTimeoutOnUnknown = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, Integer> connectionTimeoutOnConnected = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, Integer> connectionTimeoutOnConnected0 = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, Integer> connectionTimeoutOnOthers = new ConcurrentHashMap<>();

    private int retryOnce = 0;
    private int retryTwice = 0;
    private int retryMoreThanTwice = 0;
    private int retryOnSameEndpint = 0;
    private int retriesOnTheSameEndpoint = 0;
    private final String logPrefix;
    private String partitionLogPrefix;
    private String serverLogPrefix;

    private Double currentHighLatencyValue = Double.MIN_VALUE;
    private String currentHighLatencyCategory;
    private Diagnostics currentHighLatencyDiagnostics;


    public SummaryRecorder(String logPrefix) {
        this.logPrefix = logPrefix;

//        this.partitionLogPrefix = logPrefix + "partitionLog/";
//        File partitionLogDirectory = new File(this.partitionLogPrefix);
//        if (!partitionLogDirectory.exists()) {
//            partitionLogDirectory.mkdir();
//        }
//
//        this.serverLogPrefix = logPrefix + "serverLog/";
//        File serverLogDirectory = new File(this.serverLogPrefix);
//        if (!serverLogDirectory.exists()) {
//            serverLogDirectory.mkdir();
//        }
    }

    private PrintWriter printWriter;

    public PrintWriter getPrintWriter() {
        return this.printWriter;
    }

    public void setPrintWriter(PrintWriter printWriter) {
        this.printWriter = printWriter;
    }

    @Override
    public void recordErrors(ConcurrentHashMap<ExceptionCategory, AtomicInteger> errors) {
        for (ExceptionCategory exception : Collections.list(errors.keys())) {
            this.errorsByCategory.compute(exception, (error, count) -> {
                if (count == null) {
                    count = new AtomicInteger(0);
                }
                count.accumulateAndGet(errors.get(exception).get(), (existingValue, newValue) -> existingValue + newValue);
                return count;
            });
        }
    }

    @Override
    public void recordServerErrors(ConcurrentHashMap<String, Integer> errorByServer) {
        for (String serverKey : Collections.list(errorByServer.keys())) {
            this.errorByServer.compute(serverKey, (error, count) -> {
                if (count == null) {
                    count = 0;
                }

                count += errorByServer.get(serverKey);
                return count;
            });
        }
    }

    @Override
    public void recordRetries(int retryOnce, int retryTwice, int retryMoreThanTwo, int retriesOnTheSameEndpoint) {
        this.retryOnce += retryOnce;
        this.retryTwice += retryTwice;
        this.retryMoreThanTwice += retryMoreThanTwo;
        this.retryOnSameEndpint += retriesOnTheSameEndpoint;
    }

    @Override
    public void recordMaxRequestLatency(double requestLatency, String log) {
        if (this.maxRequestLatency < requestLatency) {
            this.maxRequestLatency = requestLatency;
            this.maxRequestLatencyLog = log;
        }
    }

    @Override
    public void recordTotalRequests(int totalRequests) {
        this.totalRequests += totalRequests;
    }

    @Override
    public void recordMaxBackendRequestLatency(double backendRequestLatency) {
        if (this.maxBackendLatency < backendRequestLatency) {
            this.maxBackendLatency = backendRequestLatency;
        }
    }

    @Override
    public void recordPartitionLog(Diagnostics diagnostics) {
        String pkRangeId = DiagnosticsHelper.getPartitionKeyRangeId(diagnostics);
        PartitionRecorder partitionRecorder = this.partitionRecorderMap.compute(pkRangeId, (partitionKeyRangeId, recorder) -> {
            if (recorder == null) {
                try {
                    recorder = new PartitionRecorder(partitionKeyRangeId, this.partitionLogPrefix);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }

            return recorder;
        });

        partitionRecorder.recordPartitionLog(diagnostics.getLogLine());
    }

    @Override
    public void recordServerLog(Diagnostics diagnostics) {
        for (StoreResultWrapper storeResultWrapper : diagnostics.getResponseStatisticsList()) {
            String serverKey = DiagnosticsHelper.getServerKey(storeResultWrapper);
            ServerLogRecorder serverLogRecorder = this.serverLogRecorderMap.compute(serverKey, (serverKeyString, recorder) -> {
                if (recorder == null) {
                    try {
                        recorder = new ServerLogRecorder(serverKeyString, this.serverLogPrefix);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                }

                return recorder;
            });

            try {
                serverLogRecorder.recordServerLog(Utils.getSimpleObjectMapper().writeValueAsString(storeResultWrapper.getStoreResult()));
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }

    }

    @Override
    public int getTotalRequests() {
        return this.totalRequests;
    }

    @Override
    public double getMaxRequestLatency() {
        return this.maxRequestLatency;
    }

    @Override
    public String getMaxRequestLatencyLog() {
        return this.maxRequestLatencyLog;
    }

    @Override
    public double getMaxBackendLatency() {
        return this.maxBackendLatency;
    }

    @Override
    public ConcurrentHashMap<ExceptionCategory, AtomicInteger> getErrors() {
        return this.errorsByCategory;
    }

    @Override
    public ConcurrentHashMap<String, Integer> getServerErrors() {
        return this.errorByServer;
    }

    @Override
    public int getRetryOnce() {
        return this.retryOnce;
    }

    @Override
    public int getRetryTwice() {
        return this.retryTwice;
    }

    @Override
    public int getRetryMoreThanTwo() {
        return this.retryMoreThanTwice;
    }

    @Override
    public int getRetryOnSameEndpoint() {
        return this.retryOnSameEndpint;
    }

    @Override
    public void startNewDiagnostics(Diagnostics diagnostics) {
        if (StringUtils.isNotEmpty(this.currentHighLatencyCategory)) {
            // decides the real high latency
            // if the current high latency is more than 50% of the high latency category, then put into other's group and re-evaluate
            double requestLatency = this.currentHighLatencyDiagnostics.getRequestLatencyInMs();
            if (requestLatency > this.currentHighLatencyValue * 2) {
                //switch category
                this.currentHighLatencyCategory = OTHERS;
            }

            this.latencyCategoryMap.compute(this.currentHighLatencyCategory, (category, count) -> {
                if (count == null) {
                    count = 0;
                }

                count += 1;
                return count;
            });
        }

        this.currentHighLatencyCategory = null;
        this.currentHighLatencyValue = Double.MIN_VALUE;
        this.currentHighLatencyDiagnostics = diagnostics;
    }

    @Override
    public void recordTransitLatency(double latency) {
        if (this.currentHighLatencyValue < latency) {
            this.currentHighLatencyValue = latency;
            this.currentHighLatencyCategory = HIGH_TRANSIT_TIME;
        }
    }

    @Override
    public void recordBackendLatency(double latency) {
        if (this.currentHighLatencyValue < latency) {
            this.currentHighLatencyValue = latency;
            this.currentHighLatencyCategory = HIGH_BACKEND_LATENCY;
        }
    }

    @Override
    public void recordBackoff429Latency(double latency) {
        if (this.currentHighLatencyValue < latency) {
            this.currentHighLatencyValue = latency;
            this.currentHighLatencyCategory = HIGH_THROTTLE_BACKOFF_LATENCY;
        }
    }

    @Override
    public void recordBackoff410Latency(double latency) {
        if (this.currentHighLatencyValue < latency) {
            this.currentHighLatencyValue = latency;
            this.currentHighLatencyCategory = HIGH_410_BACKOFF_LATENCY;
        }
    }

    @Override
    public void recordChannelAcquisition(double latency) {
        if (this.currentHighLatencyValue < latency) {
            this.currentHighLatencyValue = latency;
            this.currentHighLatencyCategory = HIGH_CHANNEL_ACQUISITION;
        }
    }

    @Override
    public void recordAddressResolution(double latency) {
        if (this.currentHighLatencyValue < latency) {
            this.currentHighLatencyValue = latency;
            this.currentHighLatencyCategory = HIGH_ADDRESS_RESOLUTION;
        }
    }

    @Override
    public void recordConnectionTimeoutOnUnknown(Map<String, Integer> countByServer) {
        for (String serverKey : countByServer.keySet()) {
            this.connectionTimeoutOnUnknown.compute(serverKey, (key, count) -> {
                if (count == null) {
                    count = 0;
                }

                count += countByServer.get(serverKey);
                return count;
            });
        }
    }

    @Override
    public void recordConnectionTimeoutOnConnected(Map<String, Integer> countByServer) {
        for (String serverKey : countByServer.keySet()) {
            this.connectionTimeoutOnConnected.compute(serverKey, (key, count) -> {
                if (count == null) {
                    count = 0;
                }

                count += countByServer.get(serverKey);
                return count;
            });
        }
    }

    @Override
    public void recordConnectionTimeoutOnConnected0(Map<String, Integer> countByServer) {
        for (String serverKey : countByServer.keySet()) {
            this.connectionTimeoutOnConnected0.compute(serverKey, (key, count) -> {
                if (count == null) {
                    count = 0;
                }

                count += countByServer.get(serverKey);
                return count;
            });
        }
    }

    @Override
    public void recordConnectionTimeoutOnOthers(Map<String, Integer> countByServer) {
        for (String serverKey : countByServer.keySet()) {
            this.connectionTimeoutOnOthers.compute(serverKey, (key, count) -> {
                if (count == null) {
                    count = 0;
                }

                count += countByServer.get(serverKey);
                return count;
            });
        }
    }

    @Override
    public ConcurrentHashMap<String, Integer> getConnectionTimeoutOnUnknown() {
        return this.connectionTimeoutOnUnknown;
    }

    @Override
    public ConcurrentHashMap<String, Integer> getConnectionTimeoutOnConnected() {
        return this.connectionTimeoutOnConnected;
    }

    @Override
    public ConcurrentHashMap<String, Integer> getConnectionTimeoutOnOthers() {
        return this.connectionTimeoutOnOthers;
    }

    @Override
    public ConcurrentHashMap<String, Integer> getConnectionTimeoutOnConnected0() {
        return this.connectionTimeoutOnConnected0;
    }

    @Override
    public ConcurrentHashMap<String, Integer> getHighLatencyMap() {
        return this.latencyCategoryMap;
    }

    @Override
    public void close() {
        for (PartitionRecorder partitionRecorder : this.partitionRecorderMap.values()) {
            partitionRecorder.close();
        }

        for (ServerLogRecorder serverLogRecorder : this.serverLogRecorderMap.values()) {
            serverLogRecorder.close();
        }
    }

    @Override
    public void clearServerLogFolder() {
        // clear the server log folder
        File directoryPath = new File(this.serverLogPrefix);
        String[] files = directoryPath.list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return new File(dir, name).isDirectory();
            }
        });

        System.out.println("Total servers contact: " + files.length);
        System.out.println("Servers with transit errors: " + this.errorByServer.size());

        printWriter.println("Total servers contact: " + files.length);
        printWriter.println("Servers with transit errors: " + this.errorByServer.size());
        printWriter.println("ConnectionTimeoutOnUnknown : " + this.connectionTimeoutOnUnknown);
        printWriter.println("ConnectionTimeoutOnConnected : " + this.connectionTimeoutOnConnected);
        printWriter.println("ConnectionTimeoutOnOthers : " + this.connectionTimeoutOnOthers);

        for (String serverLogFileName : files) {
            if (!this.errorByServer.containsKey((serverLogFileName))) {
                try {
                    File logDirectory = new File(this.serverLogPrefix, serverLogFileName);
                    FileUtils.cleanDirectory(logDirectory);
                    logDirectory.delete();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
