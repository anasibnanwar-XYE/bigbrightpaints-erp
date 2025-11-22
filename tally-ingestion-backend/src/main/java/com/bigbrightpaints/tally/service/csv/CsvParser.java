package com.bigbrightpaints.tally.service.csv;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Generic CSV parser for handling Tally export files
 */
@Component
@Slf4j
public class CsvParser {

    private static final int BATCH_SIZE = 1000;

    /**
     * Parse CSV file and convert to list of maps
     */
    public List<Map<String, String>> parseCsvToMaps(InputStream inputStream) throws IOException {
        List<Map<String, String>> result = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8));
             CSVReader csvReader = new CSVReaderBuilder(reader)
                     .withSkipLines(0)
                     .build()) {

            List<String[]> allData = csvReader.readAll();
            if (allData.isEmpty()) {
                return result;
            }

            String[] headers = allData.get(0);
            headers = normalizeHeaders(headers);

            for (int i = 1; i < allData.size(); i++) {
                String[] row = allData.get(i);
                Map<String, String> rowMap = new HashMap<>();

                for (int j = 0; j < Math.min(headers.length, row.length); j++) {
                    String value = row[j];
                    // Clean and normalize the value
                    value = value != null ? value.trim() : "";
                    if (!value.isEmpty()) {
                        rowMap.put(headers[j], value);
                    }
                }

                if (!rowMap.isEmpty()) {
                    result.add(rowMap);
                }
            }

        } catch (CsvException e) {
            log.error("Error parsing CSV file", e);
            throw new IOException("Failed to parse CSV file", e);
        }

        return result;
    }

    /**
     * Parse CSV file in batches for large files
     */
    public void parseCsvInBatches(
            InputStream inputStream,
            Consumer<List<Map<String, String>>> batchProcessor) throws IOException {

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8));
             CSVReader csvReader = new CSVReaderBuilder(reader)
                     .withSkipLines(0)
                     .build()) {

            String[] headers = csvReader.readNext();
            if (headers == null) {
                return;
            }
            headers = normalizeHeaders(headers);

            List<Map<String, String>> batch = new ArrayList<>();
            String[] row;

            while ((row = csvReader.readNext()) != null) {
                Map<String, String> rowMap = createRowMap(headers, row);
                if (!rowMap.isEmpty()) {
                    batch.add(rowMap);
                }

                if (batch.size() >= BATCH_SIZE) {
                    batchProcessor.accept(new ArrayList<>(batch));
                    batch.clear();
                }
            }

            // Process remaining records
            if (!batch.isEmpty()) {
                batchProcessor.accept(batch);
            }

        } catch (CsvException e) {
            log.error("Error parsing CSV file in batches", e);
            throw new IOException("Failed to parse CSV file", e);
        }
    }

    /**
     * Parse CSV using Apache Commons CSV for more flexibility
     */
    public List<Map<String, String>> parseWithApacheCommons(
            InputStream inputStream, CSVFormat format) throws IOException {

        List<Map<String, String>> result = new ArrayList<>();

        try (Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
            Iterable<CSVRecord> records = format
                    .withFirstRecordAsHeader()
                    .withIgnoreEmptyLines()
                    .withTrim()
                    .parse(reader);

            for (CSVRecord record : records) {
                Map<String, String> rowMap = record.toMap();
                // Remove empty values
                rowMap.entrySet().removeIf(entry ->
                    entry.getValue() == null || entry.getValue().trim().isEmpty());

                if (!rowMap.isEmpty()) {
                    result.add(rowMap);
                }
            }
        }

        return result;
    }

    /**
     * Detect CSV delimiter automatically
     */
    public char detectDelimiter(InputStream inputStream) throws IOException {
        inputStream.mark(8192); // Mark to reset stream later

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

            String firstLine = reader.readLine();
            if (firstLine == null) {
                return ',';
            }

            // Count occurrences of common delimiters
            Map<Character, Integer> delimiterCounts = new HashMap<>();
            char[] possibleDelimiters = {',', ';', '\t', '|'};

            for (char delimiter : possibleDelimiters) {
                int count = (int) firstLine.chars()
                        .filter(ch -> ch == delimiter)
                        .count();
                delimiterCounts.put(delimiter, count);
            }

            // Return the delimiter with highest count
            return delimiterCounts.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse(',');

        } finally {
            inputStream.reset();
        }
    }

    /**
     * Validate CSV headers against expected columns
     */
    public ValidationResult validateHeaders(
            List<String> actualHeaders,
            List<String> requiredHeaders,
            List<String> optionalHeaders) {

        Set<String> actualSet = actualHeaders.stream()
                .map(this::normalizeHeader)
                .collect(Collectors.toSet());

        Set<String> requiredSet = requiredHeaders.stream()
                .map(this::normalizeHeader)
                .collect(Collectors.toSet());

        Set<String> missingRequired = new HashSet<>(requiredSet);
        missingRequired.removeAll(actualSet);

        if (!missingRequired.isEmpty()) {
            return ValidationResult.invalid(
                "Missing required columns: " + String.join(", ", missingRequired));
        }

        return ValidationResult.valid();
    }

    /**
     * Normalize headers for consistent mapping
     */
    private String[] normalizeHeaders(String[] headers) {
        return Arrays.stream(headers)
                .map(this::normalizeHeader)
                .toArray(String[]::new);
    }

    private String normalizeHeader(String header) {
        if (header == null) {
            return "";
        }
        // Remove BOM if present
        if (header.startsWith("\uFEFF")) {
            header = header.substring(1);
        }
        return header.trim()
                .toLowerCase()
                .replaceAll("[\\s-]+", "_")
                .replaceAll("[^a-z0-9_]", "");
    }

    private Map<String, String> createRowMap(String[] headers, String[] row) {
        Map<String, String> rowMap = new HashMap<>();
        for (int i = 0; i < Math.min(headers.length, row.length); i++) {
            String value = row[i];
            value = value != null ? value.trim() : "";
            if (!value.isEmpty()) {
                rowMap.put(headers[i], value);
            }
        }
        return rowMap;
    }

    /**
     * Result class for validation
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String message;

        private ValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }

        public static ValidationResult valid() {
            return new ValidationResult(true, null);
        }

        public static ValidationResult invalid(String message) {
            return new ValidationResult(false, message);
        }

        public boolean isValid() {
            return valid;
        }

        public String getMessage() {
            return message;
        }
    }
}