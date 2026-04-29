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
