package com.bigbrightpaints.erp.modules.reports.dto;

import java.math.BigDecimal;

public record BalanceWarningDto(
    Long accountId,
    String accountCode,
    String accountName,
    BigDecimal balance,
    String warningType,
    BigDecimal threshold,
    String severity,
    String reason) {}
