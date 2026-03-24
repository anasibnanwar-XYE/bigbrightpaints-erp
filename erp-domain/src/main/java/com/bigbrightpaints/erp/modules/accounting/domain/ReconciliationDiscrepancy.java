package com.bigbrightpaints.erp.modules.accounting.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import com.bigbrightpaints.erp.core.domain.VersionedEntity;
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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "reconciliation_discrepancies")
public class ReconciliationDiscrepancy extends VersionedEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "company_id")
  private Company company;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "accounting_period_id")
  private AccountingPeriod accountingPeriod;

  @Column(name = "period_start", nullable = false)
  private LocalDate periodStart;

  @Column(name = "period_end", nullable = false)
  private LocalDate periodEnd;

  @Enumerated(EnumType.STRING)
  @Column(name = "discrepancy_type", nullable = false, length = 32)
  private ReconciliationDiscrepancyType type;

  @Enumerated(EnumType.STRING)
  @Column(name = "partner_type", length = 32)
  private PartnerType partnerType;

  @Column(name = "partner_id")
  private Long partnerId;

  @Column(name = "partner_code", length = 128)
  private String partnerCode;

  @Column(name = "partner_name", length = 255)
  private String partnerName;

  @Column(name = "expected_amount", nullable = false, precision = 19, scale = 2)
  private BigDecimal expectedAmount;

  @Column(name = "actual_amount", nullable = false, precision = 19, scale = 2)
  private BigDecimal actualAmount;

  @Column(name = "variance", nullable = false, precision = 19, scale = 2)
  private BigDecimal variance;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 32)
  private ReconciliationDiscrepancyStatus status = ReconciliationDiscrepancyStatus.OPEN;

  @Enumerated(EnumType.STRING)
  @Column(name = "resolution", length = 32)
  private ReconciliationDiscrepancyResolution resolution;

  @Column(name = "resolution_note", columnDefinition = "TEXT")
  private String resolutionNote;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "resolution_journal_id")
  private JournalEntry resolutionJournal;

  @Column(name = "resolved_by", length = 255)
  private String resolvedBy;

  @Column(name = "resolved_at")
  private Instant resolvedAt;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @PrePersist
  public void prePersist() {
    Instant now = CompanyTime.now(company);
    if (createdAt == null) {
      createdAt = now;
    }
    if (updatedAt == null) {
      updatedAt = now;
    }
    if (status == null) {
      status = ReconciliationDiscrepancyStatus.OPEN;
    }
  }

  @PreUpdate
  public void preUpdate() {
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

  public AccountingPeriod getAccountingPeriod() {
    return accountingPeriod;
  }

  public void setAccountingPeriod(AccountingPeriod accountingPeriod) {
    this.accountingPeriod = accountingPeriod;
  }

  public LocalDate getPeriodStart() {
    return periodStart;
  }

  public void setPeriodStart(LocalDate periodStart) {
    this.periodStart = periodStart;
  }

  public LocalDate getPeriodEnd() {
    return periodEnd;
  }

  public void setPeriodEnd(LocalDate periodEnd) {
    this.periodEnd = periodEnd;
  }

  public ReconciliationDiscrepancyType getType() {
    return type;
  }

  public void setType(ReconciliationDiscrepancyType type) {
    this.type = type;
  }

  public PartnerType getPartnerType() {
    return partnerType;
  }

  public void setPartnerType(PartnerType partnerType) {
    this.partnerType = partnerType;
  }

  public Long getPartnerId() {
    return partnerId;
  }

  public void setPartnerId(Long partnerId) {
    this.partnerId = partnerId;
  }

  public String getPartnerCode() {
    return partnerCode;
  }

  public void setPartnerCode(String partnerCode) {
    this.partnerCode = partnerCode;
  }

  public String getPartnerName() {
    return partnerName;
  }

  public void setPartnerName(String partnerName) {
    this.partnerName = partnerName;
  }

  public BigDecimal getExpectedAmount() {
    return expectedAmount;
  }

  public void setExpectedAmount(BigDecimal expectedAmount) {
    this.expectedAmount = expectedAmount;
  }

  public BigDecimal getActualAmount() {
    return actualAmount;
  }

  public void setActualAmount(BigDecimal actualAmount) {
    this.actualAmount = actualAmount;
  }

  public BigDecimal getVariance() {
    return variance;
  }

  public void setVariance(BigDecimal variance) {
    this.variance = variance;
  }

  public ReconciliationDiscrepancyStatus getStatus() {
    return status;
  }

  public void setStatus(ReconciliationDiscrepancyStatus status) {
    this.status = status;
  }

  public ReconciliationDiscrepancyResolution getResolution() {
    return resolution;
  }

  public void setResolution(ReconciliationDiscrepancyResolution resolution) {
    this.resolution = resolution;
  }

  public String getResolutionNote() {
    return resolutionNote;
  }

  public void setResolutionNote(String resolutionNote) {
    this.resolutionNote = resolutionNote;
  }

  public JournalEntry getResolutionJournal() {
    return resolutionJournal;
  }

  public void setResolutionJournal(JournalEntry resolutionJournal) {
    this.resolutionJournal = resolutionJournal;
  }

  public String getResolvedBy() {
    return resolvedBy;
  }

  public void setResolvedBy(String resolvedBy) {
    this.resolvedBy = resolvedBy;
  }

  public Instant getResolvedAt() {
    return resolvedAt;
  }

  public void setResolvedAt(Instant resolvedAt) {
    this.resolvedAt = resolvedAt;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
