package com.bigbrightpaints.erp.modules.accounting.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record JournalListItemDto(
    Long id,
    String referenceNumber,
    LocalDate entryDate,
    String memo,
    String status,
    String journalType,
    String sourceModule,
    String sourceReference,
    BigDecimal totalDebit,
    BigDecimal totalCredit) {}
