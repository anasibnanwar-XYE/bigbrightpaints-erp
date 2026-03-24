package com.bigbrightpaints.erp.modules.factory.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record PackagingSizeMappingDto(
    Long id,
    UUID publicId,
    String packagingSize,
    Long rawMaterialId,
    String rawMaterialSku,
    String rawMaterialName,
    Integer unitsPerPack,
    Integer cartonSize,
    BigDecimal litersPerUnit,
    boolean active) {}
