package com.azure;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;

public class DependencyTests {

   private static String ShadedPackagePrefix = "azure_cosmos_spark.";
   private static String ShadedJarName = "src/main/java/dependencyTests/azure-cosmos-spark_3-3_2-12-4.17.0-beta.1.jar";
    public static void main(String[] args) throws IOException {
        File inputFile = new File("src/main/java/dependencyTests/dummy03.txt");
        File outputFile = new File("src/main/java/dependencyTests/dummy03Output.txt");

        try(BufferedReader br = new BufferedReader(new FileReader(inputFile));
            PrintWriter printerWriter = new PrintWriter(outputFile)) {
            String line;
            while((line = br.readLine()) != null) {
                if (!line.startsWith(" "))
                {
                    continue;
                }

                String trimmedLine = line.trim();
                String[] columns = trimmedLine.split("->");
                String from = columns[0].trim();
                String to = columns[1].trim();

                if (to.startsWith(ShadedPackagePrefix) && to.endsWith(ShadedJarName))
                {
                    continue;
                }

                if (to.endsWith("java.base") ||
                        to.endsWith("java.xml") ||
                        to.endsWith("java.management") ||
                        to.endsWith("java.logging") ||
                        to.endsWith(" JDK internal API (jdk.unsupported)") ||
                        to.endsWith("java.compiler") ||
                        to.endsWith("java.sql") ||
                        to.endsWith("java.naming") ||
                        to.endsWith("java.security.jgss") ||
                        to.endsWith("java.desktop") ||
                        to.endsWith("sun.misc.Unsafe") ||
                        to.endsWith("sun.misc.Unsafe"))
                {
                    continue;
                }

                if (to.endsWith("org.apache.spark") ||
                        to.endsWith("scala.") ||
                        to.endsWith("com.azure.cosmos.spark") ||
                        to.endsWith("org.slf4j"))
                {
                    continue;
                }

                if (!to.endsWith(ShadedJarName))
                {
                    continue;
                }

                System.out.println("oh no");
                printerWriter.println(line);
            }
        }
    }
}
