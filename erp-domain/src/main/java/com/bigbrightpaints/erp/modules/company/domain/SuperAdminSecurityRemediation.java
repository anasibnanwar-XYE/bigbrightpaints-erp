package com.bigbrightpaints.erp.modules.company.domain;

import java.time.Instant;

import com.bigbrightpaints.erp.core.util.CompanyTime;

import jakarta.persistence.*;

@Entity
@Table(
    name = "super_admin_security_remediations",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_super_admin_security_remediations_audit_event",
          columnNames = "audit_event_id")
    },
    indexes = {
      @Index(name = "idx_super_admin_security_remediations_status", columnList = "status"),
      @Index(
          name = "idx_super_admin_security_remediations_updated",
          columnList = "updated_at DESC, id DESC")
    })
public class SuperAdminSecurityRemediation {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "audit_event_id", nullable = false)
  private Long auditEventId;

  @Column(nullable = false, length = 32)
  private String status;

  @Column(nullable = false, length = 32)
  private String severity;

  @Column(length = 300)
  private String reason;

  @Column(name = "updated_by", length = 160)
  private String updatedBy;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Column(name = "last_audit_event_id")
  private Long lastAuditEventId;

  @PrePersist
  void prePersist() {
    Instant now = CompanyTime.now();
    if (createdAt == null) {
      createdAt = now;
    }
    if (updatedAt == null) {
      updatedAt = now;
    }
    if (status == null) {
      status = "OPEN";
    }
    if (severity == null) {
      severity = "MEDIUM";
    }
  }

  @PreUpdate
  void preUpdate() {
    updatedAt = CompanyTime.now();
  }

  public Long getId() {
    return id;
  }

  public Long getAuditEventId() {
    return auditEventId;
  }

  public void setAuditEventId(Long auditEventId) {
    this.auditEventId = auditEventId;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getSeverity() {
    return severity;
  }

  public void setSeverity(String severity) {
    this.severity = severity;
  }

  public String getReason() {
    return reason;
  }

  public void setReason(String reason) {
    this.reason = reason;
  }

  public String getUpdatedBy() {
    return updatedBy;
  }

  public void setUpdatedBy(String updatedBy) {
    this.updatedBy = updatedBy;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public Long getLastAuditEventId() {
    return lastAuditEventId;
  }

  public void setLastAuditEventId(Long lastAuditEventId) {
    this.lastAuditEventId = lastAuditEventId;
  }
}
