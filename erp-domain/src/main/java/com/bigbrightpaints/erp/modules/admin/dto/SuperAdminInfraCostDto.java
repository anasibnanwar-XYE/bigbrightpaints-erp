package com.bigbrightpaints.erp.modules.admin.dto;

import java.time.Instant;
import java.util.List;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public final class SuperAdminInfraCostDto {

  public static final long MAX_SNAPSHOT_AMOUNT_MINOR_UNITS = Long.MAX_VALUE / 6L;

  private SuperAdminInfraCostDto() {}

  public record SnapshotRequest(
      @NotBlank String component,
      @NotNull Instant periodStartAt,
      @NotNull Instant periodEndAt,
      @NotNull @Min(0) @Max(MAX_SNAPSHOT_AMOUNT_MINOR_UNITS) Long amountMinorUnits,
      @NotBlank String currency,
      @NotBlank String source,
      @NotBlank String reason,
      String notes) {}

  public record ArchiveRequest(@NotBlank String reason) {}

  public record SnapshotResponse(
      Long snapshotId,
      String component,
      Instant periodStartAt,
      Instant periodEndAt,
      long amountMinorUnits,
      String currency,
      String source,
      String status,
      String enteredBy,
      Instant createdAt,
      Instant updatedAt,
      Instant archivedAt,
      int correctionCount,
      Long auditEventId) {}

  public record CorrectionResponse(
      Long correctionId,
      Long snapshotId,
      long previousAmountMinorUnits,
      long newAmountMinorUnits,
      String previousCurrency,
      String newCurrency,
      String reason,
      String correctedBy,
      Instant correctedAt,
      Long auditEventId) {}

  public record Dashboard(
      Instant generatedAt,
      String currency,
      long totalCostMinorUnits,
      List<SnapshotResponse> latestComponentCosts,
      List<UsageAggregate> aggregateUsage,
      List<TenantCostScore> tenantCostScores,
      CostPrivacy privacy) {}

  public record UsageAggregate(String dimension, String unit, long used, long tenantCount) {}

  public record TenantCostScore(
      Long tenantId,
      String tenantCode,
      String status,
      long aggregateUsageUnits,
      long costScoreBasisPoints,
      long estimatedCostMinorUnits,
      List<UsageAggregate> usageBasis) {}

  public record CostPrivacy(
      boolean aggregateUsageOnly,
      List<String> exposedFields,
      List<String> forbiddenFields,
      String statement) {}
}
