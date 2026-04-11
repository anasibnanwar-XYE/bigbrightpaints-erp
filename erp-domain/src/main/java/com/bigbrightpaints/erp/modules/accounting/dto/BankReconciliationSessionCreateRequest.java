package com.bigbrightpaints.erp.modules.accounting.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.validation.constraints.NotNull;

public record BankReconciliationSessionCreateRequest(
    @NotNull Long bankAccountId,
    @NotNull @JsonFormat(pattern = "yyyy-MM-dd") LocalDate statementDate,
    @NotNull BigDecimal statementEndingBalance,
    @JsonFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
    @JsonFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
    Long accountingPeriodId,
    String note) {}
