package com.bigbrightpaints.erp.modules.company.dto;

import java.time.Instant;
import java.util.Map;

public record SuperAdminPlanTemplateDto(
    Long id,
    String stableId,
    String displayName,
    String status,
    int version,
    Instant effectiveFrom,
    Instant effectiveUntil,
    String cadence,
    long priceMinorUnits,
    String currency,
    int trialDurationDays,
    String supportTier,
    Map<String, Boolean> featureFlags,
    DefaultLimits defaultLimits,
    AssignedTenants assignedTenants,
    MutationPolicy mutationPolicy,
    Long auditEventId,
    Instant createdAt,
    Instant updatedAt,
    Instant archivedAt) {

  public record DefaultLimits(
      long maxActiveUsers,
      long maxApiRequests,
      long maxStorageBytes,
      long maxPdfExports,
      long maxEmails,
      long maxJobs,
      long burstRequestsPerMinute,
      long maxConcurrentRequests,
      boolean zeroMeansUnlimited) {}

  public record AssignedTenants(
      int count, String propagation, String archiveBehavior, String privacyMode) {}

  public record MutationPolicy(
      String versioning,
      String effectiveFromPolicy,
      String assignedTenantBehavior,
      String entitlementCacheInvalidation,
      String subscriptionPricePolicy,
      Instant cacheInvalidatedAt) {}
}
