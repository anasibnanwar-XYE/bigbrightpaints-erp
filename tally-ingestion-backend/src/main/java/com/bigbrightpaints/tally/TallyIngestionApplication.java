package com.bigbrightpaints.tally;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Tally Ingestion Backend Application
 *
 * This service handles:
 * - CSV file uploads from Tally exports
 * - Data normalization and validation
 * - Deterministic ID and SKU generation
 * - Cross-entity linking and reconciliation
 * - Batch import into ERP database
 * - Audit logging and error reporting
 */
@SpringBootApplication
@EnableBatchProcessing
@EnableCaching
@EnableRetry
@EnableAsync
@EnableScheduling
@EnableTransactionManagement
public class TallyIngestionApplication {

    public static void main(String[] args) {
        SpringApplication.run(TallyIngestionApplication.class, args);
    }
}