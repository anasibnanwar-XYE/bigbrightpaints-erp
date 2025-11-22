package com.bigbrightpaints.erp.modules.accounting.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CreditNoteRequest(
        @NotNull Long invoiceId,
        LocalDate entryDate,
        String referenceNumber,
        String memo,
        String idempotencyKey,
        Boolean adminOverride
) {
}
