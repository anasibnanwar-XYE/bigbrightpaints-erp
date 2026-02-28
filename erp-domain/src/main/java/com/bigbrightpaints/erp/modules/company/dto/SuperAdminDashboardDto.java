package com.bigbrightpaints.erp.modules.company.dto;

import java.time.Instant;

public record SuperAdminDashboardDto(
        long totalTenants,
        long activeTenants,
        long suspendedTenants,
        long deactivatedTenants,
        long totalUsers,
        long totalApiCalls,
        long totalStorageBytes,
        Instant recentActivityAt
) {
}
