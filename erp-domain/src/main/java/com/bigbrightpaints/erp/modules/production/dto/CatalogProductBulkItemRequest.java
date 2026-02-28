package com.bigbrightpaints.erp.modules.production.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record CatalogProductBulkItemRequest(
        Long id,
        String sku,
        @NotNull(message = "product payload is required")
        @Valid
        CatalogProductRequest product
) {
}
