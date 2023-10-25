package com.azure;

import com.azure.common.DiagnosticsHelper;
import com.azure.cosmos.implementation.apachecommons.lang.StringUtils;
import com.azure.diagnosticsValidator.MachineIdValidator;
import com.azure.metricsRecorder.ExceptionMetricsRecorder;
import com.azure.metricsRecorder.SimpleTimelineAnalysisRecorder;
import com.azure.metricsRecorder.latency.AddressResolutionMetricsRecorder;
import com.azure.metricsRecorder.latency.RequestLatencyMetricsRecorder;
import com.azure.metricsRecorder.latency.TransportLatencyMetricsRecorder;
import com.azure.models.Diagnostics;
import com.azure.models.StoreResultWrapper;
import com.azure.models.TransportEvent;
import com.azure.models.TransportTimelineEventName;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.PrintWriter;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class MasterCardsPointReadLogParser {
    private static final Logger logger = LoggerFactory.getLogger(MasterCardsPointReadLogParser.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static void main(String[] args) throws Exception {
        String logSourceDirectory = "src/main/java/mastercards/25batch500tps_pointread.csv";
        String latencyResultPrefix = "src/main/java/mastercards/parsingresult/1025-pointreads/";

        // Add or remove the transport event you want to analysis
        List<TransportTimelineEventName> trackingEvents = Arrays.asList(
                TransportTimelineEventName.CHANNEL_ACQUISITION,
                TransportTimelineEventName.TRANSIT);

        File summaryFile = new File(latencyResultPrefix + "summary.txt");
        PrintWriter summaryFileWriter = new PrintWriter(summaryFile);
        SummaryRecorder summaryRecorder = new SummaryRecorder(latencyResultPrefix);
        summaryRecorder.setPrintWriter(summaryFileWriter);
        DiagnosticsHandler diagnosticsParser = new DiagnosticsHandler(Duration.ofMinutes(1), latencyResultPrefix, summaryRecorder);

        try {
            //String machineId = "vmId_8fe0219b-1ea5-4c68-b5bc-85d0df204fc0";
            // diagnosticsParser.registerMetricsValidator(new TransportEventDurationValidator());
           // diagnosticsParser.registerMetricsValidator(new MachineIdValidator(machineId));
            // diagnosticsParser.registerMetricsValidator(new SingleServerValidator(serverFilter));
            //  diagnosticsParser.registerMetricsValidator(new SinglePartitionMetricsValidator("1470"));
            //diagnosticsParser.registerMetricsValidator(new RequestLatencyValidator(5000, 300000));
            diagnosticsParser.registerMetricsRecorder(new RequestLatencyMetricsRecorder(latencyResultPrefix));
            // diagnosticsParser.registerMetricsRecorder(new BackendLatencyMetricsRecorder(latencyResultPrefix, summaryRecorder));
            diagnosticsParser.registerMetricsRecorder(new AddressResolutionMetricsRecorder(latencyResultPrefix, summaryRecorder));
            // diagnosticsParser.registerMetricsRecorder(new BackoffLatency429MetricsRecorder(latencyResultFullPrefix, summaryRecorder));
            // diagnosticsParser.registerMetricsRecorder(new BackoffLatency410ByRetryContextMetricsRecorder(latencyResultFullPrefix, summaryRecorder));
            //  diagnosticsParser.registerMetricsRecorder(new InflightRequestsMetricsRecorder(latencyResultFullPrefix));

            for (TransportTimelineEventName eventName : trackingEvents) {
                diagnosticsParser.registerMetricsRecorder(new TransportLatencyMetricsRecorder(eventName, latencyResultPrefix, summaryRecorder));
            }

            // diagnosticsParser.registerMetricsRecorder(new Retry410MetricsRecorder(latencyResultFullPrefix));
            diagnosticsParser.registerMetricsRecorder(new ExceptionMetricsRecorder(latencyResultPrefix));
            diagnosticsParser.registerMetricsRecorder(new SimpleTimelineAnalysisRecorder(latencyResultPrefix));
            //diagnosticsParser.registerMetricsRecorder(new MachineVMRecorder());
            //  diagnosticsParser.registerMetricsRecorder(new SimpleTimelineAnalysisRecorder(latencyResultFullPrefix, serverFilter));

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        int totalWrongLines = 0;
        Map<String, List<String>> endpointSets = new ConcurrentHashMap<String, List<String>>();
        try (BufferedReader reader = new BufferedReader(new FileReader(logSourceDirectory));) {
            String line = null;
            while ((line = reader.readLine()) != null) {
                if (StringUtils.isEmpty(line)) {
                    continue;
                }

                if (!line.contains("userAgent")) {
                    continue;
                }

                line = line.replace("\"\"", "\"");
                line = line.replace("\\\"", "\"");
                line = line.replace("\\\\\"Resource Not Found. Learn more: https://aka.ms/cosmosdb-tsg-not-found\\\\\"", "");
                if (line.startsWith("\"\"")) {
                    line = "{" + line.substring(1, line.length());
                }

                if (line.startsWith("\"{")) {
                    line = line.substring(1, line.length());
                }

                if (line.endsWith("\"")) {
                    line = line.substring(0, line.length() - 1);
                }

                if (line.endsWith("\"proactiveInit\":null}}}")) {
                    // extra { being detected
                    line = line.substring(0, line.length() - 1);
                }

                Diagnostics diagnostics = null;
                try {
                    JsonNode log = objectMapper.readTree(line);
                    diagnostics = objectMapper.convertValue(log, Diagnostics.class);
                } catch (Exception e) {
                    System.out.println(line);
                    continue;
                }
                diagnostics.setLogLine(line);
                // For some exception, the pkRangeId may miss, backfill the info
                String pkRangeId = DiagnosticsHelper.getPartitionKeyRangeId(diagnostics);
                for (StoreResultWrapper storeResultWrapper: diagnostics.getResponseStatisticsList()) {
                    storeResultWrapper.getStoreResult().setPartitionKeyRangeId(pkRangeId);
                    List<TransportEvent> transportEvents =
                            storeResultWrapper
                                    .getStoreResult()
                                    .getTransportRequestTimeline()
                                    .stream()
                                    .filter(transportEvent -> transportEvent.getDurationInMilliSecs() > 4000)
                                    .collect(Collectors.toList());
                    if (transportEvents.size() >= 1) {
                        endpointSets.compute(DiagnosticsHelper.getServerKey(storeResultWrapper), (key, partitionIds) -> {
                            if (partitionIds == null) {
                                partitionIds = new ArrayList<>();
                            }
                            partitionIds.add(DiagnosticsHelper.getPartitionId(storeResultWrapper));
                            return partitionIds;
                        });
                    }
                }
                // if you want to group the logs by partition or by server
              //  summaryRecorder.recordPartitionLog(diagnostics);
              //  summaryRecorder.recordServerLog(diagnostics);
                diagnosticsParser.processDiagnostics(diagnostics);
            }
        }


        diagnosticsParser.flush();
        diagnosticsParser.close();

        System.out.println("Total wrong lines " + totalWrongLines);
        System.out.println();
        System.out.println("Total Request:" + summaryRecorder.getTotalRequests());
        System.out.println("Max RequestLatency:" + summaryRecorder.getMaxRequestLatency());
        System.out.println("Max RequestLatencyLog:" + summaryRecorder.getMaxRequestLatencyLog());
        System.out.println("Max backendLatency:" + summaryRecorder.getMaxBackendLatency());
        System.out.println(
                String.format(
                        "Request 410 retry: 1 retry [%d], 2 retries [%d], >=2 retires [%d], retriesOnSameEndpoint [%d]",
                        summaryRecorder.getRetryOnce(),
                        summaryRecorder.getRetryTwice(),
                        summaryRecorder.getRetryMoreThanTwo(),
                        summaryRecorder.getRetryOnSameEndpoint()
                ));
        System.out.println("Exception count by category " + summaryRecorder.getErrors());
        System.out.println("High latency count by category " + summaryRecorder.getHighLatencyMap());
        System.out.println("Transit timeout by server: " + summaryRecorder.getServerErrors());

        summaryRecorder.getPrintWriter().println();
        summaryRecorder.getPrintWriter().println();
        summaryRecorder.getPrintWriter().println("Total Request:" + summaryRecorder.getTotalRequests());
        summaryRecorder.getPrintWriter().println("Max RequestLatency:" + summaryRecorder.getMaxRequestLatency());
        summaryRecorder.getPrintWriter().println("Max RequestLatencyLog:" + summaryRecorder.getMaxRequestLatencyLog());
        summaryRecorder.getPrintWriter().println("Max backendLatency:" + summaryRecorder.getMaxBackendLatency());
        summaryRecorder.getPrintWriter().println(
                String.format(
                        "Request retry: 1 retry [%d], 2 retries [%d], >=2 retires [%d], retriesOnSameEndpoint [%d]",
                        summaryRecorder.getRetryOnce(),
                        summaryRecorder.getRetryTwice(),
                        summaryRecorder.getRetryMoreThanTwo(),
                        summaryRecorder.getRetryOnSameEndpoint()
                ));
        summaryRecorder.getPrintWriter().println("Exception count by category " + summaryRecorder.getErrors());
        summaryRecorder.getPrintWriter().println("High latency count by category " + summaryRecorder.getHighLatencyMap());
        summaryRecorder.getPrintWriter().println("Transit timeout by server: " + summaryRecorder.getServerErrors());

        summaryFileWriter.flush();
        summaryFileWriter.close();
        summaryRecorder.close();
    }
}
