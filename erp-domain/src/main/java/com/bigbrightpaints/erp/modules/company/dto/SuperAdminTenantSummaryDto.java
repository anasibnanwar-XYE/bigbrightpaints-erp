package com.bigbrightpaints.erp.modules.company.dto;

import java.time.Instant;
import java.util.Set;

public record SuperAdminTenantSummaryDto(
    Long companyId,
    String companyCode,
    String companyName,
    String timezone,
    String status,
    String plan,
    String billingStatus,
    UsageSummary usage,
    Instant trialEndsAt,
    HealthSummary health,
    String lifecycleState,
    long activeUserCount,
    long quotaMaxActiveUsers,
    long apiActivityCount,
    long quotaMaxApiRequests,
    long auditStorageBytes,
    long quotaMaxStorageBytes,
    long currentConcurrentRequests,
    long quotaMaxConcurrentRequests,
    Set<String> enabledModules,
    MainAdminSummaryDto mainAdmin,
    Instant lastActivityAt) {

  public SuperAdminTenantSummaryDto(
      Long companyId,
      String companyCode,
      String companyName,
      String timezone,
      String lifecycleState,
      String lifecycleReason,
      long activeUserCount,
      long quotaMaxActiveUsers,
      long apiActivityCount,
      long quotaMaxApiRequests,
      long auditStorageBytes,
      long quotaMaxStorageBytes,
      long currentConcurrentRequests,
      long quotaMaxConcurrentRequests,
      Set<String> enabledModules,
      MainAdminSummaryDto mainAdmin,
      Instant lastActivityAt) {
    this(
        companyId,
        companyCode,
        companyName,
        timezone,
        lifecycleState,
        "TRIAL",
        "MANUAL",
        new UsageSummary(
            activeUserCount,
            quotaMaxActiveUsers,
            apiActivityCount,
            quotaMaxApiRequests,
            auditStorageBytes,
            quotaMaxStorageBytes,
            currentConcurrentRequests,
            quotaMaxConcurrentRequests),
        null,
        new HealthSummary("UNKNOWN", 0, "Health summary pending"),
        lifecycleState,
        activeUserCount,
        quotaMaxActiveUsers,
        apiActivityCount,
        quotaMaxApiRequests,
        auditStorageBytes,
        quotaMaxStorageBytes,
        currentConcurrentRequests,
        quotaMaxConcurrentRequests,
        enabledModules,
        mainAdmin,
        lastActivityAt);
  }

  public record UsageSummary(
      long activeUsers,
      long maxUsers,
      long apiCalls,
      long maxApiCalls,
      long storageBytes,
      long maxStorageBytes,
      long concurrentRequests,
      long maxConcurrentRequests) {}

  public record HealthSummary(String status, int riskScore, String message) {}
}
