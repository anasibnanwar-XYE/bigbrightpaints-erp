package com.bigbrightpaints.erp.modules.accounting.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

/**
 * Supports hybrid receipts: multiple incoming accounts (cash/bank/wallet) applied against dealer AR.
 */
public record DealerReceiptSplitRequest(
        @NotNull Long dealerId,
        @NotEmpty List<@Valid IncomingLine> incomingLines,
        String referenceNumber,
        String memo
) {
    public record IncomingLine(
            @NotNull Long accountId,
            @NotNull @DecimalMin(value = "0.01") BigDecimal amount
    ) {}
}
