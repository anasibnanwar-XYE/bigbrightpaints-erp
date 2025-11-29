package com.bigbrightpaints.erp.modules.factory.dto;

import java.math.BigDecimal;

public record PackagingSizeMappingRequest(
        String packagingSize,
        Long rawMaterialId,
        Integer unitsPerPack,
        Integer cartonSize,
        BigDecimal litersPerUnit
) {}
