package com.bigbrightpaints.erp.modules.company.dto;

import java.time.Instant;

public record SuperAdminTenantUsageDto(
        Long companyId,
        String companyCode,
        String status,
        long apiCallCount,
        long activeUsers,
        long storageBytes,
        Instant lastActivityAt
) {
}
