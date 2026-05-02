package com.bigbrightpaints.erp.modules.company.dto;

import jakarta.validation.constraints.Min;

public record SuperAdminTenantEntitlementLimitsRequest(
    @Min(value = 0, message = "maxActiveUsers must be greater than or equal to 0")
        Long maxActiveUsers,
    @Min(value = 0, message = "maxApiRequests must be greater than or equal to 0")
        Long maxApiRequests,
    @Min(value = 0, message = "maxStorageBytes must be greater than or equal to 0")
        Long maxStorageBytes,
    @Min(value = 0, message = "maxPdfExports must be greater than or equal to 0")
        Long maxPdfExports,
    @Min(value = 0, message = "maxEmails must be greater than or equal to 0") Long maxEmails,
    @Min(value = 0, message = "maxJobs must be greater than or equal to 0") Long maxJobs,
    @Min(value = 0, message = "burstRequestsPerMinute must be greater than or equal to 0")
        Long burstRequestsPerMinute,
    @Min(value = 0, message = "maxConcurrentRequests must be greater than or equal to 0")
        Long maxConcurrentRequests) {}
