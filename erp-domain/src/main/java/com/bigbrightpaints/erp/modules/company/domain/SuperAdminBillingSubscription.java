package com.bigbrightpaints.erp.modules.company.domain;

import java.time.Instant;

import com.bigbrightpaints.erp.core.util.CompanyTime;

import jakarta.persistence.*;

@Entity
@Table(
    name = "super_admin_billing_subscriptions",
    indexes = {
      @Index(name = "idx_super_admin_billing_subscriptions_company", columnList = "company_id"),
      @Index(
          name = "idx_super_admin_billing_subscriptions_status_currency",
          columnList = "status,currency")
    })
public class SuperAdminBillingSubscription {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "company_id", nullable = false)
  private Company company;

  @Column(name = "plan_id", nullable = false, length = 64)
  private String planId;

  @Column(name = "custom_plan_name", length = 160)
  private String customPlanName;

  @Column(name = "status", nullable = false, length = 32)
  private String status;

  @Column(name = "cadence", nullable = false, length = 32)
  private String cadence;

  @Column(name = "amount_minor_units", nullable = false)
  private Long amountMinorUnits;

  @Column(name = "currency", nullable = false, length = 3)
  private String currency;

  @Column(name = "collection_mode", nullable = false, length = 32)
  private String collectionMode;

  @Column(name = "period_start_at", nullable = false)
  private Instant periodStartAt;

  @Column(name = "period_end_at")
  private Instant periodEndAt;

  @Column(name = "renewal_at")
  private Instant renewalAt;

  @Column(name = "due_at")
  private Instant dueAt;

  @Column(name = "trial_start_at")
  private Instant trialStartAt;

  @Column(name = "trial_end_at")
  private Instant trialEndAt;

  @Column(name = "grace_until_at")
  private Instant graceUntilAt;

  @Column(name = "canceled_at")
  private Instant canceledAt;

  @Column(name = "archived_at")
  private Instant archivedAt;

  @Column(name = "external_reference", length = 160)
  private String externalReference;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Column(name = "audit_event_id")
  private Long auditEventId;

  @PrePersist
  void prePersist() {
    Instant now = CompanyTime.now(company);
    if (createdAt == null) {
      createdAt = now;
    }
    if (updatedAt == null) {
      updatedAt = now;
    }
  }

  @PreUpdate
  void preUpdate() {
    updatedAt = CompanyTime.now(company);
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

  public String getPlanId() {
    return planId;
  }

  public void setPlanId(String planId) {
    this.planId = planId;
  }

  public String getCustomPlanName() {
    return customPlanName;
  }

  public void setCustomPlanName(String customPlanName) {
    this.customPlanName = customPlanName;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getCadence() {
    return cadence;
  }

  public void setCadence(String cadence) {
    this.cadence = cadence;
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

  public String getCollectionMode() {
    return collectionMode;
  }

  public void setCollectionMode(String collectionMode) {
    this.collectionMode = collectionMode;
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

  public Instant getRenewalAt() {
    return renewalAt;
  }

  public void setRenewalAt(Instant renewalAt) {
    this.renewalAt = renewalAt;
  }

  public Instant getDueAt() {
    return dueAt;
  }

  public void setDueAt(Instant dueAt) {
    this.dueAt = dueAt;
  }

  public Instant getTrialStartAt() {
    return trialStartAt;
  }

  public void setTrialStartAt(Instant trialStartAt) {
    this.trialStartAt = trialStartAt;
  }

  public Instant getTrialEndAt() {
    return trialEndAt;
  }

  public void setTrialEndAt(Instant trialEndAt) {
    this.trialEndAt = trialEndAt;
  }

  public Instant getGraceUntilAt() {
    return graceUntilAt;
  }

  public void setGraceUntilAt(Instant graceUntilAt) {
    this.graceUntilAt = graceUntilAt;
  }

  public Instant getCanceledAt() {
    return canceledAt;
  }

  public void setCanceledAt(Instant canceledAt) {
    this.canceledAt = canceledAt;
  }

  public Instant getArchivedAt() {
    return archivedAt;
  }

  public void setArchivedAt(Instant archivedAt) {
    this.archivedAt = archivedAt;
  }

  public String getExternalReference() {
    return externalReference;
  }

  public void setExternalReference(String externalReference) {
    this.externalReference = externalReference;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public Long getAuditEventId() {
    return auditEventId;
  }

  public void setAuditEventId(Long auditEventId) {
    this.auditEventId = auditEventId;
  }
}
