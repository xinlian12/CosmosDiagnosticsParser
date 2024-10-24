package com.azure.metricsRecorder;

import com.azure.models.Diagnostics;

import java.io.FileNotFoundException;
import java.io.PrintWriter;

public class PlainLatencyRecorder {

    StringBuilder sb = new StringBuilder();

    PrintWriter printWriter ;
    public PlainLatencyRecorder(String logFilePathPrefix) throws FileNotFoundException {
        String logFilePath = logFilePathPrefix + "PlainLatency" + ".csv";
        this.printWriter = new PrintWriter(logFilePath);
        StringBuilder sb = new StringBuilder();
        sb.append("TIME");
        sb.append(',');
        sb.append("Latency");
        sb.append('\n');
        printWriter.write(sb.toString());
    }
     public void processDiagnostics(Diagnostics diagnostics) {
         StringBuilder sb = new StringBuilder();

         sb.append(diagnostics.getRequestStartTimeUTC());
         sb.append(',');
         sb.append(diagnostics.getRequestLatencyInMs());
         sb.append('\n');
         printWriter.write(sb.toString());
         printWriter.flush();
     }
}
