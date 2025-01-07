package com.azure;

import com.azure.archive.logParser.LogParser;
import com.azure.common.DiagnosticsHelper;
import com.azure.diagnosticsValidator.ExceptionsValidator;
import com.azure.diagnosticsValidator.RequestLatencyValidator;
import com.azure.diagnosticsValidator.TransportEventDurationValidator;
import com.azure.metricsRecorder.ExceptionMetricsRecorder;
import com.azure.metricsRecorder.PlainLatencyRecorder;
import com.azure.metricsRecorder.SimpleTimelineAnalysisRecorder;
import com.azure.metricsRecorder.latency.AddressResolutionMetricsRecorder;
import com.azure.metricsRecorder.latency.BackendLatencyMetricsRecorder;
import com.azure.metricsRecorder.latency.RequestLatencyMetricsRecorder;
import com.azure.metricsRecorder.latency.TransportLatencyMetricsRecorder;
import com.azure.models.Diagnostics;
import com.azure.models.StoreResultWrapper;
import com.azure.models.TransportTimelineEventName;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class UpgradeLogParserGateway {
    private static final Logger logger = LoggerFactory.getLogger(UpgradeLogParserGateway.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static void main(String[] args) throws Exception {
        String matchingString = ".*WARN  site.ycsb.db.AzureCosmosClient \\[\\] -(.*)";
        Pattern pattern = Pattern.compile(matchingString, Pattern.CASE_INSENSITIVE);

        // Decide how many machines you want to analysis
        for (int i = 1; i <= 1; i++) {
            String logSourceDirectory = String.format("C:/Users/nakumars/computeData/computetestread-2024-11-14-03h21m03s/computetestread-vm%s-system-diagnostics/cosmos_client_logs/cosmos_diagnostics/read", i);
            String latencyResultPrefix = String.format("C:/Users/nakumars/computeData/computetestread-2024-11-14-03h21m03s/parsingResult/read/vm%d/", i);
            System.out.println("Parsing log from directory: " + logSourceDirectory);

            File latencyResultDirectory = new File(latencyResultPrefix);
            if (!latencyResultDirectory.exists()) {
                latencyResultDirectory.mkdir();
            }

            // Add or remove the transport event you want to analysis
            List<TransportTimelineEventName> trackingEvents = Arrays.asList(
                TransportTimelineEventName.CONNECTION_ACQUIRED,
                TransportTimelineEventName.CONNECTION_CREATED,
                TransportTimelineEventName.CONNECTION_CONFIGURED,
                TransportTimelineEventName.TRANSIT);

            List<String> allLogFiles = findAllLogFiles(logSourceDirectory);
            File summaryFile = new File(latencyResultPrefix + "summary.txt");
            PrintWriter summaryFileWriter = new PrintWriter(summaryFile);
            SummaryRecorder summaryRecorder = new SummaryRecorder(latencyResultPrefix);
            summaryRecorder.setPrintWriter(summaryFileWriter);
            PlainLatencyRecorder plainLatencyRecorder = new PlainLatencyRecorder(latencyResultPrefix);
            for (String logFile : allLogFiles) {
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

                    diagnosticsParser.registerMetricsValidator(new RequestLatencyValidator(50, 300000));
                    diagnosticsParser.registerMetricsRecorder(new RequestLatencyMetricsRecorder(latencyResultFullPrefix));


                    for (TransportTimelineEventName eventName : trackingEvents) {
                        diagnosticsParser.registerMetricsRecorder(
                            new TransportLatencyMetricsRecorder(eventName, latencyResultFullPrefix, summaryRecorder, true));
                    }

                    //diagnosticsParser.registerMetricsRecorder(new ExceptionMetricsRecorder(latencyResultFullPrefix, true));

                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }

                try(BufferedReader br = new BufferedReader(new FileReader(logFile))) {
                    String line;
                    int lineIndex = 0;

                    while((line = br.readLine()) != null) {
                        if (StringUtils.isEmpty(line)) {
                            continue;
                        }

                        Matcher matcher = pattern.matcher(line);
                        if (matcher.find()) {
                            lineIndex++;
                            JsonNode log = objectMapper.readTree(matcher.group(1));
                            Diagnostics diagnostics = null;
                            try {
                                diagnostics = objectMapper.convertValue(log, Diagnostics.class);
                            } catch (Exception e) {
                                System.out.println(matcher.group(1));
                                System.out.println(e.getMessage());
                            }
                            diagnostics.setLogLine(matcher.group(1));

                            // For some exception, the pkRangeId may miss, backfill the info
                            String pkRangeId = DiagnosticsHelper.getPartitionKeyRangeId(diagnostics);
                            for (StoreResultWrapper storeResultWrapper: diagnostics.getResponseStatisticsList()) {
                                storeResultWrapper.getStoreResult().setPartitionKeyRangeId(pkRangeId);
                            }
                            // if you want to group the logs by partition or by server
                            // summaryRecorder.recordPartitionLog(diagnostics);
                            // summaryRecorder.recordServerLog(diagnostics);
                            diagnosticsParser.processDiagnostics(diagnostics);
                            plainLatencyRecorder.processDiagnostics(diagnostics);
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
                // send an ending signal:q
                diagnosticsParser.flush();
                diagnosticsParser.close();
            }

            System.out.println();
            System.out.println();
            System.out.println("Total Request:" + summaryRecorder.getTotalRequests());
            System.out.println("Max RequestLatency:" + summaryRecorder.getMaxRequestLatency());
            System.out.println("Max RequestLatencyLog:" + summaryRecorder.getMaxRequestLatencyLog());
            System.out.println("Max backendLatency:" + summaryRecorder.getMaxBackendLatency());
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

    private static List<String> findAllLogFiles(String directory) throws IOException {
        try (Stream<Path> walk = Files.walk(Paths.get(directory))) {
            return walk
                .filter(p -> !Files.isDirectory(p))   // not a directory
                .map(p -> p.toString().toLowerCase()) // convert path to string
                .filter(f -> f.endsWith(".log"))       // check end with
                .collect(Collectors.toList());        // collect all matched to a List
        }
    }
}
