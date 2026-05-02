package com.bigbrightpaints.erp.modules.admin.domain;

import java.time.Instant;

import com.bigbrightpaints.erp.core.util.CompanyTime;

import jakarta.persistence.*;

@Entity
@Table(
    name = "super_admin_infra_cost_snapshot_corrections",
    indexes = {
      @Index(
          name = "idx_super_admin_infra_cost_corrections_snapshot",
          columnList = "snapshot_id,corrected_at")
    })
public class SuperAdminInfraCostSnapshotCorrection {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "snapshot_id", nullable = false)
  private SuperAdminInfraCostSnapshot snapshot;

  @Column(name = "previous_amount_minor_units", nullable = false)
  private Long previousAmountMinorUnits;

  @Column(name = "new_amount_minor_units", nullable = false)
  private Long newAmountMinorUnits;

  @Column(name = "previous_currency", nullable = false, length = 3)
  private String previousCurrency;

  @Column(name = "new_currency", nullable = false, length = 3)
  private String newCurrency;

  @Column(nullable = false, length = 300)
  private String reason;

  @Column(name = "corrected_by", length = 160)
  private String correctedBy;

  @Column(name = "corrected_at", nullable = false)
  private Instant correctedAt;

  @Column(name = "audit_event_id")
  private Long auditEventId;

  @PrePersist
  void prePersist() {
    if (correctedAt == null) {
      correctedAt = CompanyTime.now();
    }
  }

  public Long getId() {
    return id;
  }

  public SuperAdminInfraCostSnapshot getSnapshot() {
    return snapshot;
  }

  public void setSnapshot(SuperAdminInfraCostSnapshot snapshot) {
    this.snapshot = snapshot;
  }

  public Long getPreviousAmountMinorUnits() {
    return previousAmountMinorUnits;
  }

  public void setPreviousAmountMinorUnits(Long previousAmountMinorUnits) {
    this.previousAmountMinorUnits = previousAmountMinorUnits;
  }

  public Long getNewAmountMinorUnits() {
    return newAmountMinorUnits;
  }

  public void setNewAmountMinorUnits(Long newAmountMinorUnits) {
    this.newAmountMinorUnits = newAmountMinorUnits;
  }

  public String getPreviousCurrency() {
    return previousCurrency;
  }

  public void setPreviousCurrency(String previousCurrency) {
    this.previousCurrency = previousCurrency;
  }

  public String getNewCurrency() {
    return newCurrency;
  }

  public void setNewCurrency(String newCurrency) {
    this.newCurrency = newCurrency;
  }

  public String getReason() {
    return reason;
  }

  public void setReason(String reason) {
    this.reason = reason;
  }

  public String getCorrectedBy() {
    return correctedBy;
  }

  public void setCorrectedBy(String correctedBy) {
    this.correctedBy = correctedBy;
  }

  public Instant getCorrectedAt() {
    return correctedAt;
  }

  public Long getAuditEventId() {
    return auditEventId;
  }

  public void setAuditEventId(Long auditEventId) {
    this.auditEventId = auditEventId;
  }
}
