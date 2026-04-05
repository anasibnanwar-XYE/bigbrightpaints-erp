package com.bigbrightpaints.erp.modules.accounting.dto;

import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonFormat;

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
    @JsonFormat(shape = JsonFormat.Shape.STRING) Instant requestedAt,
    String reviewedBy,
    @JsonFormat(shape = JsonFormat.Shape.STRING) Instant reviewedAt,
    String reviewNote,
    String approvalNote) {}
