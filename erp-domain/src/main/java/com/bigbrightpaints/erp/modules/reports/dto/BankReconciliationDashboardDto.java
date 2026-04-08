package com.bigbrightpaints.erp.modules.reports.dto;

import java.math.BigDecimal;

public record BankReconciliationDashboardDto(
    Long bankAccountId,
    String bankAccountCode,
    String bankAccountName,
    BigDecimal ledgerBalance,
    BigDecimal statementBalance,
    BigDecimal variance,
    boolean balanced) {}
