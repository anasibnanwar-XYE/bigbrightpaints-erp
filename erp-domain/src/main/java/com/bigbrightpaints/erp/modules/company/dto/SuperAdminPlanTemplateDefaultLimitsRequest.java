package com.bigbrightpaints.erp.modules.company.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record SuperAdminPlanTemplateDefaultLimitsRequest(
    @NotNull @Min(0) Long maxActiveUsers,
    @NotNull @Min(0) Long maxApiRequests,
    @NotNull @Min(0) Long maxStorageBytes,
    @NotNull @Min(0) Long maxPdfExports,
    @NotNull @Min(0) Long maxEmails,
    @NotNull @Min(0) Long maxJobs,
    @NotNull @Min(0) Long burstRequestsPerMinute,
    @NotNull @Min(0) Long maxConcurrentRequests) {}
