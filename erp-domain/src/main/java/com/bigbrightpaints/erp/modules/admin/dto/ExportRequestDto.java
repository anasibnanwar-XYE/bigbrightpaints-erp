package com.bigbrightpaints.erp.modules.admin.dto;

import java.time.Instant;

import com.bigbrightpaints.erp.modules.admin.domain.ExportApprovalStatus;

public record ExportRequestDto(
    Long id,
    Long userId,
    String userEmail,
    String reportType,
    String parameters,
    ExportApprovalStatus status,
    String rejectionReason,
    Instant createdAt,
    String approvedBy,
    Instant approvedAt) {}
