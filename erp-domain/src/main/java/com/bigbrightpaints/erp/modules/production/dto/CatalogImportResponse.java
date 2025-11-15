package com.bigbrightpaints.erp.modules.production.dto;

import java.util.List;

public record CatalogImportResponse(
        int rowsProcessed,
        int brandsCreated,
        int productsCreated,
        int productsUpdated,
        int rawMaterialsSeeded,
        List<ImportError> errors
) {
    public record ImportError(long rowNumber, String message) {}
}
