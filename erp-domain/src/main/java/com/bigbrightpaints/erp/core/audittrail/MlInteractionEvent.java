package com.bigbrightpaints.erp.core.audittrail;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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

@Entity
@Table(
    name = "ml_interaction_events",
    indexes = {
      @Index(
          name = "idx_ml_interaction_events_company_occurred",
          columnList = "company_id, occurred_at"),
      @Index(
          name = "idx_ml_interaction_events_company_actor_user",
          columnList = "company_id, actor_user_id, occurred_at"),
      @Index(
          name = "idx_ml_interaction_events_company_actor_identifier",
          columnList = "company_id, actor_identifier, occurred_at"),
      @Index(
          name = "idx_ml_interaction_events_company_module_action",
          columnList = "company_id, module, action, occurred_at"),
      @Index(
          name = "idx_ml_interaction_events_company_trace",
          columnList = "company_id, trace_id, occurred_at")
    })
public class MlInteractionEvent extends VersionedEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "company_id", nullable = false)
  private Long companyId;

  @Column(name = "occurred_at", nullable = false)
  private Instant occurredAt;

  @Column(name = "module", nullable = false, length = 64)
  private String module;

  @Column(name = "action", nullable = false, length = 128)
  private String action;

  @Column(name = "interaction_type", length = 32)
  private String interactionType;

  @Column(name = "screen", length = 128)
  private String screen;

  @Column(name = "target_id", length = 256)
  private String targetId;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 16)
  private AuditActionEventStatus status;

  @Column(name = "failure_reason", length = 512)
  private String failureReason;

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

  @Column(name = "consent_opt_in", nullable = false)
  private boolean consentOptIn;

  @Column(name = "training_subject_key", length = 128)
  private String trainingSubjectKey;

  @Column(name = "payload", columnDefinition = "TEXT")
  private String payload;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @ElementCollection
  @CollectionTable(
      name = "ml_interaction_event_metadata",
      joinColumns = @JoinColumn(name = "event_id"))
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

  public String getInteractionType() {
    return interactionType;
  }

  public void setInteractionType(String interactionType) {
    this.interactionType = interactionType;
  }

  public String getScreen() {
    return screen;
  }

  public void setScreen(String screen) {
    this.screen = screen;
  }

  public String getTargetId() {
    return targetId;
  }

  public void setTargetId(String targetId) {
    this.targetId = targetId;
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

  public boolean isConsentOptIn() {
    return consentOptIn;
  }

  public void setConsentOptIn(boolean consentOptIn) {
    this.consentOptIn = consentOptIn;
  }

  public String getTrainingSubjectKey() {
    return trainingSubjectKey;
  }

  public void setTrainingSubjectKey(String trainingSubjectKey) {
    this.trainingSubjectKey = trainingSubjectKey;
  }

  public String getPayload() {
    return payload;
  }

  public void setPayload(String payload) {
    this.payload = payload;
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
