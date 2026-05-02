package com.bigbrightpaints.erp.modules.company.domain;

import java.time.Instant;

import com.bigbrightpaints.erp.core.util.CompanyTime;

import jakarta.persistence.*;

@Entity
@Table(
    name = "super_admin_billing_ledger_entries",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uq_super_admin_billing_ledger_idempotency",
            columnNames = {"company_id", "idempotency_key"}),
    indexes = {
      @Index(
          name = "idx_super_admin_billing_ledger_company_created",
          columnList = "company_id,created_at,id"),
      @Index(name = "idx_super_admin_billing_ledger_subscription", columnList = "subscription_id")
    })
public class SuperAdminBillingLedgerEntry {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "company_id", nullable = false)
  private Company company;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "subscription_id", nullable = false)
  private SuperAdminBillingSubscription subscription;

  @Column(name = "entry_type", nullable = false, length = 32, updatable = false)
  private String entryType;

  @Column(name = "direction", nullable = false, length = 16, updatable = false)
  private String direction;

  @Column(name = "amount_minor_units", nullable = false, updatable = false)
  private Long amountMinorUnits;

  @Column(name = "currency", nullable = false, length = 3, updatable = false)
  private String currency;

  @Column(name = "reason", nullable = false, length = 300, updatable = false)
  private String reason;

  @Column(name = "external_reference", length = 160, updatable = false)
  private String externalReference;

  @Column(name = "idempotency_key", nullable = false, length = 160, updatable = false)
  private String idempotencyKey;

  @Column(name = "balance_before_minor_units", nullable = false, updatable = false)
  private Long balanceBeforeMinorUnits;

  @Column(name = "balance_after_minor_units", nullable = false, updatable = false)
  private Long balanceAfterMinorUnits;

  @Column(name = "billing_status_after", nullable = false, length = 32, updatable = false)
  private String billingStatusAfter;

  @Column(name = "created_by", length = 160, updatable = false)
  private String createdBy;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "audit_event_id")
  private Long auditEventId;

  @PrePersist
  void prePersist() {
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

  public SuperAdminBillingSubscription getSubscription() {
    return subscription;
  }

  public void setSubscription(SuperAdminBillingSubscription subscription) {
    this.subscription = subscription;
  }

  public String getEntryType() {
    return entryType;
  }

  public void setEntryType(String entryType) {
    this.entryType = entryType;
  }

  public String getDirection() {
    return direction;
  }

  public void setDirection(String direction) {
    this.direction = direction;
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

  public String getReason() {
    return reason;
  }

  public void setReason(String reason) {
    this.reason = reason;
  }

  public String getExternalReference() {
    return externalReference;
  }

  public void setExternalReference(String externalReference) {
    this.externalReference = externalReference;
  }

  public String getIdempotencyKey() {
    return idempotencyKey;
  }

  public void setIdempotencyKey(String idempotencyKey) {
    this.idempotencyKey = idempotencyKey;
  }

  public Long getBalanceBeforeMinorUnits() {
    return balanceBeforeMinorUnits;
  }

  public void setBalanceBeforeMinorUnits(Long balanceBeforeMinorUnits) {
    this.balanceBeforeMinorUnits = balanceBeforeMinorUnits;
  }

  public Long getBalanceAfterMinorUnits() {
    return balanceAfterMinorUnits;
  }

  public void setBalanceAfterMinorUnits(Long balanceAfterMinorUnits) {
    this.balanceAfterMinorUnits = balanceAfterMinorUnits;
  }

  public String getBillingStatusAfter() {
    return billingStatusAfter;
  }

  public void setBillingStatusAfter(String billingStatusAfter) {
    this.billingStatusAfter = billingStatusAfter;
  }

  public String getCreatedBy() {
    return createdBy;
  }

  public void setCreatedBy(String createdBy) {
    this.createdBy = createdBy;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Long getAuditEventId() {
    return auditEventId;
  }

  public void setAuditEventId(Long auditEventId) {
    this.auditEventId = auditEventId;
  }
}
