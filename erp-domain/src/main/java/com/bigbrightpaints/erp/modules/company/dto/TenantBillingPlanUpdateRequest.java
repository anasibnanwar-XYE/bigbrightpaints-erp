package com.bigbrightpaints.erp.modules.company.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record TenantBillingPlanUpdateRequest(
    @Size(max = 64, message = "planCode must be at most 64 characters") String planCode,
    @Size(max = 255, message = "planName must be at most 255 characters") String planName,
    @Size(max = 16, message = "currency must be at most 16 characters") String currency,
    @Min(value = 0, message = "monthlyRate must be greater than or equal to 0")
        BigDecimal monthlyRate,
    @Min(value = 0, message = "annualRate must be greater than or equal to 0") BigDecimal annualRate,
    @Min(value = 0, message = "seats must be greater than or equal to 0") Long seats) {}
