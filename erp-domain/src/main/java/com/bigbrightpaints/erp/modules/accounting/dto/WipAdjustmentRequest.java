package com.bigbrightpaints.erp.modules.accounting.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;

public record WipAdjustmentRequest(
    @NotNull Long productionLogId,
    @NotNull BigDecimal amount,
    @NotNull Long wipAccountId,
    @NotNull Long inventoryAccountId,
    @NotNull Direction direction,
    String memo,
    LocalDate entryDate,
    String referenceNumber,
    String idempotencyKey,
    Boolean adminOverride) {
  public enum Direction {
    ISSUE,
    COMPLETION
  }
}
