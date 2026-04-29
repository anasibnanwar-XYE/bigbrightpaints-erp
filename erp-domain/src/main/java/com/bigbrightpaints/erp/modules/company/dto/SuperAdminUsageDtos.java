package com.bigbrightpaints.erp.modules.company.dto;

import java.time.Instant;
import java.util.List;

public final class SuperAdminUsageDtos {

  private SuperAdminUsageDtos() {}

  public record TenantUsage(
      Long companyId,
      String companyCode,
      String companyName,
      String timezone,
      Period currentDailyPeriod,
      Period currentMonthlyPeriod,
      List<DimensionUsage> dimensions,
      List<RollupWindow> history) {}

  public record TenantQuotaPolicy(
      Long companyId,
      String companyCode,
      String timezone,
      Period currentMonthlyPeriod,
      List<QuotaDimensionPolicy> dimensions,
      List<EmailQuotaCategoryPolicy> emailCategories) {}

  public record QuotaDimensionPolicy(
      String dimension,
      String label,
      String unit,
      long used,
      long limit,
      long percentage,
      String state,
      int warningThresholdPercent,
      long graceStartAt,
      long graceEndAt,
      long hardBlockAt,
      boolean safeReadsAllowed,
      boolean existingResourcesPreserved,
      boolean softLimitEnabled,
      boolean hardLimitEnabled,
      String loweredLimitBehavior) {}

  public record EmailQuotaCategoryPolicy(
      String category,
      boolean counted,
      boolean blockedWhenTenantEmailQuotaExhausted,
      String exemptReason,
      boolean mailhogEvidenceSafe,
      boolean tokenRedactionRequired) {}

  public record QuotaActionRequest(
      String dimension, Long units, Long bytes, String emailCategory, boolean dryRun) {}

  public record QuotaActionResult(
      Long companyId,
      String companyCode,
      String dimension,
      String emailCategory,
      String decision,
      boolean accepted,
      boolean usageRecorded,
      long requestedUnits,
      long usedBefore,
      long usedAfter,
      long limit,
      String stateBefore,
      String stateAfter,
      String reasonCode,
      String message,
      boolean safeReadsAllowed,
      boolean existingResourcesPreserved,
      boolean mailhogEvidenceSafe,
      boolean tokenRedactionRequired) {}

  public record PlatformUsage(
      Instant generatedAt,
      Period currentMonthlyPeriod,
      List<DimensionAggregate> totals,
      List<TenantSummary> tenants) {}

  public record TenantUsageHistory(
      Long companyId,
      String companyCode,
      String timezone,
      String periodType,
      List<RollupWindow> windows) {}

  public record Period(
      String periodType,
      String periodId,
      Instant startAt,
      Instant endAt,
      String timezone,
      boolean closed,
      Instant closedAt) {}

  public record DimensionUsage(
      String dimension,
      String label,
      String accountingMode,
      String unit,
      long used,
      long limit,
      long percentage,
      String state,
      Period period) {}

  public record DimensionAggregate(
      String dimension,
      String label,
      String accountingMode,
      String unit,
      long used,
      long limit,
      long tenantCount) {}

  public record TenantSummary(
      Long companyId,
      String companyCode,
      String companyName,
      String status,
      List<DimensionUsage> dimensions) {}

  public record RollupWindow(
      String dimension,
      String label,
      String accountingMode,
      String unit,
      long used,
      long limit,
      Period period) {}
}
