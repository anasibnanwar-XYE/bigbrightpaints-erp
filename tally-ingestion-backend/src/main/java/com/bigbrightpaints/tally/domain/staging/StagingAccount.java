package com.bigbrightpaints.tally.domain.staging;

import com.bigbrightpaints.tally.domain.BaseEntity;
import com.bigbrightpaints.tally.domain.IngestionRun;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/**
 * Staging table for Tally account/ledger data
 */
@Entity
@Table(name = "stg_tally_accounts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class StagingAccount extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "run_id", nullable = false)
    private IngestionRun run;

    @Column(name = "row_number", nullable = false)
    private Integer rowNumber;

    // Tally fields
    @Column(name = "ledger_name", length = 500)
    private String ledgerName;

    @Column(name = "ledger_group")
    private String ledgerGroup;

    @Column(name = "opening_balance", precision = 18, scale = 2)
    private BigDecimal openingBalance;

    @Column(name = "dr_cr", length = 10)
    private String drCr; // 'Dr' or 'Cr'

    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    @Column(name = "city")
    private String city;

    @Column(name = "state")
    private String state;

    @Column(name = "pincode", length = 20)
    private String pincode;

    @Column(name = "pan", length = 20)
    private String pan;

    @Column(name = "gstin", length = 20)
    private String gstin;

    @Column(name = "bank_name")
    private String bankName;

    @Column(name = "account_number", length = 100)
    private String accountNumber;

    @Column(name = "ifsc_code", length = 20)
    private String ifscCode;

    // Processing fields
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_data", columnDefinition = "jsonb")
    private Map<String, Object> rawData;

    @Column(name = "source_hash", length = 64)
    private String sourceHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "validation_status", length = 32)
    private ValidationStatus validationStatus = ValidationStatus.PENDING;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "validation_errors", columnDefinition = "jsonb")
    private Map<String, String> validationErrors;

    @Column(name = "processed")
    private Boolean processed = false;

    @Column(name = "processed_at")
    private Instant processedAt;

    // Mapped IDs (after processing)
    @Column(name = "mapped_account_id")
    private Long mappedAccountId;

    @Column(name = "mapped_dealer_id")
    private Long mappedDealerId;

    @Column(name = "mapped_supplier_id")
    private Long mappedSupplierId;

    public enum ValidationStatus {
        PENDING,
        VALID,
        INVALID
    }
}