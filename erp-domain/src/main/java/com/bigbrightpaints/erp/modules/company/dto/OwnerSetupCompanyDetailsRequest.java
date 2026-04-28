package com.bigbrightpaints.erp.modules.company.dto;

import jakarta.validation.constraints.NotBlank;

public record OwnerSetupCompanyDetailsRequest(
    @NotBlank String name, @NotBlank String timezone, String stateCode) {}
