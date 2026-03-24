package com.bigbrightpaints.erp.modules.accounting.dto;

import java.time.Instant;
import java.util.UUID;

public record PeriodCloseRequestDto(
    Long id,
    UUID publicId,
    Long periodId,
    String periodLabel,
    String periodStatus,
    String status,
    boolean forceRequested,
    String requestedBy,
    String requestNote,
    Instant requestedAt,
    String reviewedBy,
    Instant reviewedAt,
    String reviewNote,
    String approvalNote) {}
