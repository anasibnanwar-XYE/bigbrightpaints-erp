package com.bigbrightpaints.erp.modules.production.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Request to generate multiple product variants (color x size) in one shot.
 */
public record BulkVariantRequest(
        Long brandId,
        String brandName,
        String brandCode,
        @NotBlank String baseProductName,
        @NotBlank String category,
        List<String> colors,
        List<String> sizes,
        List<@Valid ColorSizeMatrixEntry> colorSizeMatrix,
        String unitOfMeasure,
        String skuPrefix,
        BigDecimal basePrice,
        BigDecimal gstRate,
        BigDecimal minDiscountPercent,
        BigDecimal minSellingPrice,
        Map<String, Object> metadata
) {
    public record ColorSizeMatrixEntry(
            @NotBlank String color,
            List<String> sizes
    ) {}
}
