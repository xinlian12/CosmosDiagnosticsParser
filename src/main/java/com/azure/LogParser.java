package com.azure;

import com.azure.diagnosticsValidator.TransportEventDurationValidator;
import com.azure.metricsRecorder.BackendLatencyMetricsRecorder;
import com.azure.metricsRecorder.InflightRequestsMetricsRecorder;
import com.azure.metricsRecorder.RequestLatencyMetricsRecorder;
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
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LogParser {
    private static final Logger logger = LoggerFactory.getLogger(LogParser.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static void main(String[] args) {
        String matchingString = ".*WARN  site.ycsb.db.AzureCosmosClient  -(.*)";
        Pattern pattern = Pattern.compile(matchingString, Pattern.CASE_INSENSITIVE);

        String logSourceFile = "src/main/java/Benchmarking-vm1-ycsb.log";
        String latencyResultPrefix = "src/main/java/metricsResult/";

        // Add or remove the transport event you want to analysis
        List<TransportTimelineEventName> trackingEvents = Arrays.asList(
                TransportTimelineEventName.QUEUED,
                TransportTimelineEventName.CHANNEL_ACQUISITION,
                TransportTimelineEventName.PIPELINED,
                TransportTimelineEventName.TRANSIT,
                TransportTimelineEventName.DECODE,
                TransportTimelineEventName.RECEIVED,
                TransportTimelineEventName.COMPLETED);

        DiagnosticsHandler diagnosticsParser = new DiagnosticsHandler(Duration.ofMinutes(1), latencyResultPrefix);
        try {
            diagnosticsParser.registerMetricsValidator(new TransportEventDurationValidator());

            diagnosticsParser.registerMetricsRecorder(new RequestLatencyMetricsRecorder(latencyResultPrefix));
            diagnosticsParser.registerMetricsRecorder(new BackendLatencyMetricsRecorder(latencyResultPrefix));
            diagnosticsParser.registerMetricsRecorder(new InflightRequestsMetricsRecorder(latencyResultPrefix));

            for (TransportTimelineEventName eventName : trackingEvents) {
                diagnosticsParser.registerMetricsRecorder(new TransportLatencyMetricsRecorder(eventName, latencyResultPrefix));
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        try(BufferedReader br = new BufferedReader(new FileReader(logSourceFile))) {
            String line;
            int lineIndex = 0;

            while((line = br.readLine()) != null) {
                if (StringUtils.isEmpty(line)) {
                    continue;
                }

                Matcher matcher = pattern.matcher(line);
                if (matcher.find()) {
                    lineIndex++;
                    System.out.println(lineIndex);
                    JsonNode log = objectMapper.readTree(matcher.group(1));
                    Diagnostics diagnostics = objectMapper.convertValue(log, Diagnostics.class);
                    diagnostics.setLogLine(matcher.group(1));

                    diagnosticsParser.processDiagnostics(diagnostics);
                } else {
                    //logger.warn("Cannot find matching pattern {}", line);
                }
            }

            // send an ending signal
            diagnosticsParser.flush();
            diagnosticsParser.close();
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
