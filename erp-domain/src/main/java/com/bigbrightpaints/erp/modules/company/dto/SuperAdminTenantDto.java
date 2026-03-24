package com.bigbrightpaints.erp.modules.company.dto;

import java.time.Instant;

public record SuperAdminTenantDto(
    Long companyId,
    String companyCode,
    String companyName,
    String status,
    long activeUsers,
    long apiCallCount,
    long storageBytes,
    Instant lastActivityAt) {}
