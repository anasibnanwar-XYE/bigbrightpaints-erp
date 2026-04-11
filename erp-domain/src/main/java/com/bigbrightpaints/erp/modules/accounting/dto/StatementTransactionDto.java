package com.bigbrightpaints.erp.modules.accounting.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;

public record StatementTransactionDto(
    @JsonFormat(pattern = "yyyy-MM-dd") LocalDate entryDate,
    String referenceNumber,
    String memo,
    BigDecimal debit,
    BigDecimal credit,
    BigDecimal runningBalance,
    Long journalEntryId) {}
