package com.bigbrightpaints.erp.modules.sales.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.bigbrightpaints.erp.core.domain.VersionedEntity;
import com.bigbrightpaints.erp.core.util.CompanyTime;
import com.bigbrightpaints.erp.modules.company.domain.Company;
import com.bigbrightpaints.erp.modules.inventory.domain.PackagingSlip;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "credit_limit_override_requests")
public class CreditLimitOverrideRequest extends VersionedEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "public_id", nullable = false)
  private UUID publicId;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "company_id")
  private Company company;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "dealer_id")
  private Dealer dealer;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "packaging_slip_id")
  private PackagingSlip packagingSlip;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "sales_order_id")
  private SalesOrder salesOrder;

  @Column(name = "dispatch_amount", nullable = false)
  private BigDecimal dispatchAmount = BigDecimal.ZERO;

  @Column(name = "current_exposure", nullable = false)
  private BigDecimal currentExposure = BigDecimal.ZERO;

  @Column(name = "credit_limit", nullable = false)
  private BigDecimal creditLimit = BigDecimal.ZERO;

  @Column(name = "required_headroom", nullable = false)
  private BigDecimal requiredHeadroom = BigDecimal.ZERO;

  @Column(nullable = false)
  private String status;

  @Column(columnDefinition = "TEXT")
  private String reason;

  @Column(name = "requested_by")
  private String requestedBy;

  @Column(name = "reviewed_by")
  private String reviewedBy;

  @Column(name = "reviewed_at")
  private Instant reviewedAt;

  @Column(name = "expires_at")
  private Instant expiresAt;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @PrePersist
  public void prePersist() {
    if (publicId == null) {
      publicId = UUID.randomUUID();
    }
    if (createdAt == null) {
      createdAt = CompanyTime.now(company);
    }
    if (status == null) {
      status = "PENDING";
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

  public Dealer getDealer() {
    return dealer;
  }

  public void setDealer(Dealer dealer) {
    this.dealer = dealer;
  }

  public PackagingSlip getPackagingSlip() {
    return packagingSlip;
  }

  public void setPackagingSlip(PackagingSlip packagingSlip) {
    this.packagingSlip = packagingSlip;
  }

  public SalesOrder getSalesOrder() {
    return salesOrder;
  }

  public void setSalesOrder(SalesOrder salesOrder) {
    this.salesOrder = salesOrder;
  }

  public BigDecimal getDispatchAmount() {
    return dispatchAmount;
  }

  public void setDispatchAmount(BigDecimal dispatchAmount) {
    this.dispatchAmount = dispatchAmount;
  }

  public BigDecimal getCurrentExposure() {
    return currentExposure;
  }

  public void setCurrentExposure(BigDecimal currentExposure) {
    this.currentExposure = currentExposure;
  }

  public BigDecimal getCreditLimit() {
    return creditLimit;
  }

  public void setCreditLimit(BigDecimal creditLimit) {
    this.creditLimit = creditLimit;
  }

  public BigDecimal getRequiredHeadroom() {
    return requiredHeadroom;
  }

  public void setRequiredHeadroom(BigDecimal requiredHeadroom) {
    this.requiredHeadroom = requiredHeadroom;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getReason() {
    return reason;
  }

  public void setReason(String reason) {
    this.reason = reason;
  }

  public String getRequestedBy() {
    return requestedBy;
  }

  public void setRequestedBy(String requestedBy) {
    this.requestedBy = requestedBy;
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

  public Instant getExpiresAt() {
    return expiresAt;
  }

  public void setExpiresAt(Instant expiresAt) {
    this.expiresAt = expiresAt;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
