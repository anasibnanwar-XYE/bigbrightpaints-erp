package com.bigbrightpaints.erp.modules.accounting.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.validation.constraints.NotNull;

public record BankReconciliationRequest(
    @NotNull Long bankAccountId,
    @NotNull @JsonFormat(pattern = "yyyy-MM-dd") LocalDate statementDate,
    @NotNull BigDecimal statementEndingBalance,
    @JsonFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
    @JsonFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
    List<String> clearedReferences,
    Long accountingPeriodId,
    Boolean markAsComplete,
    String note) {}
