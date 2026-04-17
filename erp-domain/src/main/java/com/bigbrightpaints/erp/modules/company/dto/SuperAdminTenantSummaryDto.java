package com.bigbrightpaints.erp.modules.company.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;

public record SuperAdminTenantSummaryDto(
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
    BillingPlanSummary billingPlan,
    Instant lastActivityAt) {

  public record BillingPlanSummary(
      String planCode,
      String planName,
      String currency,
      BigDecimal monthlyRate,
      BigDecimal annualRate,
      long seats,
      Instant updatedAt,
      String updatedBy) {}
}
