package com.bigbrightpaints.tally.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration properties for Tally ingestion
 */
@Configuration
@ConfigurationProperties(prefix = "tally.ingestion")
@Data
public class TallyIngestionProperties {

    // Company prefix for ID generation
    private String companyPrefix = "BBP";

    // Batch processing settings
    private BatchSettings batch = new BatchSettings();

    // Validation settings
    private ValidationSettings validation = new ValidationSettings();

    // Storage settings
    private StorageSettings storage = new StorageSettings();

    // Reconciliation settings
    private ReconciliationSettings reconciliation = new ReconciliationSettings();

    // SKU generation patterns
    private Map<String, String> skuPatterns = new HashMap<>();

    // SKU abbreviations
    private Map<String, String> skuAbbreviations = new HashMap<>();

    // ID generation settings
    private IdGenerationSettings idGeneration = new IdGenerationSettings();

    // Audit settings
    private AuditSettings audit = new AuditSettings();

    // Parallel processing settings
    private ParallelSettings parallel = new ParallelSettings();

    @Data
    public static class BatchSettings {
        private int chunkSize = 1000;
        private int pageSize = 5000;
        private int retryLimit = 3;
        private int skipLimit = 100;
    }

    @Data
    public static class ValidationSettings {
        private boolean enableStrict = false;
        private String gstinPattern = "^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$";
        private String panPattern = "^[A-Z]{5}[0-9]{4}[A-Z]{1}$";
    }

    @Data
    public static class StorageSettings {
        private String type = "local"; // local or s3
        private LocalStorage local = new LocalStorage();
        private S3Storage s3 = new S3Storage();

        @Data
        public static class LocalStorage {
            private String path = "/tmp/tally-uploads";
        }

        @Data
        public static class S3Storage {
            private String bucket = "tally-ingestion";
            private String region = "us-east-1";
            private String prefix = "uploads/";
        }
    }

    @Data
    public static class ReconciliationSettings {
        private double fuzzyMatchThreshold = 0.8;
        private String[] exactMatchFields = {"gstin", "pan", "email"};
        private String[] fuzzyMatchFields = {"name", "address"};
    }

    @Data
    public static class IdGenerationSettings {
        private boolean useDeterministic = true;
        private String hashAlgorithm = "SHA-256";
        private int idLength = 12;
    }

    @Data
    public static class AuditSettings {
        private boolean enabled = true;
        private boolean logAllChanges = true;
        private int retainDays = 90;
    }

    @Data
    public static class ParallelSettings {
        private boolean enabled = true;
        private int threadPoolSize = 8;
    }
}