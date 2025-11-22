package com.bigbrightpaints.tally.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Represents a single ingestion run for importing Tally data
 */
@Entity
@Table(name = "tally_ingestion_runs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class IngestionRun extends BaseEntity {

    @Column(name = "run_id", nullable = false, unique = true)
    private UUID runId;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Enumerated(EnumType.STRING)
    @Column(name = "run_type", nullable = false, length = 32)
    private RunType runType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private RunStatus status;

    @Column(name = "dry_run", nullable = false)
    private Boolean dryRun = false;

    // Statistics
    @Column(name = "total_files")
    private Integer totalFiles = 0;

    @Column(name = "files_processed")
    private Integer filesProcessed = 0;

    @Column(name = "total_rows")
    private Integer totalRows = 0;

    @Column(name = "rows_processed")
    private Integer rowsProcessed = 0;

    @Column(name = "rows_succeeded")
    private Integer rowsSucceeded = 0;

    @Column(name = "rows_failed")
    private Integer rowsFailed = 0;

    @Column(name = "rows_skipped")
    private Integer rowsSkipped = 0;

    // Timing
    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    // Metadata
    @Column(name = "initiated_by")
    private Long initiatedBy;

    @Column(name = "source_system", length = 32)
    private String sourceSystem = "TALLY";

    @Column(name = "source_version", length = 32)
    private String sourceVersion;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "configuration", columnDefinition = "jsonb")
    private Map<String, Object> configuration;

    @Column(name = "error_summary", columnDefinition = "TEXT")
    private String errorSummary;

    @PrePersist
    protected void onCreate() {
        if (runId == null) {
            runId = UUID.randomUUID();
        }
        if (status == null) {
            status = RunStatus.PENDING;
        }
    }

    public enum RunType {
        FULL_IMPORT,
        INCREMENTAL,
        RECONCILIATION
    }

    public enum RunStatus {
        PENDING,
        RUNNING,
        COMPLETED,
        FAILED,
        PARTIAL
    }

    // Business methods
    public void startRun() {
        this.status = RunStatus.RUNNING;
        this.startedAt = Instant.now();
    }

    public void completeRun() {
        this.status = RunStatus.COMPLETED;
        this.completedAt = Instant.now();
    }

    public void failRun(String error) {
        this.status = RunStatus.FAILED;
        this.errorSummary = error;
        this.completedAt = Instant.now();
    }

    public void updateProgress(int filesProcessed, int rowsProcessed) {
        this.filesProcessed = filesProcessed;
        this.rowsProcessed = rowsProcessed;
    }

    public double getSuccessRate() {
        if (totalRows == null || totalRows == 0) {
            return 0.0;
        }
        return (rowsSucceeded != null ? rowsSucceeded : 0) * 100.0 / totalRows;
    }
}