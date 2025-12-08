package com.bigbrightpaints.erp.modules.production.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

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
        @NotEmpty List<String> colors,
        @NotEmpty List<String> sizes,
        String unitOfMeasure,
        String skuPrefix,
        BigDecimal basePrice,
        BigDecimal gstRate,
        BigDecimal minDiscountPercent,
        BigDecimal minSellingPrice,
        Map<String, Object> metadata
) {}
