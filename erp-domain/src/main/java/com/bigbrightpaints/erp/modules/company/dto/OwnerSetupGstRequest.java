package com.bigbrightpaints.erp.modules.company.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;

public record OwnerSetupGstRequest(
    @NotNull Boolean enabled, BigDecimal defaultGstRate, String stateCode) {}
