package com.bigbrightpaints.erp.modules.accounting.service;

import com.bigbrightpaints.erp.core.util.CompanyEntityLookup;
import com.bigbrightpaints.erp.modules.accounting.domain.DealerLedgerEntry;
import com.bigbrightpaints.erp.modules.accounting.domain.DealerLedgerRepository;
import com.bigbrightpaints.erp.modules.accounting.dto.DealerBalanceView;
import com.bigbrightpaints.erp.modules.company.domain.Company;
import com.bigbrightpaints.erp.modules.company.service.CompanyContextService;
import com.bigbrightpaints.erp.modules.sales.domain.Dealer;
import com.bigbrightpaints.erp.modules.sales.domain.DealerRepository;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class DealerLedgerService extends AbstractPartnerLedgerService<Dealer, DealerLedgerEntry> {

    private final DealerLedgerRepository dealerLedgerRepository;
    private final CompanyContextService companyContextService;
    private final DealerRepository dealerRepository;
    private final CompanyEntityLookup companyEntityLookup;

    public DealerLedgerService(DealerLedgerRepository dealerLedgerRepository,
                               CompanyContextService companyContextService,
                               DealerRepository dealerRepository,
                               CompanyEntityLookup companyEntityLookup) {
        this.dealerLedgerRepository = dealerLedgerRepository;
        this.companyContextService = companyContextService;
        this.dealerRepository = dealerRepository;
        this.companyEntityLookup = companyEntityLookup;
    }

    @Transactional
    public void recordLedgerEntry(Dealer dealer, LedgerContext context) {
        super.recordLedgerEntry(dealer, context);
    }

    public Map<Long, BigDecimal> currentBalances(Collection<Long> dealerIds) {
        if (dealerIds == null || dealerIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Company company = companyContextService.requireCurrentCompany();
        List<DealerBalanceView> aggregates = dealerLedgerRepository.aggregateBalances(company, dealerIds);
        Map<Long, BigDecimal> balanceMap = new HashMap<>();
        for (DealerBalanceView view : aggregates) {
            balanceMap.put(view.dealerId(), view.balance());
        }
        return balanceMap;
    }

    public BigDecimal currentBalance(Long dealerId) {
        if (dealerId == null) {
            return BigDecimal.ZERO;
        }
        Company company = companyContextService.requireCurrentCompany();
        Dealer dealer = companyEntityLookup.requireDealer(company, dealerId);
        return dealerLedgerRepository.aggregateBalance(company, dealer)
                .map(DealerBalanceView::balance)
                .orElse(BigDecimal.ZERO);
    }

    @Override
    protected DealerLedgerEntry createEntry() {
        return new DealerLedgerEntry();
    }

    @Override
    protected void persistEntry(DealerLedgerEntry entry) {
        dealerLedgerRepository.save(entry);
    }

    @Override
    protected Dealer reloadPartner(Dealer partner) {
        return dealerRepository.lockByCompanyAndId(partner.getCompany(), partner.getId())
                .orElse(partner);
    }

    @Override
    protected BigDecimal aggregateBalance(Dealer partner) {
        return dealerLedgerRepository.aggregateBalance(partner.getCompany(), partner)
                .map(DealerBalanceView::balance)
                .orElse(BigDecimal.ZERO);
    }

    @Override
    protected void updateOutstandingBalance(Dealer partner, BigDecimal balance) {
        BigDecimal nonNegative = balance != null && balance.compareTo(BigDecimal.ZERO) > 0 ? balance : BigDecimal.ZERO;
        partner.setOutstandingBalance(nonNegative);
        dealerRepository.save(partner);
    }

    public List<DealerLedgerEntry> entries(Dealer dealer) {
        Company company = companyContextService.requireCurrentCompany();
        return dealerLedgerRepository.findByCompanyAndDealerOrderByEntryDateAsc(company, dealer);
    }

    @Override
    protected void populateEntry(DealerLedgerEntry entry,
                                 Dealer partner,
                                 LedgerContext context,
                                 BigDecimal debit,
                                 BigDecimal credit) {
        entry.setCompany(partner.getCompany());
        entry.setDealer(partner);
        entry.setEntryDate(context.entryDate());
        entry.setReferenceNumber(context.referenceNumber());
        entry.setMemo(context.memo());
        entry.setJournalEntry(context.journalEntry());
        entry.setDebit(debit);
        entry.setCredit(credit);
    }
}
