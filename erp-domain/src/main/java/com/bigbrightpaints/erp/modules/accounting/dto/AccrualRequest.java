package com.bigbrightpaints.erp.modules.accounting.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record AccrualRequest(
    @NotNull Long debitAccountId,
    @NotNull Long creditAccountId,
    @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
    LocalDate entryDate,
    String referenceNumber,
    String memo,
    String idempotencyKey,
    LocalDate autoReverseDate,
    Boolean adminOverride) {}
