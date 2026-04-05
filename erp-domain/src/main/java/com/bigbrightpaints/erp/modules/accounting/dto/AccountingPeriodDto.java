package com.bigbrightpaints.erp.modules.accounting.dto;

import java.time.Instant;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;

public record AccountingPeriodDto(
    Long id,
    int year,
    int month,
    @JsonFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
    @JsonFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
    String label,
    String status,
    boolean bankReconciled,
    @JsonFormat(shape = JsonFormat.Shape.STRING) Instant bankReconciledAt,
    String bankReconciledBy,
    boolean inventoryCounted,
    @JsonFormat(shape = JsonFormat.Shape.STRING) Instant inventoryCountedAt,
    String inventoryCountedBy,
    @JsonFormat(shape = JsonFormat.Shape.STRING) Instant closedAt,
    String closedBy,
    String closedReason,
    @JsonFormat(shape = JsonFormat.Shape.STRING) Instant lockedAt,
    String lockedBy,
    String lockReason,
    @JsonFormat(shape = JsonFormat.Shape.STRING) Instant reopenedAt,
    String reopenedBy,
    String reopenReason,
    Long closingJournalEntryId,
    String checklistNotes,
    String costingMethod) {}
