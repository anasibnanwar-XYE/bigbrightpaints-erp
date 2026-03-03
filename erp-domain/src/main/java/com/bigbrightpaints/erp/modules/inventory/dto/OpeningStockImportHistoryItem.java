package com.bigbrightpaints.erp.modules.inventory.dto;

import java.time.Instant;

public record OpeningStockImportHistoryItem(
        Long id,
        String idempotencyKey,
        String referenceNumber,
        String fileName,
        Long journalEntryId,
        int rowsProcessed,
        int rawMaterialsCreated,
        int rawMaterialBatchesCreated,
        int finishedGoodsCreated,
        int finishedGoodBatchesCreated,
        int errorCount,
        Instant createdAt
) {}
