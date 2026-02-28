package com.bigbrightpaints.erp.modules.accounting.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ManualJournalRequest(
        LocalDate entryDate,
        String narration,
        String idempotencyKey,
        Boolean adminOverride,
        List<LineRequest> lines
) {
    public record LineRequest(Long accountId,
                              BigDecimal amount,
                              String narration,
                              EntryType entryType) {
    }

    public enum EntryType {
        DEBIT,
        CREDIT
    }
}
