package com.bigbrightpaints.erp.modules.accounting.service;

import com.bigbrightpaints.erp.modules.accounting.domain.AccountRepository;
import com.bigbrightpaints.erp.modules.accounting.domain.AccountingPeriodRepository;
import com.bigbrightpaints.erp.modules.accounting.domain.DealerLedgerRepository;
import com.bigbrightpaints.erp.modules.accounting.domain.JournalEntryRepository;
import com.bigbrightpaints.erp.modules.accounting.domain.JournalLineRepository;
import com.bigbrightpaints.erp.modules.accounting.domain.ReconciliationDiscrepancyRepository;
import com.bigbrightpaints.erp.modules.accounting.domain.SupplierLedgerRepository;
import com.bigbrightpaints.erp.modules.company.domain.CompanyRepository;
import com.bigbrightpaints.erp.modules.company.service.CompanyContextService;
import com.bigbrightpaints.erp.modules.purchasing.domain.SupplierRepository;
import com.bigbrightpaints.erp.modules.reports.service.ReportService;
import com.bigbrightpaints.erp.modules.sales.domain.DealerRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

@Service
public class ReconciliationService extends ReconciliationServiceCore {

    public ReconciliationService(CompanyContextService companyContextService,
                                 CompanyRepository companyRepository,
                                 AccountRepository accountRepository,
                                 DealerRepository dealerRepository,
                                 DealerLedgerRepository dealerLedgerRepository,
                                 SupplierRepository supplierRepository,
                                 SupplierLedgerRepository supplierLedgerRepository,
                                 JournalEntryRepository journalEntryRepository,
                                 JournalLineRepository journalLineRepository,
                                 TemporalBalanceService temporalBalanceService,
                                 ReconciliationDiscrepancyRepository reconciliationDiscrepancyRepository,
                                 AccountingPeriodRepository accountingPeriodRepository,
                                 TaxService taxService,
                                 ReportService reportService,
                                 ObjectProvider<AccountingFacade> accountingFacadeProvider) {
        super(companyContextService,
                companyRepository,
                accountRepository,
                dealerRepository,
                dealerLedgerRepository,
                supplierRepository,
                supplierLedgerRepository,
                journalEntryRepository,
                journalLineRepository,
                temporalBalanceService,
                reconciliationDiscrepancyRepository,
                accountingPeriodRepository,
                taxService,
                reportService,
                accountingFacadeProvider);
    }
}
