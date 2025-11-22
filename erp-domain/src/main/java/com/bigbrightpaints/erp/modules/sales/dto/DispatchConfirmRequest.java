package com.bigbrightpaints.erp.modules.sales.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record DispatchConfirmRequest(
        Long packingSlipId,
        Long orderId,
        List<DispatchLine> lines,
        Boolean adminOverrideCreditLimit
) {
    public record DispatchLine(
            Long lineId,
            Long batchId,
            @NotNull BigDecimal shipQty,
            BigDecimal priceOverride,
            BigDecimal discount,
            BigDecimal taxRate,
            Boolean taxInclusive
    ) {}
}
