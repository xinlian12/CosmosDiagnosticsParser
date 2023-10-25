package com.azure;

import javafx.css.Match;

import javax.swing.text.Document;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SimpleLineReader {
    private static String InsertWriteModel = "(.*)writeModel=InsertOneModel\\{document=Document\\{\\{_id=Document\\{\\{(.*)\\}\\}, value=(.*)";
    private static String deleteWriteModel = "(.*)writeModel=DeleteOneModel\\{filter=Document\\{\\{_id=Document\\{\\{(.*)\\}\\}\\}\\}, options=(.*)";
    private static Pattern insertPattern = Pattern.compile(InsertWriteModel, Pattern.CASE_INSENSITIVE);
    private static Pattern deletePattern = Pattern.compile(deleteWriteModel, Pattern.CASE_INSENSITIVE);

    public static void main(String[] args) throws Exception {
        String logSourceDirectory = "src/main/java/ABI/stdout";
        Map<String, DocumentDetails> deleteCountMap = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(logSourceDirectory))) {
            String line = null;
            while ((line = reader.readLine()) != null) {
                if (line.contains("writeModel")) {
                    Matcher insertMatcher = insertPattern.matcher(line);
                    Matcher deleteMatcher = deletePattern.matcher(line);
                    if (insertMatcher.matches()) {
                        int documentIdEndingIndex = insertMatcher.group(2).indexOf("}}, value=");
                        String documentIdString = insertMatcher.group(2).substring(0, documentIdEndingIndex);
                        deleteCountMap.compute(documentIdString, (key, value) -> {
                            if (value == null) {
                                value = new DocumentDetails(key);
                            }

                            value.recordInsert();
                            return value;
                        });
                    } else if (deleteMatcher.matches()) {
                        deleteCountMap.compute(deleteMatcher.group(2), (key, value) -> {
                            if (value == null) {
                                value = new DocumentDetails(key);
                            }

                            value.recordDelete();
                            return value;
                        });
                    } else {
                        System.out.println(line);
                    }
                }
            }
        }

        for (String documentId : deleteCountMap.keySet()) {
            if (deleteCountMap.get(documentId).deleteCount > deleteCountMap.get(documentId).insertCount) {
                System.out.println(deleteCountMap.get(documentId));
            }
        }
    }

    private static class DocumentDetails {
        private String id;
        private int deleteCount;
        private int insertCount;
        private DocumentDetails(String id) {
            this.id = id;
        }

        private void recordInsert() {
            this.insertCount++;
        }

        private void recordDelete() {
            this.deleteCount++;
        }

        @Override
        public String toString() {
            return "DocumentDetails{" +
                    "id='" + id + '\'' +
                    ", deleteCount=" + deleteCount +
                    ", insertCount=" + insertCount +
                    '}';
        }
    }
}
