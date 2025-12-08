package com.bigbrightpaints.erp.modules.factory.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Response from bulk-to-size packing operation.
 */
public record BulkPackResponse(
    Long bulkBatchId,
    String bulkBatchCode,
    BigDecimal volumeDeducted,
    BigDecimal remainingBulkQuantity,
    BigDecimal packagingCost,
    List<ChildBatchDto> childBatches,
    Long journalEntryId,
    Instant packedAt
) {
    public record ChildBatchDto(
        Long id,
        UUID publicId,
        String batchCode,
        Long finishedGoodId,
        String finishedGoodCode,
        String finishedGoodName,
        String sizeLabel,
        BigDecimal quantity,
        BigDecimal unitCost,
        BigDecimal totalValue
    ) {}
}
