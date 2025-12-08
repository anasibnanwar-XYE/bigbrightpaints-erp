package com.bigbrightpaints.erp.modules.accounting.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record DealerSettlementRequest(
        @NotNull Long dealerId,
        // Legacy single-tender cash field (will be ignored if payments list is provided)
        Long cashAccountId,
        Long discountAccountId,
        Long writeOffAccountId,
        Long fxGainAccountId,
        Long fxLossAccountId,
        LocalDate settlementDate,
        String referenceNumber,
        String memo,
        String idempotencyKey,
        Boolean adminOverride,
        @NotEmpty List<@Valid SettlementAllocationRequest> allocations,
        List<@Valid SettlementPaymentRequest> payments
) {
}
