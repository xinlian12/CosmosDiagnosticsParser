package com.azure.archive.logParser;

import com.azure.DiagnosticsHandler;
import com.azure.SummaryRecorder;
import com.azure.diagnosticsValidator.TransportEventDurationValidator;
import com.azure.metricsRecorder.ExceptionMetricsRecorder;
import com.azure.metricsRecorder.SimpleTimelineAnalysisRecorder;
import com.azure.metricsRecorder.latency.TransportLatencyMetricsRecorder;
import com.azure.models.Diagnostics;
import com.azure.models.StoreResult;
import com.azure.models.StoreResultWrapper;
import com.azure.models.TransportTimelineEventName;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ServerLogParser {
    private static final Logger logger = LoggerFactory.getLogger(ServerLogParser.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static void main(String[] args) throws Exception {

        for (int i = 1; i <= 5; i++) {
            String baseDirectory = String.format("src/main/java/upgrade/parsingResult/fix/vm%d/serverLog/", i);
            // First find all log folders
            List<String> subFolders = findAllSubFolders(baseDirectory);
            for (String subFolderName : subFolders) {
                String logSourceDirectory = String.format("src/main/java/upgrade/parsingResult/fix/vm%d/serverLog/%s/", i, subFolderName);
                String latencyResultPrefix = String.format("src/main/java/upgrade/parsingResult/fix/vm%d/serverLog/%s/log/", i, subFolderName);
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

                List<String> allLogFiles = findAllLogFiles(logSourceDirectory, subFolderName);
                File summaryFile = new File(latencyResultPrefix + "summary.txt");
                PrintWriter summaryFileWriter = new PrintWriter(summaryFile);
                SummaryRecorder summaryRecorder = new SummaryRecorder(latencyResultPrefix);
                summaryRecorder.setPrintWriter(summaryFileWriter);

                for (String logFile : allLogFiles) {
                    DiagnosticsHandler diagnosticsParser = new DiagnosticsHandler(Duration.ofMinutes(1), latencyResultPrefix, summaryRecorder);

                    try {
                        diagnosticsParser.registerMetricsValidator(new TransportEventDurationValidator());
                        //diagnosticsParser.registerMetricsValidator(new SingleServerValidator("cdb-ms-prod-westus2-fd22.documents.azure.com:14317"));
                        //diagnosticsParser.registerMetricsValidator(new RequestLatencyValidator(1000, 300000));
                        diagnosticsParser.registerMetricsRecorder(new SimpleTimelineAnalysisRecorder(latencyResultPrefix));
                        //  diagnosticsParser.registerMetricsRecorder(new InflightRequestsMetricsRecorder(latencyResultFullPrefix));

                        for (TransportTimelineEventName eventName : trackingEvents) {
                            diagnosticsParser.registerMetricsRecorder(new TransportLatencyMetricsRecorder(eventName, latencyResultPrefix, summaryRecorder));
                        }

                        diagnosticsParser.registerMetricsRecorder(new ExceptionMetricsRecorder(latencyResultPrefix));

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
                            JsonNode log = null;
                            try {
                                log = objectMapper.readTree(line);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }

                            // for server logs, each log line represents a storeResult
                            StoreResult storeResult = objectMapper.convertValue(log, StoreResult.class);
                            StoreResultWrapper storeResultWrapper = new StoreResultWrapper(storeResult);
                            Diagnostics diagnostics = new Diagnostics();
                            diagnostics.setResponseStatisticsList(Arrays.asList(storeResultWrapper));
                            diagnostics.setRequestStartTimeUTC(storeResult.getTransportRequestTimeline().get(0).getStartTimeUTC());
                            diagnostics.setLogLine(line);
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

    private static List<String> findAllLogFiles(String directory, String fileName) throws IOException {
        File directoryPath = new File(directory);
        List<String> files =  Arrays.asList(directoryPath.list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.equals(fileName + ".log");
            }
        }));

        return files.stream().map(filePath -> directory + filePath).collect(Collectors.toList());
    }

    private static List<String> findAllSubFolders(String directory) {
        File directoryPath = new File(directory);
        return Arrays.asList(directoryPath.list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return new File(dir, name).isDirectory()
                        && !name.equals("log");
            }
        }));
    }
}
