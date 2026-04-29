package com.bigbrightpaints.erp.modules.admin.domain;

import java.time.Instant;

import com.bigbrightpaints.erp.core.util.CompanyTime;

import jakarta.persistence.*;

@Entity
@Table(
    name = "super_admin_infra_cost_snapshots",
    indexes = {
      @Index(name = "idx_super_admin_infra_cost_component", columnList = "component,status"),
      @Index(
          name = "idx_super_admin_infra_cost_period",
          columnList = "currency,period_end_at,status")
    })
public class SuperAdminInfraCostSnapshot {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 32)
  private String component;

  @Column(name = "period_start_at", nullable = false)
  private Instant periodStartAt;

  @Column(name = "period_end_at", nullable = false)
  private Instant periodEndAt;

  @Column(name = "amount_minor_units", nullable = false)
  private Long amountMinorUnits;

  @Column(nullable = false, length = 3)
  private String currency;

  @Column(nullable = false, length = 120)
  private String source;

  @Column(length = 300)
  private String notes;

  @Column(nullable = false, length = 32)
  private String status = "ACTIVE";

  @Column(name = "entered_by", length = 160)
  private String enteredBy;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Column(name = "archived_at")
  private Instant archivedAt;

  @Column(name = "correction_count", nullable = false)
  private Integer correctionCount = 0;

  @Column(name = "audit_event_id")
  private Long auditEventId;

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
      status = "ACTIVE";
    }
    if (correctionCount == null) {
      correctionCount = 0;
    }
  }

  @PreUpdate
  void preUpdate() {
    updatedAt = CompanyTime.now();
    if (correctionCount == null) {
      correctionCount = 0;
    }
  }

  public Long getId() {
    return id;
  }

  public String getComponent() {
    return component;
  }

  public void setComponent(String component) {
    this.component = component;
  }

  public Instant getPeriodStartAt() {
    return periodStartAt;
  }

  public void setPeriodStartAt(Instant periodStartAt) {
    this.periodStartAt = periodStartAt;
  }

  public Instant getPeriodEndAt() {
    return periodEndAt;
  }

  public void setPeriodEndAt(Instant periodEndAt) {
    this.periodEndAt = periodEndAt;
  }

  public Long getAmountMinorUnits() {
    return amountMinorUnits;
  }

  public void setAmountMinorUnits(Long amountMinorUnits) {
    this.amountMinorUnits = amountMinorUnits;
  }

  public String getCurrency() {
    return currency;
  }

  public void setCurrency(String currency) {
    this.currency = currency;
  }

  public String getSource() {
    return source;
  }

  public void setSource(String source) {
    this.source = source;
  }

  public String getNotes() {
    return notes;
  }

  public void setNotes(String notes) {
    this.notes = notes;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getEnteredBy() {
    return enteredBy;
  }

  public void setEnteredBy(String enteredBy) {
    this.enteredBy = enteredBy;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public Instant getArchivedAt() {
    return archivedAt;
  }

  public void setArchivedAt(Instant archivedAt) {
    this.archivedAt = archivedAt;
  }

  public Integer getCorrectionCount() {
    return correctionCount;
  }

  public void setCorrectionCount(Integer correctionCount) {
    this.correctionCount = correctionCount;
  }

  public Long getAuditEventId() {
    return auditEventId;
  }

  public void setAuditEventId(Long auditEventId) {
    this.auditEventId = auditEventId;
  }
}
