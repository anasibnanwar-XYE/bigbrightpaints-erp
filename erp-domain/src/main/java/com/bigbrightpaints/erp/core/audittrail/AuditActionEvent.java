package com.bigbrightpaints.erp.core.audittrail;

import com.bigbrightpaints.erp.core.domain.VersionedEntity;
import com.bigbrightpaints.erp.core.util.CompanyTime;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "audit_action_events", indexes = {
        @Index(name = "idx_audit_action_events_company_occurred", columnList = "company_id, occurred_at"),
        @Index(name = "idx_audit_action_events_company_module_action", columnList = "company_id, module, action, occurred_at"),
        @Index(name = "idx_audit_action_events_company_status", columnList = "company_id, status, occurred_at"),
        @Index(name = "idx_audit_action_events_company_reference", columnList = "company_id, reference_number, occurred_at"),
        @Index(name = "idx_audit_action_events_company_actor_user", columnList = "company_id, actor_user_id, occurred_at"),
        @Index(name = "idx_audit_action_events_company_actor_identifier", columnList = "company_id, actor_identifier, occurred_at"),
        @Index(name = "idx_audit_action_events_company_trace", columnList = "company_id, trace_id, occurred_at")
})
public class AuditActionEvent extends VersionedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 32)
    private AuditActionEventSource source;

    @Column(name = "module", nullable = false, length = 64)
    private String module;

    @Column(name = "action", nullable = false, length = 128)
    private String action;

    @Column(name = "entity_type", length = 128)
    private String entityType;

    @Column(name = "entity_id", length = 128)
    private String entityId;

    @Column(name = "reference_number", length = 128)
    private String referenceNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private AuditActionEventStatus status;

    @Column(name = "failure_reason", length = 512)
    private String failureReason;

    @Column(name = "amount", precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "currency", length = 16)
    private String currency;

    @Column(name = "correlation_id")
    private UUID correlationId;

    @Column(name = "request_id", length = 128)
    private String requestId;

    @Column(name = "trace_id", length = 128)
    private String traceId;

    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(name = "actor_user_id")
    private Long actorUserId;

    @Column(name = "actor_identifier", nullable = false, length = 255)
    private String actorIdentifier;

    @Column(name = "actor_anonymized", nullable = false)
    private boolean actorAnonymized;

    @Column(name = "ml_eligible", nullable = false)
    private boolean mlEligible;

    @Column(name = "training_subject_key", length = 128)
    private String trainingSubjectKey;

    @Column(name = "training_payload", columnDefinition = "TEXT")
    private String trainingPayload;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @ElementCollection
    @CollectionTable(name = "audit_action_event_metadata", joinColumns = @JoinColumn(name = "event_id"))
    @MapKeyColumn(name = "metadata_key")
    @Column(name = "metadata_value")
    private Map<String, String> metadata = new HashMap<>();

    @PrePersist
    protected void onCreate() {
        if (occurredAt == null) {
            occurredAt = CompanyTime.now();
        }
        if (createdAt == null) {
            createdAt = CompanyTime.now();
        }
        if (status == null) {
            status = AuditActionEventStatus.SUCCESS;
        }
        if (source == null) {
            source = AuditActionEventSource.SYSTEM;
        }
    }

    public Long getId() {
        return id;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Long companyId) {
        this.companyId = companyId;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(Instant occurredAt) {
        this.occurredAt = occurredAt;
    }

    public AuditActionEventSource getSource() {
        return source;
    }

    public void setSource(AuditActionEventSource source) {
        this.source = source;
    }

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public String getReferenceNumber() {
        return referenceNumber;
    }

    public void setReferenceNumber(String referenceNumber) {
        this.referenceNumber = referenceNumber;
    }

    public AuditActionEventStatus getStatus() {
        return status;
    }

    public void setStatus(AuditActionEventStatus status) {
        this.status = status;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public UUID getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(UUID correlationId) {
        this.correlationId = correlationId;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public Long getActorUserId() {
        return actorUserId;
    }

    public void setActorUserId(Long actorUserId) {
        this.actorUserId = actorUserId;
    }

    public String getActorIdentifier() {
        return actorIdentifier;
    }

    public void setActorIdentifier(String actorIdentifier) {
        this.actorIdentifier = actorIdentifier;
    }

    public boolean isActorAnonymized() {
        return actorAnonymized;
    }

    public void setActorAnonymized(boolean actorAnonymized) {
        this.actorAnonymized = actorAnonymized;
    }

    public boolean isMlEligible() {
        return mlEligible;
    }

    public void setMlEligible(boolean mlEligible) {
        this.mlEligible = mlEligible;
    }

    public String getTrainingSubjectKey() {
        return trainingSubjectKey;
    }

    public void setTrainingSubjectKey(String trainingSubjectKey) {
        this.trainingSubjectKey = trainingSubjectKey;
    }

    public String getTrainingPayload() {
        return trainingPayload;
    }

    public void setTrainingPayload(String trainingPayload) {
        this.trainingPayload = trainingPayload;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }
}
