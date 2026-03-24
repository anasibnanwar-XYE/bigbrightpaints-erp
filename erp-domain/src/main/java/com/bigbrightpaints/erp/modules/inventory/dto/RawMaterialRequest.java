package com.bigbrightpaints.erp.modules.inventory.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RawMaterialRequest(
    @NotBlank String name,
    String sku,
    String materialType,
    @NotBlank String unitType,
    @NotNull BigDecimal reorderLevel,
    @NotNull BigDecimal minStock,
    @NotNull BigDecimal maxStock,
    Long inventoryAccountId,
    String costingMethod) {}
