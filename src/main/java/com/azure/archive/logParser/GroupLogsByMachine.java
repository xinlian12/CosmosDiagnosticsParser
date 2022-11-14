package com.azure.archive.logParser;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public class GroupLogsByMachine {

    public static void main(String[] args) {
        String logFile = "src/main/java/linkedInUpgrade/exportByVM.csv";
        String exportPathPrefix = "src/main/java/linkedInUpgrade/byMachine/";
        String matchingString = "GET took more than: 100 ms. Total duration: (.*). Collection name: (.*). Key: (.*). ActivityId: (.*). Diagnostics (.*).\"";
        Pattern pattern = Pattern.compile(matchingString, Pattern.CASE_INSENSITIVE);

        ConcurrentHashMap<String, List<String>> logsByMachine = new ConcurrentHashMap<>();

        try (CSVReader reader = new CSVReader(new FileReader(logFile))) {
            String[] lineInArray;
            while ((lineInArray = reader.readNext()) != null) {
                String[] finalLineInArray = lineInArray;
                logsByMachine.compute(lineInArray[1], (machineId, logList) -> {
                    if (logList == null) {
                        logList = new ArrayList<>();
                    }
                    logList.add(finalLineInArray[0]);
                    return logList;
                });
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (CsvValidationException e) {
            e.printStackTrace();
        }

        System.out.println("Finished grouping logs by machine: " + logsByMachine.size());
        for (String machineId : logsByMachine.keySet()) {
            int validLines = 0;
            String simplifiedMachineId = machineId.split("\\.")[0].split("-")[1];
            String fullLogPath = exportPathPrefix + simplifiedMachineId + ".log";
            File file = new File(fullLogPath);
            try(PrintWriter printWriter = new PrintWriter(file)) {
                for (String logLine : logsByMachine.get(machineId)) {
                    String modifiedLogLine = logLine.replace("\n", "");
                    printWriter.println(modifiedLogLine);
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

            System.out.println("Valid lines: " + validLines);
        }

    }
}
