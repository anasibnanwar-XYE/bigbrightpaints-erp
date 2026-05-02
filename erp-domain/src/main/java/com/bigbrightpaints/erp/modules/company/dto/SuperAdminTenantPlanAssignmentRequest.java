package com.bigbrightpaints.erp.modules.company.dto;

import java.util.Map;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record SuperAdminTenantPlanAssignmentRequest(
    @Size(max = 64, message = "planId must be at most 64 characters") String planId,
    @Valid CustomPlan customPlan,
    Boolean repriceSubscription,
    @Size(max = 300, message = "reason must be at most 300 characters") String reason) {

  public record CustomPlan(
      @Size(max = 120, message = "displayName must be at most 120 characters") String displayName,
      @Size(max = 32, message = "cadence must be at most 32 characters") String cadence,
      @Min(value = 0, message = "priceMinorUnits must be greater than or equal to 0")
          Long priceMinorUnits,
      @Size(max = 3, message = "currency must be exactly 3 characters") String currency,
      @Min(value = 0, message = "trialDurationDays must be greater than or equal to 0")
          Integer trialDurationDays,
      @Size(max = 32, message = "supportTier must be at most 32 characters") String supportTier,
      Map<String, Boolean> featureFlags,
      @Valid SuperAdminTenantEntitlementLimitsRequest defaultLimits) {}
}
