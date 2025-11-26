package com.bigbrightpaints.erp.modules.inventory.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record DispatchConfirmationResponse(
        Long packagingSlipId,
        String slipNumber,
        String status,
        Instant confirmedAt,
        String confirmedBy,
        BigDecimal totalOrderedAmount,
        BigDecimal totalShippedAmount,
        BigDecimal totalBackorderAmount,
        Long journalEntryId,
        Long cogsJournalEntryId,
        List<LineResult> lines,
        Long backorderSlipId
) {
    public record LineResult(
            Long lineId,
            String productCode,
            String productName,
            BigDecimal orderedQuantity,
            BigDecimal shippedQuantity,
            BigDecimal backorderQuantity,
            BigDecimal unitCost,
            BigDecimal lineTotal,
            String notes
    ) {}
}
