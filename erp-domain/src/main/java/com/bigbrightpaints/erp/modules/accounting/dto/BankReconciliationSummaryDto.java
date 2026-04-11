package com.bigbrightpaints.erp.modules.accounting.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

public record BankReconciliationSummaryDto(
    Long accountId,
    String accountCode,
    String accountName,
    @JsonFormat(pattern = "yyyy-MM-dd") LocalDate statementDate,
    BigDecimal ledgerBalance,
    BigDecimal statementEndingBalance,
    BigDecimal outstandingDeposits,
    BigDecimal outstandingChecks,
    BigDecimal difference,
    boolean balanced,
    List<BankReconciliationItemDto> unclearedDeposits,
    List<BankReconciliationItemDto> unclearedChecks) {

  public record BankReconciliationItemDto(
      Long journalEntryId,
      String referenceNumber,
      @JsonFormat(pattern = "yyyy-MM-dd") LocalDate entryDate,
      String memo,
      BigDecimal debit,
      BigDecimal credit,
      BigDecimal netAmount) {}
}
