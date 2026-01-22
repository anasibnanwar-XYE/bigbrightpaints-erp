package com.bigbrightpaints.erp.modules.accounting.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DebitNoteRequest(
        @NotNull Long purchaseId,
        @DecimalMin(value = "0.01") BigDecimal amount,
        LocalDate entryDate,
        String referenceNumber,
        String memo,
        String idempotencyKey,
        Boolean adminOverride
) {
}
