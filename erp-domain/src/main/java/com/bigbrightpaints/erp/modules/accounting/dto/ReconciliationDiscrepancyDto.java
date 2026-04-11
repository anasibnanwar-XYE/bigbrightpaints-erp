package com.bigbrightpaints.erp.modules.accounting.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;

public record ReconciliationDiscrepancyDto(
    Long id,
    Long accountingPeriodId,
    @JsonFormat(pattern = "yyyy-MM-dd") LocalDate periodStart,
    @JsonFormat(pattern = "yyyy-MM-dd") LocalDate periodEnd,
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
    @JsonFormat(shape = JsonFormat.Shape.STRING) Instant resolvedAt,
    @JsonFormat(shape = JsonFormat.Shape.STRING) Instant createdAt,
    @JsonFormat(shape = JsonFormat.Shape.STRING) Instant updatedAt) {}
