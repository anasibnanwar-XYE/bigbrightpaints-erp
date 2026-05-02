package com.bigbrightpaints.erp.modules.company.domain;

import java.time.Instant;

import com.bigbrightpaints.erp.core.domain.VersionedEntity;
import com.bigbrightpaints.erp.core.util.CompanyTime;

import jakarta.persistence.*;

@Entity
@Table(
    name = "tenant_usage_rollups",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uq_tenant_usage_rollups_window",
          columnNames = {"company_id", "dimension", "period_type", "period_start_at"})
    },
    indexes = {
      @Index(
          name = "idx_tenant_usage_rollups_company_period",
          columnList = "company_id,period_type,period_start_at"),
      @Index(
          name = "idx_tenant_usage_rollups_dimension_period",
          columnList = "dimension,period_type,period_start_at"),
      @Index(
          name = "idx_tenant_usage_rollups_closed",
          columnList = "company_id,closed,period_end_at")
    })
public class TenantUsageRollup extends VersionedEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "company_id", nullable = false)
  private Company company;

  @Column(name = "company_code", nullable = false, length = 64)
  private String companyCode;

  @Column(nullable = false, length = 32)
  private String dimension;

  @Column(name = "period_type", nullable = false, length = 16)
  private String periodType;

  @Column(name = "period_start_at", nullable = false)
  private Instant periodStartAt;

  @Column(name = "period_end_at", nullable = false)
  private Instant periodEndAt;

  @Column(name = "tenant_timezone", nullable = false, length = 64)
  private String tenantTimezone;

  @Column(name = "usage_count", nullable = false)
  private long usageCount;

  @Column(name = "usage_bytes", nullable = false)
  private long usageBytes;

  @Column(nullable = false, length = 32)
  private String source;

  @Column(nullable = false)
  private boolean closed;

  @Column(name = "closed_at")
  private Instant closedAt;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected TenantUsageRollup() {}

  private TenantUsageRollup(
      Company company,
      String dimension,
      String periodType,
      Instant periodStartAt,
      Instant periodEndAt,
      String tenantTimezone,
      long usageCount,
      long usageBytes,
      String source) {
    this.company = company;
    this.companyCode = company == null ? null : company.getCode();
    this.dimension = dimension;
    this.periodType = periodType;
    this.periodStartAt = periodStartAt;
    this.periodEndAt = periodEndAt;
    this.tenantTimezone = tenantTimezone;
    this.usageCount = Math.max(usageCount, 0L);
    this.usageBytes = Math.max(usageBytes, 0L);
    this.source = source;
  }

  public static TenantUsageRollup snapshot(
      Company company,
      String dimension,
      String periodType,
      Instant periodStartAt,
      Instant periodEndAt,
      String tenantTimezone,
      long usageCount,
      long usageBytes) {
    return new TenantUsageRollup(
        company,
        dimension,
        periodType,
        periodStartAt,
        periodEndAt,
        tenantTimezone,
        usageCount,
        usageBytes,
        "SNAPSHOT");
  }

  public static TenantUsageRollup counter(
      Company company,
      String dimension,
      String periodType,
      Instant periodStartAt,
      Instant periodEndAt,
      String tenantTimezone) {
    return new TenantUsageRollup(
        company,
        dimension,
        periodType,
        periodStartAt,
        periodEndAt,
        tenantTimezone,
        0L,
        0L,
        "COUNTER");
  }

  @PrePersist
  public void prePersist() {
    Instant now = CompanyTime.now(company);
    if (createdAt == null) {
      createdAt = now;
    }
    if (updatedAt == null) {
      updatedAt = now;
    }
  }

  @PreUpdate
  public void preUpdate() {
    updatedAt = CompanyTime.now(company);
  }

  public void updateSnapshot(
      long usageCount, long usageBytes, Instant periodEndAt, String timezone) {
    this.usageCount = Math.max(usageCount, 0L);
    this.usageBytes = Math.max(usageBytes, 0L);
    this.periodEndAt = periodEndAt;
    this.tenantTimezone = timezone;
    this.source = "SNAPSHOT";
  }

  public void close(Instant closedAt) {
    this.closed = true;
    this.closedAt = closedAt;
  }

  public Long getId() {
    return id;
  }

  public Company getCompany() {
    return company;
  }

  public String getCompanyCode() {
    return companyCode;
  }

  public String getDimension() {
    return dimension;
  }

  public String getPeriodType() {
    return periodType;
  }

  public Instant getPeriodStartAt() {
    return periodStartAt;
  }

  public Instant getPeriodEndAt() {
    return periodEndAt;
  }

  public String getTenantTimezone() {
    return tenantTimezone;
  }

  public long getUsageCount() {
    return usageCount;
  }

  public long getUsageBytes() {
    return usageBytes;
  }

  public String getSource() {
    return source;
  }

  public boolean isClosed() {
    return closed;
  }

  public Instant getClosedAt() {
    return closedAt;
  }
}
