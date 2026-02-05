package com.bigbrightpaints.erp.modules.factory.domain;

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
@Table(name = "packing_request_records",
        uniqueConstraints = @UniqueConstraint(name = "uq_packing_request_records_company_key",
                columnNames = {"company_id", "idempotency_key"}))
public class PackingRequestRecord extends VersionedEntity {

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

    @Column(name = "production_log_id", nullable = false)
    private Long productionLogId;

    @Column(name = "packing_record_id")
    private Long packingRecordId;

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

    public Long getProductionLogId() {
        return productionLogId;
    }

    public void setProductionLogId(Long productionLogId) {
        this.productionLogId = productionLogId;
    }

    public Long getPackingRecordId() {
        return packingRecordId;
    }

    public void setPackingRecordId(Long packingRecordId) {
        this.packingRecordId = packingRecordId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
