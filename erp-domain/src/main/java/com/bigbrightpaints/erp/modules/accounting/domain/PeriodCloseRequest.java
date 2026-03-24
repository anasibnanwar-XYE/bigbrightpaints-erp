package com.bigbrightpaints.erp.modules.accounting.domain;

import java.time.Instant;
import java.util.UUID;

import com.bigbrightpaints.erp.core.util.CompanyTime;
import com.bigbrightpaints.erp.modules.company.domain.Company;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "period_close_requests")
public class PeriodCloseRequest {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "public_id", nullable = false)
  private UUID publicId;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "company_id")
  private Company company;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "accounting_period_id")
  private AccountingPeriod accountingPeriod;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private PeriodCloseRequestStatus status = PeriodCloseRequestStatus.PENDING;

  @Column(name = "requested_by", nullable = false)
  private String requestedBy;

  @Column(name = "request_note", columnDefinition = "TEXT")
  private String requestNote;

  @Column(name = "force_requested", nullable = false)
  private boolean forceRequested;

  @Column(name = "requested_at", nullable = false)
  private Instant requestedAt;

  @Column(name = "reviewed_by")
  private String reviewedBy;

  @Column(name = "reviewed_at")
  private Instant reviewedAt;

  @Column(name = "review_note", columnDefinition = "TEXT")
  private String reviewNote;

  @Column(name = "approval_note", columnDefinition = "TEXT")
  private String approvalNote;

  @PrePersist
  public void prePersist() {
    if (publicId == null) {
      publicId = UUID.randomUUID();
    }
    if (status == null) {
      status = PeriodCloseRequestStatus.PENDING;
    }
    if (requestedAt == null) {
      requestedAt = CompanyTime.now(company);
    }
  }

  public Long getId() {
    return id;
  }

  public UUID getPublicId() {
    return publicId;
  }

  public Company getCompany() {
    return company;
  }

  public void setCompany(Company company) {
    this.company = company;
  }

  public AccountingPeriod getAccountingPeriod() {
    return accountingPeriod;
  }

  public void setAccountingPeriod(AccountingPeriod accountingPeriod) {
    this.accountingPeriod = accountingPeriod;
  }

  public PeriodCloseRequestStatus getStatus() {
    return status;
  }

  public void setStatus(PeriodCloseRequestStatus status) {
    this.status = status;
  }

  public String getRequestedBy() {
    return requestedBy;
  }

  public void setRequestedBy(String requestedBy) {
    this.requestedBy = requestedBy;
  }

  public String getRequestNote() {
    return requestNote;
  }

  public void setRequestNote(String requestNote) {
    this.requestNote = requestNote;
  }

  public boolean isForceRequested() {
    return forceRequested;
  }

  public void setForceRequested(boolean forceRequested) {
    this.forceRequested = forceRequested;
  }

  public Instant getRequestedAt() {
    return requestedAt;
  }

  public void setRequestedAt(Instant requestedAt) {
    this.requestedAt = requestedAt;
  }

  public String getReviewedBy() {
    return reviewedBy;
  }

  public void setReviewedBy(String reviewedBy) {
    this.reviewedBy = reviewedBy;
  }

  public Instant getReviewedAt() {
    return reviewedAt;
  }

  public void setReviewedAt(Instant reviewedAt) {
    this.reviewedAt = reviewedAt;
  }

  public String getReviewNote() {
    return reviewNote;
  }

  public void setReviewNote(String reviewNote) {
    this.reviewNote = reviewNote;
  }

  public String getApprovalNote() {
    return approvalNote;
  }

  public void setApprovalNote(String approvalNote) {
    this.approvalNote = approvalNote;
  }
}
