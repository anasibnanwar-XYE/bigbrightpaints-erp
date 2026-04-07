package com.bigbrightpaints.erp.modules.reports.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record AccountStatementReportDto(
    Long accountId,
    String accountCode,
    String accountName,
    LocalDate from,
    LocalDate to,
    BigDecimal openingBalance,
    List<AccountStatementLineDto> entries,
    BigDecimal closingBalance) {}
