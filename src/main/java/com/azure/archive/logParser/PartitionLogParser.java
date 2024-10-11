package com.azure.archive.logParser;

import com.azure.DiagnosticsHandler;
import com.azure.SummaryRecorder;
import com.azure.diagnosticsValidator.TransportEventDurationValidator;
import com.azure.metricsRecorder.ExceptionMetricsRecorder;
import com.azure.metricsRecorder.Retry410MetricsRecorder;
import com.azure.metricsRecorder.SimpleTimelineAnalysisRecorder;
import com.azure.metricsRecorder.latency.AddressResolutionMetricsRecorder;
import com.azure.metricsRecorder.latency.BackendLatencyMetricsRecorder;
import com.azure.metricsRecorder.latency.RequestLatencyMetricsRecorder;
import com.azure.metricsRecorder.latency.TransportLatencyMetricsRecorder;
import com.azure.models.Diagnostics;
import com.azure.models.TransportTimelineEventName;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PartitionLogParser {
    private static final Logger logger = LoggerFactory.getLogger(LogParser.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static void main(String[] args) throws Exception {

        for (int i = 1; i <= 5; i++) {
            for (int j = 0; j <= 1666; j++){
                String logSourceDirectory = String.format("src/main/java/upgrade/parsingResult/fix/vm%d/partitionLog/%d/", i, j);
                String latencyResultPrefix = String.format("src/main/java/upgrade/parsingResult/fix/vm%d/partitionLog/%d/", i, j);
                System.out.println();
                System.out.println();
                System.out.println("Parsing log from directory: " + logSourceDirectory);

                File latencyResultDirectory = new File(latencyResultPrefix);
                if (!latencyResultDirectory.exists()) {
                    latencyResultDirectory.mkdir();
                }

                // Add or remove the transport event you want to analysis
                List<TransportTimelineEventName> trackingEvents = Arrays.asList(
                        TransportTimelineEventName.CHANNEL_ACQUISITION,
                        TransportTimelineEventName.PIPELINED,
                        TransportTimelineEventName.TRANSIT);

                List<String> allLogFiles = findAllLogFiles(logSourceDirectory);
                File summaryFile = new File(latencyResultPrefix + "summary.txt");
                PrintWriter summaryFileWriter = new PrintWriter(summaryFile);
                SummaryRecorder summaryRecorder = new SummaryRecorder(latencyResultPrefix);
                summaryRecorder.setPrintWriter(summaryFileWriter);

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
                        diagnosticsParser.registerMetricsValidator(new TransportEventDurationValidator());
                        //diagnosticsParser.registerMetricsValidator(new SingleServerValidator("cdb-ms-prod-westus2-fd22.documents.azure.com:14317"));
                        //diagnosticsParser.registerMetricsValidator(new RequestLatencyValidator(1000, 300000));
                        diagnosticsParser.registerMetricsRecorder(new RequestLatencyMetricsRecorder(latencyResultFullPrefix));
                        diagnosticsParser.registerMetricsRecorder(new BackendLatencyMetricsRecorder(latencyResultFullPrefix, summaryRecorder));
                        diagnosticsParser.registerMetricsRecorder(new AddressResolutionMetricsRecorder(latencyResultFullPrefix, summaryRecorder));
                        diagnosticsParser.registerMetricsRecorder(new SimpleTimelineAnalysisRecorder(latencyResultFullPrefix));
                        //  diagnosticsParser.registerMetricsRecorder(new InflightRequestsMetricsRecorder(latencyResultFullPrefix));

                        for (TransportTimelineEventName eventName : trackingEvents) {
                            diagnosticsParser.registerMetricsRecorder(
                                new TransportLatencyMetricsRecorder(eventName, latencyResultFullPrefix, summaryRecorder, false));
                        }

                        diagnosticsParser.registerMetricsRecorder(new Retry410MetricsRecorder(latencyResultFullPrefix));
                        diagnosticsParser.registerMetricsRecorder(new ExceptionMetricsRecorder(latencyResultFullPrefix, false));

                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }

                    try(BufferedReader br = new BufferedReader(new FileReader(logFile))) {
                        String line;
                        int logline = 0;

                        while((line = br.readLine()) != null) {
                            if (StringUtils.isEmpty(line)) {
                                continue;
                            }

                            logline++;
                            JsonNode log = objectMapper.readTree(line);
                            Diagnostics diagnostics = objectMapper.convertValue(log, Diagnostics.class);
                            diagnostics.setLogLine(line);
                            if (diagnostics.getResponseStatisticsList() == null) {
                                System.out.println("something is wrong");
                            }
                            diagnosticsParser.processDiagnostics(diagnostics);
                        }
                    } catch (FileNotFoundException ex) {
                        ex.printStackTrace();
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    // send an ending signal
                    diagnosticsParser.flush();
                    diagnosticsParser.close();
                }

                System.out.println("High latency count by category " + summaryRecorder.getHighLatencyMap());
                summaryRecorder.getPrintWriter().println("High latency count by category " + summaryRecorder.getHighLatencyMap());

                summaryFileWriter.flush();
                summaryFileWriter.close();

                summaryRecorder.close();
            }
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
