package com.bigbrightpaints.erp.modules.company.domain;

import java.time.Instant;
import java.util.UUID;

import com.bigbrightpaints.erp.core.domain.VersionedEntity;
import com.bigbrightpaints.erp.core.util.CompanyTime;

import jakarta.persistence.*;

@Entity
@Table(
    name = "tenant_default_seed_runs",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_tenant_default_seed_runs_company_category",
          columnNames = {"company_id", "category"}),
      @UniqueConstraint(name = "uk_tenant_default_seed_runs_run_id", columnNames = "run_id")
    },
    indexes = {
      @Index(name = "idx_tenant_default_seed_runs_company", columnList = "company_id"),
      @Index(name = "idx_tenant_default_seed_runs_status", columnList = "company_id,status")
    })
public class TenantDefaultSeedRun extends VersionedEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "company_id", nullable = false)
  private Company company;

  @Column(name = "run_id", nullable = false, length = 64)
  private String runId;

  @Column(nullable = false, length = 64)
  private String category;

  @Column(nullable = false, length = 32)
  private String status;

  @Column(nullable = false, length = 32)
  private String operation;

  @Column(nullable = false)
  private boolean required;

  @Column(name = "completed_at", nullable = false)
  private Instant completedAt;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected TenantDefaultSeedRun() {}

  private TenantDefaultSeedRun(
      Company company,
      String category,
      String status,
      String operation,
      boolean required,
      Instant completedAt) {
    this.company = company;
    this.runId = UUID.randomUUID().toString();
    this.category = category;
    this.status = status;
    this.operation = operation;
    this.required = required;
    this.completedAt = completedAt;
  }

  public static TenantDefaultSeedRun create(
      Company company,
      String category,
      String status,
      String operation,
      boolean required,
      Instant completedAt) {
    return new TenantDefaultSeedRun(company, category, status, operation, required, completedAt);
  }

  @PrePersist
  public void prePersist() {
    Instant now = CompanyTime.now(company);
    if (runId == null) {
      runId = UUID.randomUUID().toString();
    }
    if (createdAt == null) {
      createdAt = now;
    }
    if (updatedAt == null) {
      updatedAt = now;
    }
    if (completedAt == null) {
      completedAt = now;
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

  public String getRunId() {
    return runId;
  }

  public String getCategory() {
    return category;
  }

  public String getStatus() {
    return status;
  }

  public String getOperation() {
    return operation;
  }

  public boolean isRequired() {
    return required;
  }

  public Instant getCompletedAt() {
    return completedAt;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void record(String status, String operation, boolean required, Instant completedAt) {
    this.status = status;
    this.operation = operation;
    this.required = required;
    this.completedAt = completedAt;
  }
}
