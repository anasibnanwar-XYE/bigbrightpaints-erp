package com.bigbrightpaints.erp.modules.accounting.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;

public record InventoryRevaluationRequest(
    @NotNull Long inventoryAccountId,
    @NotNull Long revaluationAccountId,
    @NotNull BigDecimal deltaAmount,
    String memo,
    LocalDate entryDate,
    String referenceNumber,
    String idempotencyKey,
    Boolean adminOverride) {}
