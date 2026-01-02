package com.bigbrightpaints.erp.modules.inventory.dto;

import java.util.List;

public record OpeningStockImportResponse(
        int rowsProcessed,
        int rawMaterialsCreated,
        int rawMaterialBatchesCreated,
        int finishedGoodsCreated,
        int finishedGoodBatchesCreated,
        List<ImportError> errors
) {
    public record ImportError(long rowNumber, String message) {}
}
