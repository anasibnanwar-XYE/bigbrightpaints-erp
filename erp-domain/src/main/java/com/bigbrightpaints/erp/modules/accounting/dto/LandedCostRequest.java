package com.bigbrightpaints.erp.modules.accounting.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;

public record LandedCostRequest(
    @NotNull Long rawMaterialPurchaseId,
    @NotNull BigDecimal amount,
    @NotNull Long inventoryAccountId,
    @NotNull Long offsetAccountId,
    LocalDate entryDate,
    String memo,
    String referenceNumber,
    String idempotencyKey,
    Boolean adminOverride) {}
