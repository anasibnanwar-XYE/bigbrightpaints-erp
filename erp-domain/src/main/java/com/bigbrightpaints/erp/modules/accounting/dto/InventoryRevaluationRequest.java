package com.bigbrightpaints.erp.modules.accounting.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.validation.constraints.NotNull;

public record InventoryRevaluationRequest(
    @NotNull Long inventoryAccountId,
    @NotNull Long revaluationAccountId,
    @NotNull BigDecimal deltaAmount,
    String memo,
    @JsonFormat(pattern = "yyyy-MM-dd") LocalDate entryDate,
    String referenceNumber,
    String idempotencyKey,
    Boolean adminOverride) {}
