package com.bigbrightpaints.erp.modules.accounting.service;

import com.bigbrightpaints.erp.modules.accounting.domain.JournalEntry;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

abstract class AbstractPartnerLedgerService<P, Entry> {

    public record LedgerContext(LocalDate entryDate,
                                String referenceNumber,
                                String memo,
                                BigDecimal debit,
                                BigDecimal credit,
                                JournalEntry journalEntry) {
    }

    protected void recordLedgerEntry(P partner, LedgerContext context) {
        Objects.requireNonNull(partner, "Partner is required for ledger entry");
        Objects.requireNonNull(context, "Ledger context is required");

        BigDecimal debit = normalize(context.debit());
        BigDecimal credit = normalize(context.credit());
        if (debit.compareTo(BigDecimal.ZERO) == 0 && credit.compareTo(BigDecimal.ZERO) == 0) {
            return;
        }

        Entry entry = createEntry();
        populateEntry(entry, partner, context, debit, credit);
        persistEntry(entry);

        P managedPartner = reloadPartner(partner);
        BigDecimal aggregate = aggregateBalance(managedPartner);
        updateOutstandingBalance(managedPartner, aggregate);
    }

    private BigDecimal normalize(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }

    protected abstract Entry createEntry();

    protected abstract void persistEntry(Entry entry);

    protected abstract P reloadPartner(P partner);

    protected abstract BigDecimal aggregateBalance(P partner);

    protected abstract void updateOutstandingBalance(P partner, BigDecimal balance);

    protected abstract void populateEntry(Entry entry,
                                          P partner,
                                          LedgerContext context,
                                          BigDecimal debit,
                                          BigDecimal credit);
}
