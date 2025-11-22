package com.bigbrightpaints.tally.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Tracks individual CSV files uploaded for ingestion
 */
@Entity
@Table(name = "tally_ingestion_files")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class IngestionFile extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "run_id", nullable = false)
    private IngestionRun run;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Enumerated(EnumType.STRING)
    @Column(name = "file_type", nullable = false, length = 32)
    private FileType fileType;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Column(name = "file_hash", length = 64)
    private String fileHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private FileStatus status = FileStatus.PENDING;

    @Column(name = "total_rows")
    private Integer totalRows = 0;

    @Column(name = "processed_rows")
    private Integer processedRows = 0;

    @Column(name = "failed_rows")
    private Integer failedRows = 0;

    @Column(name = "s3_bucket")
    private String s3Bucket;

    @Column(name = "s3_key", length = 500)
    private String s3Key;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "processed_at")
    private Instant processedAt;

    public enum FileType {
        ACCOUNTS,
        DEALERS,
        SUPPLIERS,
        PRODUCTS,
        INVENTORY,
        PRICING,
        TRANSACTIONS
    }

    public enum FileStatus {
        PENDING,
        PROCESSING,
        COMPLETED,
        FAILED
    }

    // Business methods
    public void markProcessing() {
        this.status = FileStatus.PROCESSING;
    }

    public void markCompleted() {
        this.status = FileStatus.COMPLETED;
        this.processedAt = Instant.now();
    }

    public void markFailed(String error) {
        this.status = FileStatus.FAILED;
        this.errorMessage = error;
        this.processedAt = Instant.now();
    }
}