package com.bigbrightpaints.erp.modules.factory.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PackagingSizeMappingRequest(
    @NotBlank String packagingSize,
    @NotNull Long rawMaterialId,
    @NotNull @Positive Integer unitsPerPack,
    @Positive Integer cartonSize,
    @Positive BigDecimal litersPerUnit) {}
