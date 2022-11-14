package com.azure;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

public class PartitionRecorder {
    private final PrintWriter printWriter;

    public PartitionRecorder(String partitionId, String logPrefix) throws FileNotFoundException {
        String directoryName = logPrefix + partitionId + "/";
        File directoryFile = new File(directoryName);
        if (!directoryFile.exists()) {
            directoryFile.mkdir();
        }

        String fileName = directoryName + partitionId + ".log";
        File file = new File(fileName);
        this.printWriter = new PrintWriter(file);
    }

    public void recordPartitionLog(String log) {
        this.printWriter.println(log);
        this.printWriter.flush();
    }

    public void close() {
        if (this.printWriter != null) {
            this.printWriter.close();
        }
    }
}
