package com.bigbrightpaints.erp.modules.company.domain;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.bigbrightpaints.erp.core.util.CompanyTime;

import jakarta.persistence.*;

@Entity
@Table(
    name = "super_admin_plan_templates",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uq_super_admin_plan_templates_stable_version",
            columnNames = {"stable_id", "template_version"}),
    indexes = {
      @Index(name = "idx_super_admin_plan_templates_stable_id", columnList = "stable_id"),
      @Index(name = "idx_super_admin_plan_templates_status", columnList = "status")
    })
public class SuperAdminPlanTemplate {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "stable_id", nullable = false, length = 64)
  private String stableId;

  @Column(name = "display_name", nullable = false, length = 120)
  private String displayName;

  @Column(name = "status", nullable = false, length = 32)
  private String status = "ACTIVE";

  @Column(name = "template_version", nullable = false)
  private Integer templateVersion = 1;

  @Column(name = "effective_from", nullable = false)
  private Instant effectiveFrom;

  @Column(name = "effective_until")
  private Instant effectiveUntil;

  @Column(name = "cadence", nullable = false, length = 32)
  private String cadence;

  @Column(name = "price_minor_units", nullable = false)
  private Long priceMinorUnits;

  @Column(name = "currency", nullable = false, length = 3)
  private String currency;

  @Column(name = "trial_duration_days", nullable = false)
  private Integer trialDurationDays;

  @Column(name = "support_tier", nullable = false, length = 32)
  private String supportTier;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "feature_flags", nullable = false, columnDefinition = "jsonb")
  private Map<String, Boolean> featureFlags = new LinkedHashMap<>();

  @Column(name = "max_active_users", nullable = false)
  private Long maxActiveUsers;

  @Column(name = "max_api_requests", nullable = false)
  private Long maxApiRequests;

  @Column(name = "max_storage_bytes", nullable = false)
  private Long maxStorageBytes;

  @Column(name = "max_pdf_exports", nullable = false)
  private Long maxPdfExports;

  @Column(name = "max_emails", nullable = false)
  private Long maxEmails;

  @Column(name = "max_jobs", nullable = false)
  private Long maxJobs;

  @Column(name = "burst_requests_per_minute", nullable = false)
  private Long burstRequestsPerMinute;

  @Column(name = "max_concurrent_requests", nullable = false)
  private Long maxConcurrentRequests;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Column(name = "archived_at")
  private Instant archivedAt;

  @PrePersist
  void prePersist() {
    Instant now = CompanyTime.now();
    if (createdAt == null) {
      createdAt = now;
    }
    if (updatedAt == null) {
      updatedAt = now;
    }
    if (effectiveFrom == null) {
      effectiveFrom = now;
    }
    if (status == null || status.isBlank()) {
      status = "ACTIVE";
    }
    if (featureFlags == null) {
      featureFlags = new LinkedHashMap<>();
    }
  }

  @PreUpdate
  void preUpdate() {
    updatedAt = CompanyTime.now();
    if (featureFlags == null) {
      featureFlags = new LinkedHashMap<>();
    }
  }

  public Long getId() {
    return id;
  }

  public String getStableId() {
    return stableId;
  }

  public void setStableId(String stableId) {
    this.stableId = stableId;
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public Integer getTemplateVersion() {
    return templateVersion;
  }

  public void setTemplateVersion(Integer templateVersion) {
    this.templateVersion = templateVersion;
  }

  public Instant getEffectiveFrom() {
    return effectiveFrom;
  }

  public void setEffectiveFrom(Instant effectiveFrom) {
    this.effectiveFrom = effectiveFrom;
  }

  public Instant getEffectiveUntil() {
    return effectiveUntil;
  }

  public void setEffectiveUntil(Instant effectiveUntil) {
    this.effectiveUntil = effectiveUntil;
  }

  public String getCadence() {
    return cadence;
  }

  public void setCadence(String cadence) {
    this.cadence = cadence;
  }

  public Long getPriceMinorUnits() {
    return priceMinorUnits;
  }

  public void setPriceMinorUnits(Long priceMinorUnits) {
    this.priceMinorUnits = priceMinorUnits;
  }

  public String getCurrency() {
    return currency;
  }

  public void setCurrency(String currency) {
    this.currency = currency;
  }

  public Integer getTrialDurationDays() {
    return trialDurationDays;
  }

  public void setTrialDurationDays(Integer trialDurationDays) {
    this.trialDurationDays = trialDurationDays;
  }

  public String getSupportTier() {
    return supportTier;
  }

  public void setSupportTier(String supportTier) {
    this.supportTier = supportTier;
  }

  public Map<String, Boolean> getFeatureFlags() {
    return featureFlags == null ? Map.of() : new LinkedHashMap<>(featureFlags);
  }

  public void setFeatureFlags(Map<String, Boolean> featureFlags) {
    this.featureFlags =
        featureFlags == null ? new LinkedHashMap<>() : new LinkedHashMap<>(featureFlags);
  }

  public Long getMaxActiveUsers() {
    return maxActiveUsers;
  }

  public void setMaxActiveUsers(Long maxActiveUsers) {
    this.maxActiveUsers = maxActiveUsers;
  }

  public Long getMaxApiRequests() {
    return maxApiRequests;
  }

  public void setMaxApiRequests(Long maxApiRequests) {
    this.maxApiRequests = maxApiRequests;
  }

  public Long getMaxStorageBytes() {
    return maxStorageBytes;
  }

  public void setMaxStorageBytes(Long maxStorageBytes) {
    this.maxStorageBytes = maxStorageBytes;
  }

  public Long getMaxPdfExports() {
    return maxPdfExports;
  }

  public void setMaxPdfExports(Long maxPdfExports) {
    this.maxPdfExports = maxPdfExports;
  }

  public Long getMaxEmails() {
    return maxEmails;
  }

  public void setMaxEmails(Long maxEmails) {
    this.maxEmails = maxEmails;
  }

  public Long getMaxJobs() {
    return maxJobs;
  }

  public void setMaxJobs(Long maxJobs) {
    this.maxJobs = maxJobs;
  }

  public Long getBurstRequestsPerMinute() {
    return burstRequestsPerMinute;
  }

  public void setBurstRequestsPerMinute(Long burstRequestsPerMinute) {
    this.burstRequestsPerMinute = burstRequestsPerMinute;
  }

  public Long getMaxConcurrentRequests() {
    return maxConcurrentRequests;
  }

  public void setMaxConcurrentRequests(Long maxConcurrentRequests) {
    this.maxConcurrentRequests = maxConcurrentRequests;
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
}
