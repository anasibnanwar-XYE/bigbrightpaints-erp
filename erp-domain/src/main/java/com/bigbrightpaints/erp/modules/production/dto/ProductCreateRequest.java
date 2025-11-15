package com.bigbrightpaints.erp.modules.production.dto;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.util.Map;

public record ProductCreateRequest(
        Long brandId,
        String brandName,
        String brandCode,
        @NotBlank(message = "Product name is required")
        String productName,
        @NotBlank(message = "Category is required")
        String category,
        String defaultColour,
        String sizeLabel,
        String unitOfMeasure,
        String customSkuCode,
        BigDecimal basePrice,
        BigDecimal gstRate,
        BigDecimal minDiscountPercent,
        BigDecimal minSellingPrice,
        Map<String, Object> metadata
) {}
