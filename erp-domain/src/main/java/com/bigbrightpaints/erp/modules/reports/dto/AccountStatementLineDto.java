package com.bigbrightpaints.erp.modules.reports.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record AccountStatementLineDto(
    Long journalEntryId,
    LocalDate date,
    Instant timestamp,
    String reference,
    String description,
    BigDecimal debit,
    BigDecimal credit,
    BigDecimal runningBalance) {}
