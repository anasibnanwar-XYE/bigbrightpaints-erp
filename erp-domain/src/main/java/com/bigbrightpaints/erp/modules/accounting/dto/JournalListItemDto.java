package com.bigbrightpaints.erp.modules.accounting.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;

public record JournalListItemDto(
    Long id,
    String referenceNumber,
    @JsonFormat(pattern = "yyyy-MM-dd") LocalDate entryDate,
    String memo,
    String status,
    String journalType,
    String sourceModule,
    String sourceReference,
    BigDecimal totalDebit,
    BigDecimal totalCredit) {}
