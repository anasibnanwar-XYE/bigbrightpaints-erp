package com.bigbrightpaints.erp.modules.company.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.bigbrightpaints.erp.core.validation.ValidationUtils;
import com.bigbrightpaints.erp.modules.company.dto.SuperAdminTenantEntitlementsDto;
import com.bigbrightpaints.erp.modules.company.dto.SuperAdminUsageDtos;

@Service
public class SuperAdminUsageService {

  private static final String OPERATIONAL_ACCOUNTING_MODE = "RUNTIME_WINDOW";
  private static final String IN_FLIGHT_ACCOUNTING_MODE = "RUNTIME_SNAPSHOT";
  private static final String COUNT_UNIT = "COUNT";
  private static final String OPERATIONAL_TIMEZONE = "UTC";

  private final TenantUsageRollupService tenantUsageRollupService;
  private final TenantRuntimeEnforcementService tenantRuntimeEnforcementService;

  public SuperAdminUsageService(
      TenantUsageRollupService tenantUsageRollupService,
      TenantRuntimeEnforcementService tenantRuntimeEnforcementService) {
    this.tenantUsageRollupService = tenantUsageRollupService;
    this.tenantRuntimeEnforcementService = tenantRuntimeEnforcementService;
  }

  public SuperAdminUsageDtos.TenantUsage getTenantUsage(
      Long companyId,
      Map<String, SuperAdminTenantEntitlementsDto.LimitEntitlement> effectiveLimits) {
    SuperAdminUsageDtos.TenantUsage durableUsage =
        tenantUsageRollupService.getTenantUsage(companyId, effectiveLimits);
    TenantRuntimeEnforcementService.TenantRuntimeSnapshot runtimeSnapshot =
        tenantRuntimeEnforcementService.snapshot(durableUsage.companyCode());
    return new SuperAdminUsageDtos.TenantUsage(
        durableUsage.companyId(),
        durableUsage.companyCode(),
        durableUsage.companyName(),
        durableUsage.timezone(),
        durableUsage.currentDailyPeriod(),
        durableUsage.currentMonthlyPeriod(),
        durableUsage.dimensions(),
        operationalDimensions(runtimeSnapshot),
        durableUsage.history());
  }

  private List<SuperAdminUsageDtos.DimensionUsage> operationalDimensions(
      TenantRuntimeEnforcementService.TenantRuntimeSnapshot snapshot) {
    if (snapshot == null || snapshot.metrics() == null) {
      throw ValidationUtils.invalidState("Tenant runtime usage snapshot is unavailable");
    }
    TenantRuntimeEnforcementService.TenantRuntimeMetrics metrics = snapshot.metrics();
    List<SuperAdminUsageDtos.DimensionUsage> dimensions = new ArrayList<>();
    SuperAdminUsageDtos.Period currentWindow =
        currentWindowPeriod(metrics.currentWindowStartAt(), metrics.currentWindowEndAt());
    dimensions.add(
        operationalDimension(
            "CURRENT_WINDOW_API_REQUESTS",
            "Current-window API requests",
            OPERATIONAL_ACCOUNTING_MODE,
            metrics.minuteRequestCount(),
            snapshot.maxRequestsPerMinute(),
            currentWindow));
    dimensions.add(
        operationalDimension(
            "CURRENT_WINDOW_REJECTED_REQUESTS",
            "Current-window rejected requests",
            OPERATIONAL_ACCOUNTING_MODE,
            metrics.minuteRejectedCount(),
            snapshot.maxRequestsPerMinute(),
            currentWindow));
    dimensions.add(
        operationalDimension(
            "IN_FLIGHT_CONCURRENT_REQUESTS",
            "In-flight concurrent requests",
            IN_FLIGHT_ACCOUNTING_MODE,
            metrics.inFlightRequests(),
            snapshot.maxConcurrentRequests(),
            pointInTimePeriod(metrics.capturedAt())));
    return List.copyOf(dimensions);
  }

  private SuperAdminUsageDtos.DimensionUsage operationalDimension(
      String dimension,
      String label,
      String accountingMode,
      long used,
      long limit,
      SuperAdminUsageDtos.Period period) {
    return new SuperAdminUsageDtos.DimensionUsage(
        dimension,
        label,
        accountingMode,
        COUNT_UNIT,
        Math.max(used, 0L),
        Math.max(limit, 0L),
        percentage(used, limit),
        state(used, limit),
        period);
  }

  private SuperAdminUsageDtos.Period currentWindowPeriod(Instant startAt, Instant endAt) {
    if (startAt == null || endAt == null) {
      throw ValidationUtils.invalidState("Tenant runtime window metadata is unavailable");
    }
    return new SuperAdminUsageDtos.Period(
        "MINUTE", startAt.toString(), startAt, endAt, OPERATIONAL_TIMEZONE, false, null);
  }

  private SuperAdminUsageDtos.Period pointInTimePeriod(Instant capturedAt) {
    if (capturedAt == null) {
      throw ValidationUtils.invalidState("Tenant runtime snapshot timestamp is unavailable");
    }
    return new SuperAdminUsageDtos.Period(
        "POINT_IN_TIME",
        capturedAt.toString(),
        capturedAt,
        capturedAt,
        OPERATIONAL_TIMEZONE,
        false,
        null);
  }

  private long percentage(long used, long limit) {
    if (used <= 0L || limit <= 0L) {
      return 0L;
    }
    return Math.min(100L, (Math.max(used, 0L) * 100L) / Math.max(limit, 1L));
  }

  private String state(long used, long limit) {
    if (limit <= 0L) {
      return "OK";
    }
    long percentage = percentage(used, limit);
    if (percentage >= 100L) {
      return "BLOCKED";
    }
    if (percentage >= 80L) {
      return "WARNING";
    }
    return "OK";
  }
}
