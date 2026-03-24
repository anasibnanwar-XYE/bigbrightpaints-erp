package com.bigbrightpaints.erp.modules.accounting.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record CreditNoteRequest(
    @NotNull Long invoiceId,
    @DecimalMin(value = "0.01") BigDecimal amount,
    LocalDate entryDate,
    String referenceNumber,
    String memo,
    String idempotencyKey,
    Boolean adminOverride) {}
