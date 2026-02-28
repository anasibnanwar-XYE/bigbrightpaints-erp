package com.bigbrightpaints.erp.modules.production.dto;

import jakarta.validation.constraints.NotBlank;

public record CatalogBrandRequest(
        @NotBlank(message = "Brand name is required")
        String name,
        String logoUrl,
        String description,
        Boolean active
) {
}
