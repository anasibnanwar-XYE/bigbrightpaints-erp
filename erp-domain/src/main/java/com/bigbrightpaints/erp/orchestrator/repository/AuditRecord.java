package com.bigbrightpaints.erp.orchestrator.repository;

import com.bigbrightpaints.erp.modules.company.domain.Company;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import com.bigbrightpaints.erp.core.domain.VersionedEntity;

@Entity
@Table(name = "orchestrator_audit")
public class AuditRecord extends VersionedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;

    @Column(nullable = false)
    private String traceId;

    @Column(nullable = false)
    private String eventType;

    @Column(nullable = false)
    private Instant timestamp;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String details;

    protected AuditRecord() {
    }

    public AuditRecord(Company company, String traceId, String eventType, Instant timestamp, String details) {
        this.company = company;
        this.traceId = traceId;
        this.eventType = eventType;
        this.timestamp = timestamp;
        this.details = details;
    }

    public UUID getId() {
        return id;
    }

    public Company getCompany() {
        return company;
    }

    public String getTraceId() {
        return traceId;
    }

    public String getEventType() {
        return eventType;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public String getDetails() {
        return details;
    }
}
