package com.azure;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

public class ServerLogRecorder {
    private final PrintWriter printWriter;

    // the expected serverKey format would be : cdb-ms-prod-northcentralus1-fd3.documents.azure.com:14031
    public ServerLogRecorder(String serverKey, String logPrefix) throws FileNotFoundException {
        String directoryName = logPrefix + serverKey + "/";
        File directoryFile = new File(directoryName);
        if (!directoryFile.exists()) {
            directoryFile.mkdir();
        }

        String fileName = directoryName + serverKey + ".log";
        File logFile = new File(fileName);
        this.printWriter = new PrintWriter(logFile);
    }

    public void recordServerLog(String log) {
        this.printWriter.println(log);
        this.printWriter.flush();
    }

    public void close() {
        if (this.printWriter != null) {
            this.printWriter.close();
        }
    }
}
