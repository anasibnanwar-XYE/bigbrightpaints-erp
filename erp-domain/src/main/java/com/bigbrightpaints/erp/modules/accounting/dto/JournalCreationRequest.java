package com.bigbrightpaints.erp.modules.accounting.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public record JournalCreationRequest(
        BigDecimal amount,
        Long debitAccount,
        Long creditAccount,
        String narration,
        String sourceModule,
        String sourceReference,
        GstBreakdown gstBreakdown,
        List<LineRequest> lines,
        LocalDate entryDate,
        Long dealerId,
        Long supplierId,
        Boolean adminOverride
) {

    public List<JournalEntryRequest.JournalLineRequest> resolvedLines() {
        if (lines != null && !lines.isEmpty()) {
            return lines.stream()
                    .map(line -> new JournalEntryRequest.JournalLineRequest(
                            line.accountId(),
                            line.narration(),
                            line.debit(),
                            line.credit()
                    ))
                    .toList();
        }
        List<JournalEntryRequest.JournalLineRequest> generated = new ArrayList<>();
        generated.add(new JournalEntryRequest.JournalLineRequest(
                debitAccount,
                narration,
                amount,
                BigDecimal.ZERO
        ));
        generated.add(new JournalEntryRequest.JournalLineRequest(
                creditAccount,
                narration,
                BigDecimal.ZERO,
                amount
        ));
        return generated;
    }

    public record LineRequest(Long accountId,
                              BigDecimal debit,
                              BigDecimal credit,
                              String narration) {
    }

    public record GstBreakdown(BigDecimal taxableAmount,
                               BigDecimal cgst,
                               BigDecimal sgst,
                               BigDecimal igst) {
    }
}
