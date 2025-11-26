package com.bigbrightpaints.erp.modules.inventory.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

public record DispatchConfirmationRequest(
        @NotNull Long packagingSlipId,
        @NotNull List<LineConfirmation> lines,
        String notes,
        String confirmedBy
) {
    public record LineConfirmation(
            @NotNull Long lineId,
            @NotNull BigDecimal shippedQuantity,
            String notes
    ) {}
}
