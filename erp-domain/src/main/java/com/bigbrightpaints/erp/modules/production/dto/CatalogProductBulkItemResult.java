package com.bigbrightpaints.erp.modules.production.dto;

public record CatalogProductBulkItemResult(
        int index,
        boolean success,
        String action,
        Long productId,
        String sku,
        String message,
        CatalogProductDto product
) {
}
