package com.bigbrightpaints.erp.modules.company.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class SuperAdminBillingDtos {

  private SuperAdminBillingDtos() {}

  public record SubscriptionRequest(
      @NotBlank @Size(max = 64) String planId,
      @Size(max = 160) String customPlanName,
      @NotBlank @Size(max = 32) String status,
      @NotBlank @Size(max = 32) String cadence,
      @NotNull @Min(0) Long amountMinorUnits,
      @NotBlank @Size(min = 3, max = 3) String currency,
      @NotBlank @Size(max = 32) String collectionMode,
      @NotNull Instant periodStartAt,
      Instant periodEndAt,
      Instant renewalAt,
      Instant dueAt,
      Instant trialStartAt,
      Instant trialEndAt,
      Instant graceUntilAt,
      @Size(max = 160) String externalReference,
      @Size(max = 300) String reason) {}

  public record LedgerEntryRequest(
      @NotNull @Min(1) Long amountMinorUnits,
      @NotBlank @Size(min = 3, max = 3) String currency,
      @NotBlank @Size(max = 300) String reason,
      @NotBlank @Size(max = 160) String idempotencyKey,
      @Size(max = 160) String externalReference) {}

  public record AdjustmentRequest(
      @NotNull @Min(1) Long amountMinorUnits,
      @NotBlank @Size(min = 3, max = 3) String currency,
      @NotBlank @Size(max = 16) String direction,
      @NotBlank @Size(max = 300) String reason,
      @NotBlank @Size(max = 160) String idempotencyKey,
      @Size(max = 160) String externalReference) {}

  public record CommercialStateActionRequest(
      @NotBlank @Size(max = 300) String reason, Instant effectiveAt, Instant graceUntilAt) {}

  public record SubscriptionResponse(
      Long subscriptionId,
      Long tenantId,
      String tenantCode,
      String planId,
      String customPlanName,
      String status,
      String billingStatus,
      String cadence,
      long amountMinorUnits,
      String currency,
      String collectionMode,
      Instant periodStartAt,
      Instant periodEndAt,
      Instant renewalAt,
      Instant dueAt,
      Instant trialStartAt,
      Instant trialEndAt,
      Instant graceUntilAt,
      Instant canceledAt,
      Instant archivedAt,
      String externalReference,
      Long auditEventId) {}

  public record LedgerEntryResponse(
      Long entryId,
      Long tenantId,
      String tenantCode,
      Long subscriptionId,
      String entryType,
      String direction,
      long amountMinorUnits,
      String currency,
      String reason,
      String externalReference,
      String idempotencyKey,
      long balanceBeforeMinorUnits,
      long balanceAfterMinorUnits,
      String billingStatusAfter,
      Instant createdAt,
      Long auditEventId) {}

  public record LedgerResponse(
      Long tenantId,
      String tenantCode,
      Long subscriptionId,
      long balanceDueMinorUnits,
      String currency,
      String billingStatus,
      List<LedgerEntryResponse> entries,
      BillingPrivacy privacy) {}

  public record BillingPrivacy(
      boolean platformOnly, List<String> exposedData, List<String> forbiddenData, String rule) {}

  public record BillingStatusSummary(
      String billingStatus,
      long balanceDueMinorUnits,
      String currency,
      Instant trialEndsAt,
      Long subscriptionId,
      int historyRows) {}

  public record CommercialStateResponse(
      Long tenantId,
      String tenantCode,
      Long subscriptionId,
      String commercialState,
      String billingStatus,
      String lifecycleState,
      String runtimeState,
      String reason,
      Instant effectiveAt,
      Instant graceUntilAt,
      Instant canceledAt,
      Instant archivedAt,
      boolean loginAllowed,
      boolean safeReadsAllowed,
      boolean writesAllowed,
      boolean backgroundWorkAllowed,
      boolean defaultListIncluded,
      Long auditEventId,
      Map<String, AccessPolicy> accessMatrix) {}

  public record AccessPolicy(
      boolean loginAllowed,
      boolean safeReadsAllowed,
      boolean writesAllowed,
      boolean backgroundWorkAllowed,
      String runtimeState,
      String recoveryRule) {}

  public record BillingMetricsResponse(
      Instant calculatedAt, Map<String, CurrencyMetrics> currencies) {
    public CurrencyMetrics get(String currency) {
      return currencies == null ? null : currencies.get(currency);
    }
  }

  public record CurrencyMetrics(
      String currency,
      long mrrMinorUnits,
      long arrMinorUnits,
      int activeSubscriptionCount,
      int excludedSubscriptionCount,
      String annualToMonthlyRoundingPolicy,
      String lifecycleInclusionPolicy) {}
}
