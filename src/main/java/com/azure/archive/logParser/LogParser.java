package com.azure.archive.logParser;

import com.azure.DiagnosticsHandler;
import com.azure.SummaryRecorder;
import com.azure.common.DiagnosticsHelper;
import com.azure.diagnosticsValidator.TransportEventDurationValidator;
import com.azure.metricsRecorder.ExceptionMetricsRecorder;
import com.azure.metricsRecorder.Retry410MetricsRecorder;
import com.azure.metricsRecorder.SimpleTimelineAnalysisRecorder;
import com.azure.metricsRecorder.latency.AddressResolutionMetricsRecorder;
import com.azure.metricsRecorder.latency.BackendLatencyMetricsRecorder;
import com.azure.metricsRecorder.latency.RequestLatencyMetricsRecorder;
import com.azure.metricsRecorder.latency.TransportLatencyMetricsRecorder;
import com.azure.models.Diagnostics;
import com.azure.models.StoreResultWrapper;
import com.azure.models.TransportTimelineEventName;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LogParser {
    private static final Logger logger = LoggerFactory.getLogger(LogParser.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static void main(String[] args) throws Exception {
        //String matchingString = ".*WARN  site.ycsb.db.AzureCosmosClient  -(.*)";
        String matchingString = "GET took more than: 100 ms. Total duration: (.*). Collection name: (.*). Key: (.*). ActivityId: (.*). Diagnostics (.*)\\.";
        Pattern pattern = Pattern.compile(matchingString, Pattern.CASE_INSENSITIVE);

        String logFile = "src/main/java/linkedInUpgrade/byMachine/104562.log";
        String latencyResultPrefix = "src/main/java/linkedInUpgrade/results/";

        File latencyResultDirectory = new File(latencyResultPrefix);
        if (!latencyResultDirectory.exists()) {
            latencyResultDirectory.mkdir();
        }

        // Add or remove the transport event you want to analysis
        List<TransportTimelineEventName> trackingEvents = Arrays.asList(
                TransportTimelineEventName.CHANNEL_ACQUISITION,
                TransportTimelineEventName.PIPELINED,
                TransportTimelineEventName.TRANSIT);

        File summaryFile = new File(latencyResultPrefix + "summary.txt");
        PrintWriter summaryFileWriter = new PrintWriter(summaryFile);
        SummaryRecorder summaryRecorder = new SummaryRecorder(latencyResultPrefix);
        summaryRecorder.setPrintWriter(summaryFileWriter);

        DiagnosticsHandler diagnosticsParser = new DiagnosticsHandler(Duration.ofMinutes(1), latencyResultPrefix, summaryRecorder);
        // create the directory for the result
        String[] directoryNameParts = logFile.split("/");
        String[] fileNameParts = directoryNameParts[directoryNameParts.length-1].split("\\.");
        String directoryName = fileNameParts[1];

        String latencyResultFullPrefix = latencyResultPrefix + "/" + directoryName + "/";
        File directory = new File(latencyResultFullPrefix);
        if (!directory.exists()) {
            directory.mkdir();
        }

        try {
            diagnosticsParser.registerMetricsValidator(new TransportEventDurationValidator());
           // diagnosticsParser.registerMetricsValidator(new SingleServerValidator("rntbd://cdb-ms-prod-westus2-fd46.documents.azure.com:14101"));
            //diagnosticsParser.registerMetricsValidator(new SinglePartitionMetricsValidator("1166"));
            //diagnosticsParser.registerMetricsValidator(new RequestLatencyValidator(1000, 300000));
            diagnosticsParser.registerMetricsRecorder(new RequestLatencyMetricsRecorder(latencyResultFullPrefix));
            diagnosticsParser.registerMetricsRecorder(new BackendLatencyMetricsRecorder(latencyResultFullPrefix, summaryRecorder));
            diagnosticsParser.registerMetricsRecorder(new AddressResolutionMetricsRecorder(latencyResultFullPrefix, summaryRecorder));
            //  diagnosticsParser.registerMetricsRecorder(new InflightRequestsMetricsRecorder(latencyResultFullPrefix));

            for (TransportTimelineEventName eventName : trackingEvents) {
                diagnosticsParser.registerMetricsRecorder(
                    new TransportLatencyMetricsRecorder(eventName, latencyResultFullPrefix, summaryRecorder, false));
            }

            diagnosticsParser.registerMetricsRecorder(new Retry410MetricsRecorder(latencyResultFullPrefix));
            diagnosticsParser.registerMetricsRecorder(new ExceptionMetricsRecorder(latencyResultFullPrefix, false));
            diagnosticsParser.registerMetricsRecorder(new SimpleTimelineAnalysisRecorder(latencyResultFullPrefix));

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        try(BufferedReader br = new BufferedReader(new FileReader(logFile))) {
            String line;
            int lineIndex = 0;
            int failedOnes = 0;

            while((line = br.readLine()) != null) {
                if (StringUtils.isEmpty(line)) {
                    continue;
                }

                Matcher matcher = pattern.matcher(line);
                if (matcher.find()) {
                    lineIndex++;
                    String logString = matcher.group(5).replace("\"\"", "\"");
                    Diagnostics diagnostics = null;
                    try {
                        JsonNode log = objectMapper.readTree(logString);
                        diagnostics = objectMapper.convertValue(log, Diagnostics.class);
                    } catch (Exception e) {
                        failedOnes++;
                        continue;
                    }
                    diagnostics.setLogLine(logString);

                    // For some exception, the pkRangeId may miss, backfill the info
                    String pkRangeId = DiagnosticsHelper.getPartitionKeyRangeId(diagnostics);
                    for (StoreResultWrapper storeResultWrapper: diagnostics.getResponseStatisticsList()) {
                        storeResultWrapper.getStoreResult().setPartitionKeyRangeId(pkRangeId);
                    }
                    //summaryRecorder.recordPartitionLog(diagnostics);
                    //summaryRecorder.recordServerLog(diagnostics);
                    diagnosticsParser.processDiagnostics(diagnostics);
                } else {
                    //logger.warn("Cannot find matching pattern {}", line);
                }
            }
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println();
        System.out.println();
        System.out.println("Flush result for " + logFile);

        summaryFileWriter.println();
        summaryFileWriter.println();
        summaryFileWriter.println("Flush result for " + logFile);
        // send an ending signal
        diagnosticsParser.flush();
        diagnosticsParser.close();

        System.out.println();
        System.out.println();
        System.out.println("Total Request:" + summaryRecorder.getTotalRequests());
        System.out.println("Max RequestLatency:" + summaryRecorder.getMaxRequestLatency());
        System.out.println("Max RequestLatencyLog:" + summaryRecorder.getMaxRequestLatencyLog());
        System.out.println("Max backendLatency:" + summaryRecorder.getMaxBackendLatency());
        System.out.println(
                String.format(
                        "Request retry: 1 retry [%d], 2 retries [%d], >=2 retires [%d], retriesOnSameEndpoint [%d]",
                        summaryRecorder.getRetryOnce(),
                        summaryRecorder.getRetryTwice(),
                        summaryRecorder.getRetryMoreThanTwo(),
                        summaryRecorder.getRetryOnSameEndpoint()
                ));
        System.out.println("Exception count by category " + summaryRecorder.getErrors());

        System.out.println("ConnectionTimeoutOnUnknown : " + summaryRecorder.getConnectionTimeoutOnUnknown().values().stream().reduce(0, (initialValue, count) -> initialValue + count));
        System.out.println("ConnectionTimeoutOnConnected0 : " + summaryRecorder.getConnectionTimeoutOnConnected0().values().stream().reduce(0, (initialValue, count) -> initialValue + count));
        System.out.println("ConnectionTimeoutOnConnected : " + summaryRecorder.getConnectionTimeoutOnConnected().values().stream().reduce(0, (initialValue, count) -> initialValue + count));
        for (String serverKey : summaryRecorder.getConnectionTimeoutOnConnected().keySet()) {
            System.out.println(serverKey + ": " + summaryRecorder.getConnectionTimeoutOnConnected().get(serverKey));
        }
        System.out.println("ConnectionTimeoutOnOthers : " + summaryRecorder.getConnectionTimeoutOnOthers().values().stream().reduce(0, (initialValue, count) -> initialValue + count));

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

        summaryRecorder.clearServerLogFolder();

        summaryFileWriter.flush();
        summaryFileWriter.close();
        summaryRecorder.close();
    }
}
