package com.bigbrightpaints.erp.modules.company.dto;

import java.time.Instant;
import java.util.List;
import java.util.Set;

public record SuperAdminAddClientCreateResponse(
    Long tenantId,
    String tenantCode,
    String tenantName,
    String status,
    Owner owner,
    String planId,
    String billingStatus,
    Instant trialEndsAt,
    String supportTier,
    Quotas quotas,
    Set<String> modules,
    Activation activation,
    SuperAdminAddClientOptionsDto.SeedPolicy seedPolicy,
    Long auditEventId) {

  public record Owner(Long ownerId, String email, String displayName, String state) {}

  public record Quotas(
      long maxActiveUsers,
      long maxApiRequests,
      long maxStorageBytes,
      long maxConcurrentRequests,
      boolean softLimitEnabled,
      boolean hardLimitEnabled) {}

  public record Activation(
      String status,
      Instant sentAt,
      Instant expiresAt,
      Long tokenId,
      String delivery,
      List<String> redactedFields) {}
}
