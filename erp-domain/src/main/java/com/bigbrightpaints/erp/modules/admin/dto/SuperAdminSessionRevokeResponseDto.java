package com.bigbrightpaints.erp.modules.admin.dto;

import java.time.Instant;

public record SuperAdminSessionRevokeResponseDto(
    String sessionId, boolean revoked, Instant revokedAt, String auditEvidence) {}
