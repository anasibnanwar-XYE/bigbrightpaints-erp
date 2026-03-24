package com.bigbrightpaints.erp.modules.accounting.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record ReconciliationDiscrepancyDto(
    Long id,
    Long accountingPeriodId,
    LocalDate periodStart,
    LocalDate periodEnd,
    String type,
    String partnerType,
    Long partnerId,
    String partnerCode,
    String partnerName,
    BigDecimal expectedAmount,
    BigDecimal actualAmount,
    BigDecimal variance,
    String status,
    String resolution,
    String resolutionNote,
    Long resolutionJournalId,
    String resolvedBy,
    Instant resolvedAt,
    Instant createdAt,
    Instant updatedAt) {}
