package com.bigbrightpaints.erp.modules.accounting.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record AccrualRequest(
    @NotNull Long debitAccountId,
    @NotNull Long creditAccountId,
    @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
    @JsonFormat(pattern = "yyyy-MM-dd") LocalDate entryDate,
    String referenceNumber,
    String memo,
    String idempotencyKey,
    @JsonFormat(pattern = "yyyy-MM-dd") LocalDate autoReverseDate,
    Boolean adminOverride) {}
