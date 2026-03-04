package com.bigbrightpaints.erp.modules.accounting.dto;

import java.util.List;

public record ReconciliationDiscrepancyListResponse(
        List<ReconciliationDiscrepancyDto> items,
        long openCount,
        long resolvedCount
) {
}
