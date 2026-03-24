package com.bigbrightpaints.erp.modules.accounting.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.NotNull;

public record BankReconciliationRequest(
    @NotNull Long bankAccountId,
    @NotNull LocalDate statementDate,
    @NotNull BigDecimal statementEndingBalance,
    LocalDate startDate,
    LocalDate endDate,
    List<String> clearedReferences,
    Long accountingPeriodId,
    Boolean markAsComplete,
    String note) {}
