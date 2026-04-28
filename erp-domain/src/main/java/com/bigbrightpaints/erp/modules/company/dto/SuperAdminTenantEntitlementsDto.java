package com.bigbrightpaints.erp.modules.company.dto;

import java.time.Instant;
import java.util.Map;

public record SuperAdminTenantEntitlementsDto(
    Long companyId,
    String companyCode,
    PlanSummary plan,
    Map<String, LimitEntitlement> limits,
    Map<String, FeatureEntitlement> features,
    CacheMetadata cache,
    BillingSnapshot billing,
    Long auditEventId) {

  public record PlanSummary(
      String planId,
      String displayName,
      int version,
      boolean custom,
      String supportTier,
      Instant effectiveFrom) {}

  public record LimitEntitlement(
      String key,
      long planDefault,
      Long tenantOverride,
      long effectiveValue,
      String source,
      Instant updatedAt) {}

  public record FeatureEntitlement(
      String key,
      boolean planDefault,
      Boolean tenantOverride,
      boolean effectiveValue,
      String source,
      Instant updatedAt) {}

  public record CacheMetadata(
      boolean invalidated,
      Instant invalidatedAt,
      boolean appliedWithoutRestart,
      String enforcementPolicyReference) {}

  public record BillingSnapshot(
      String snapshotPlanId,
      String displayName,
      String cadence,
      long priceMinorUnits,
      String currency,
      Instant capturedAt,
      boolean repriceApplied,
      String policy) {}
}
