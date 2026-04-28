package com.bigbrightpaints.erp.modules.admin.dto;

import java.time.Instant;

public record SuperAdminProfileSessionDto(
    String sessionId,
    String scopeCode,
    Instant issuedAt,
    Instant expiresAt,
    boolean active,
    String device,
    String ipAddress) {}
