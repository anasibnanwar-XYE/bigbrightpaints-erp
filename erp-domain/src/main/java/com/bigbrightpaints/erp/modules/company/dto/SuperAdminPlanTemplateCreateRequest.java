package com.bigbrightpaints.erp.modules.company.dto;

import java.time.Instant;
import java.util.Map;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SuperAdminPlanTemplateCreateRequest(
    @NotBlank @Size(max = 64) String stableId,
    @NotBlank @Size(max = 120) String displayName,
    @NotBlank @Size(max = 32) String cadence,
    @NotNull @Min(0) Long priceMinorUnits,
    @NotBlank @Size(min = 3, max = 3) String currency,
    @NotNull @Min(0) Integer trialDurationDays,
    @NotBlank @Size(max = 32) String supportTier,
    Map<@NotBlank @Size(max = 64) String, @NotNull Boolean> featureFlags,
    @Valid @NotNull SuperAdminPlanTemplateDto.DefaultLimits defaultLimits,
    Instant effectiveFrom,
    @Size(max = 300) String reason) {}
