package com.bigbrightpaints.erp.modules.company.dto;

import java.util.Map;

import jakarta.validation.constraints.Size;

public record SuperAdminTenantEntitlementOverrideRequest(
    Map<String, Long> limits,
    Map<String, Boolean> features,
    @Size(max = 300, message = "reason must be at most 300 characters") String reason) {}
