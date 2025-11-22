package com.bigbrightpaints.erp.modules.accounting.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record InventoryRevaluationRequest(
        @NotNull Long inventoryAccountId,
        @NotNull Long revaluationAccountId,
        @NotNull BigDecimal deltaAmount,
        String memo,
        LocalDate entryDate,
        String referenceNumber,
        String idempotencyKey,
        Boolean adminOverride
) {}
