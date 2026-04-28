package com.bigbrightpaints.erp.modules.admin.dto;

import java.time.Instant;

public record SuperAdminPasswordChangeResponseDto(
    String status, Instant changedAt, String sessionPolicy, String auditEvidence) {}
