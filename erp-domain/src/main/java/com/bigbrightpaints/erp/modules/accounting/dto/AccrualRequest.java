package com.bigbrightpaints.erp.modules.accounting.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AccrualRequest(
        @NotNull Long debitAccountId,
        @NotNull Long creditAccountId,
        @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
        LocalDate entryDate,
        String referenceNumber,
        String memo,
        String idempotencyKey,
        LocalDate autoReverseDate,
        Boolean adminOverride
) {
}
