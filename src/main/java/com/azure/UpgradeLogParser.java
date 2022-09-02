package com.azure;

import com.azure.diagnosticsValidator.SinglePartitionMetricsValidator;
import com.azure.diagnosticsValidator.SingleServerValidator;
import com.azure.diagnosticsValidator.TransportEventDurationValidator;
import com.azure.metricsRecorder.BackendLatencyMetricsRecorder;
import com.azure.metricsRecorder.RequestLatencyMetricsRecorder;
import com.azure.metricsRecorder.SimpleTimelineAnalysisRecorder;
import com.azure.metricsRecorder.TimelineAnalysisRecorder;
import com.azure.metricsRecorder.TransportLatencyMetricsRecorder;
import com.azure.models.Diagnostics;
import com.azure.models.TransportTimelineEventName;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
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

public class UpgradeLogParser {
    private static final Logger logger = LoggerFactory.getLogger(LogParser.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static void main(String[] args) throws Exception {
        String matchingString = ".*WARN  site.ycsb.db.AzureCosmosClient \\[\\] -(.*)";
        Pattern pattern = Pattern.compile(matchingString, Pattern.CASE_INSENSITIVE);

        String logSourceDirectory = "src/main/java/ClientDiagnoticsRead11-13Aug/Read11-13Diagnostic/Machine5-cosmos_client_diagnostic_logs/read";
        String latencyResultPrefix = "src/main/java/upgrade/machine5/metricsResult/";

        // Add or remove the transport event you want to analysis
        List<TransportTimelineEventName> trackingEvents = Arrays.asList(
               // TransportTimelineEventName.QUEUED,
                TransportTimelineEventName.CHANNEL_ACQUISITION,
             //   TransportTimelineEventName.PIPELINED,
                TransportTimelineEventName.TRANSIT
             //   TransportTimelineEventName.DECODE,
             //   TransportTimelineEventName.RECEIVED,
             //   TransportTimelineEventName.COMPLETED
                //
                );

        DiagnosticsHandler diagnosticsParser = new DiagnosticsHandler(Duration.ofMinutes(1), latencyResultPrefix);
        try {
            diagnosticsParser.registerMetricsValidator(new TransportEventDurationValidator());
//            diagnosticsParser.registerMetricsValidator(new SinglePartitionMetricsValidator("133"));
//            diagnosticsParser.registerMetricsValidator(new SingleServerValidator("cdb-ms-prod-southcentralus1-fd27.documents.azure.com:14077"));

       //     diagnosticsParser.registerMetricsRecorder(new SimpleTimelineAnalysisRecorder(latencyResultPrefix));
            diagnosticsParser.registerMetricsRecorder(new RequestLatencyMetricsRecorder(latencyResultPrefix));
            diagnosticsParser.registerMetricsRecorder(new BackendLatencyMetricsRecorder(latencyResultPrefix));
            for (TransportTimelineEventName eventName : trackingEvents) {
                diagnosticsParser.registerMetricsRecorder(new TransportLatencyMetricsRecorder(eventName, latencyResultPrefix));
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        List<String> allLogFiles = findAllLogFiles(logSourceDirectory);

        for (String logFile : allLogFiles) {
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
                       // System.out.println(lineIndex);
                        JsonNode log = objectMapper.readTree(matcher.group(1));
                        Diagnostics diagnostics = objectMapper.convertValue(log, Diagnostics.class);
                        diagnostics.setLogLine(matcher.group(1));

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
        }

        // send an ending signal
        diagnosticsParser.flush();
        diagnosticsParser.close();
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
