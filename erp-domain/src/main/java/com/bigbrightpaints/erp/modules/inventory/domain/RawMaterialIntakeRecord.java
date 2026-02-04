package com.bigbrightpaints.erp.modules.inventory.domain;

import com.bigbrightpaints.erp.core.domain.VersionedEntity;
import com.bigbrightpaints.erp.core.util.CompanyTime;
import com.bigbrightpaints.erp.modules.company.domain.Company;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

@Entity
@Table(name = "raw_material_intake_requests",
        uniqueConstraints = @UniqueConstraint(name = "uq_raw_material_intake_company_key",
                columnNames = {"company_id", "idempotency_key"}))
public class RawMaterialIntakeRecord extends VersionedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;

    @Column(name = "idempotency_key", nullable = false, length = 128)
    private String idempotencyKey;

    @Column(name = "idempotency_hash", length = 64)
    private String idempotencyHash;

    @Column(name = "raw_material_id")
    private Long rawMaterialId;

    @Column(name = "raw_material_batch_id")
    private Long rawMaterialBatchId;

    @Column(name = "raw_material_movement_id")
    private Long rawMaterialMovementId;

    @Column(name = "journal_entry_id")
    private Long journalEntryId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = CompanyTime.now(company);
        }
    }

    public Long getId() {
        return id;
    }

    public Company getCompany() {
        return company;
    }

    public void setCompany(Company company) {
        this.company = company;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public String getIdempotencyHash() {
        return idempotencyHash;
    }

    public void setIdempotencyHash(String idempotencyHash) {
        this.idempotencyHash = idempotencyHash;
    }

    public Long getRawMaterialId() {
        return rawMaterialId;
    }

    public void setRawMaterialId(Long rawMaterialId) {
        this.rawMaterialId = rawMaterialId;
    }

    public Long getRawMaterialBatchId() {
        return rawMaterialBatchId;
    }

    public void setRawMaterialBatchId(Long rawMaterialBatchId) {
        this.rawMaterialBatchId = rawMaterialBatchId;
    }

    public Long getRawMaterialMovementId() {
        return rawMaterialMovementId;
    }

    public void setRawMaterialMovementId(Long rawMaterialMovementId) {
        this.rawMaterialMovementId = rawMaterialMovementId;
    }

    public Long getJournalEntryId() {
        return journalEntryId;
    }

    public void setJournalEntryId(Long journalEntryId) {
        this.journalEntryId = journalEntryId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
