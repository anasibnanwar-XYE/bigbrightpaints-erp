package com.bigbrightpaints.erp.modules.sales.dto;

import java.math.BigDecimal;
import java.util.List;

public record DispatchConfirmResponse(
        Long packingSlipId,
        Long salesOrderId,
        Long finalInvoiceId,
        Long arJournalEntryId,
        List<CogsPostingDto> cogsPostings,
        boolean dispatched
) {
    public record CogsPostingDto(Long inventoryAccountId, Long cogsAccountId, BigDecimal cost) {}
}
