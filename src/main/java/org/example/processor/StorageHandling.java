package org.example.processor;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Handles the storage of the PageProcessor results.
 */
public abstract class StorageHandling {

    /**
     * Writes a list of map entries in csv format to file
     * @param mapEntries list of map entries String, Integer
     * @param urlString the url string to name the file
     */
    public static void saveToCsv(List<Map.Entry<String, Integer>> mapEntries, String urlString) {
        try {
            urlString = java.net.URLEncoder.encode(urlString, StandardCharsets.UTF_8);
            String fileName = "output/"+ urlString + ".csv";
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
                // Write CSV header
                writer.write("Word,Frequency");
                writer.newLine();

                // Write each map entry as a CSV row
                for (Map.Entry<String, Integer> entry : mapEntries) {
                    writer.write(entry.getKey() + "," + entry.getValue());
                    writer.newLine();
                }
            }
        } catch (IOException e) {
            System.out.printf("Failed to save to csv, %s\n", e.getMessage());
        }
    }
}
