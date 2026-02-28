package com.bigbrightpaints.erp.modules.accounting.service;

import com.bigbrightpaints.erp.modules.accounting.domain.Account;
import com.bigbrightpaints.erp.modules.accounting.domain.AccountRepository;
import com.bigbrightpaints.erp.modules.accounting.domain.AccountType;
import com.bigbrightpaints.erp.modules.accounting.domain.DealerLedgerRepository;
import com.bigbrightpaints.erp.modules.accounting.domain.JournalEntry;
import com.bigbrightpaints.erp.modules.accounting.domain.JournalEntryRepository;
import com.bigbrightpaints.erp.modules.accounting.domain.JournalLine;
import com.bigbrightpaints.erp.modules.accounting.domain.JournalLineRepository;
import com.bigbrightpaints.erp.modules.accounting.domain.SupplierLedgerRepository;
import com.bigbrightpaints.erp.modules.accounting.dto.BankReconciliationRequest;
import com.bigbrightpaints.erp.modules.accounting.dto.DealerBalanceView;
import com.bigbrightpaints.erp.modules.accounting.dto.SupplierBalanceView;
import com.bigbrightpaints.erp.modules.company.domain.Company;
import com.bigbrightpaints.erp.modules.company.service.CompanyContextService;
import com.bigbrightpaints.erp.modules.inventory.domain.InventoryReservationRepository;
import com.bigbrightpaints.erp.modules.inventory.domain.PackagingSlipRepository;
import com.bigbrightpaints.erp.modules.purchasing.domain.Supplier;
import com.bigbrightpaints.erp.modules.purchasing.domain.SupplierRepository;
import com.bigbrightpaints.erp.modules.sales.domain.Dealer;
import com.bigbrightpaints.erp.modules.sales.domain.SalesOrderRepository;
import com.bigbrightpaints.erp.modules.sales.domain.DealerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReconciliationServiceTest {

    @Mock private CompanyContextService companyContextService;
    @Mock private AccountRepository accountRepository;
    @Mock private DealerRepository dealerRepository;
    @Mock private DealerLedgerRepository dealerLedgerRepository;
    @Mock private SupplierRepository supplierRepository;
    @Mock private SupplierLedgerRepository supplierLedgerRepository;
    @Mock private InventoryReservationRepository inventoryReservationRepository;
    @Mock private PackagingSlipRepository packagingSlipRepository;
    @Mock private SalesOrderRepository salesOrderRepository;
    @Mock private JournalEntryRepository journalEntryRepository;
    @Mock private JournalLineRepository journalLineRepository;

    private ReconciliationService reconciliationService;
    private Company company;

    @BeforeEach
    void setUp() {
        reconciliationService = new ReconciliationService(
                companyContextService,
                accountRepository,
                dealerRepository,
                dealerLedgerRepository,
                supplierRepository,
                supplierLedgerRepository,
                inventoryReservationRepository,
                packagingSlipRepository,
                salesOrderRepository,
                journalEntryRepository,
                journalLineRepository
        );
        company = new Company();
        company.setCode("ACME");
        when(companyContextService.requireCurrentCompany()).thenReturn(company);
    }

    @Test
    void reconcileArWithDealerLedger_reportsDiscrepanciesPerDealer() {
        Account receivable = new Account();
        ReflectionTestUtils.setField(receivable, "id", 10L);
        receivable.setType(AccountType.ASSET);
        receivable.setCode("AR-CONTROL");
        receivable.setBalance(new BigDecimal("260.00"));

        Dealer firstDealer = new Dealer();
        ReflectionTestUtils.setField(firstDealer, "id", 1L);
        firstDealer.setCode("D-1");
        firstDealer.setName("Dealer One");
        firstDealer.setOutstandingBalance(new BigDecimal("120.00"));
        firstDealer.setReceivableAccount(receivable);

        Dealer secondDealer = new Dealer();
        ReflectionTestUtils.setField(secondDealer, "id", 2L);
        secondDealer.setCode("D-2");
        secondDealer.setName("Dealer Two");
        secondDealer.setOutstandingBalance(new BigDecimal("140.00"));
        secondDealer.setReceivableAccount(receivable);

        when(accountRepository.findByCompanyOrderByCodeAsc(company)).thenReturn(List.of(receivable));
        when(dealerRepository.findByCompanyOrderByNameAsc(company)).thenReturn(List.of(firstDealer, secondDealer));
        when(dealerLedgerRepository.aggregateBalances(company, List.of(1L, 2L))).thenReturn(List.of(
                new DealerBalanceView(1L, new BigDecimal("100.00")),
                new DealerBalanceView(2L, new BigDecimal("170.00"))
        ));

        ReconciliationService.ReconciliationResult result = reconciliationService.reconcileArWithDealerLedger();

        assertThat(result.discrepancies()).hasSize(2);
        assertThat(result.discrepancies().get(0).dealerId()).isEqualTo(1L);
        assertThat(result.discrepancies().get(0).variance()).isEqualByComparingTo("20.00");
        assertThat(result.discrepancies().get(1).dealerId()).isEqualTo(2L);
        assertThat(result.discrepancies().get(1).variance()).isEqualByComparingTo("-30.00");
    }

    @Test
    void reconcileBankAccount_matchesClearedReferencesAndReportsUnclearedVariance() {
        Account bank = new Account();
        ReflectionTestUtils.setField(bank, "id", 99L);
        bank.setCode("BANK-MAIN");
        bank.setName("Main Bank");
        bank.setType(AccountType.ASSET);
        bank.setBalance(new BigDecimal("1000.00"));

        when(accountRepository.findByCompanyAndId(company, 99L)).thenReturn(Optional.of(bank));

        JournalEntry depositEntry = new JournalEntry();
        ReflectionTestUtils.setField(depositEntry, "id", 501L);
        depositEntry.setReferenceNumber("DEP-1");
        depositEntry.setEntryDate(LocalDate.of(2026, 2, 5));
        depositEntry.setMemo("Deposit in transit");

        JournalLine depositLine = new JournalLine();
        depositLine.setJournalEntry(depositEntry);
        depositLine.setDebit(new BigDecimal("300.00"));
        depositLine.setCredit(BigDecimal.ZERO);

        JournalEntry checkEntry = new JournalEntry();
        ReflectionTestUtils.setField(checkEntry, "id", 502L);
        checkEntry.setReferenceNumber("CHK-1");
        checkEntry.setEntryDate(LocalDate.of(2026, 2, 6));
        checkEntry.setMemo("Outstanding cheque");

        JournalLine checkLine = new JournalLine();
        checkLine.setJournalEntry(checkEntry);
        checkLine.setDebit(BigDecimal.ZERO);
        checkLine.setCredit(new BigDecimal("200.00"));

        JournalEntry clearedEntry = new JournalEntry();
        ReflectionTestUtils.setField(clearedEntry, "id", 503L);
        clearedEntry.setReferenceNumber("CLR-1");
        clearedEntry.setEntryDate(LocalDate.of(2026, 2, 7));
        clearedEntry.setMemo("Cleared movement");

        JournalLine clearedLine = new JournalLine();
        clearedLine.setJournalEntry(clearedEntry);
        clearedLine.setDebit(new BigDecimal("50.00"));
        clearedLine.setCredit(BigDecimal.ZERO);

        when(journalLineRepository.findLinesForAccountBetween(
                company,
                99L,
                LocalDate.of(2026, 2, 1),
                LocalDate.of(2026, 2, 28)
        )).thenReturn(List.of(depositLine, checkLine, clearedLine));

        BankReconciliationRequest request = new BankReconciliationRequest(
                99L,
                LocalDate.of(2026, 2, 28),
                new BigDecimal("900.00"),
                LocalDate.of(2026, 2, 1),
                LocalDate.of(2026, 2, 28),
                List.of("CLR-1"),
                null,
                null,
                null
        );

        var result = reconciliationService.reconcileBankAccount(request);

        assertThat(result.outstandingDeposits()).isEqualByComparingTo("300.00");
        assertThat(result.outstandingChecks()).isEqualByComparingTo("200.00");
        assertThat(result.difference()).isEqualByComparingTo("0.00");
        assertThat(result.balanced()).isTrue();
        assertThat(result.unclearedDeposits()).hasSize(1);
        assertThat(result.unclearedChecks()).hasSize(1);
    }

    @Test
    void reconcileSubledgerBalances_includesCombinedVarianceSummary() {
        Account receivable = new Account();
        ReflectionTestUtils.setField(receivable, "id", 11L);
        receivable.setType(AccountType.ASSET);
        receivable.setCode("AR-CONTROL");
        receivable.setBalance(new BigDecimal("500.00"));

        Account payable = new Account();
        ReflectionTestUtils.setField(payable, "id", 12L);
        payable.setType(AccountType.LIABILITY);
        payable.setCode("AP-CONTROL");
        payable.setBalance(new BigDecimal("-300.00"));

        Dealer dealer = new Dealer();
        ReflectionTestUtils.setField(dealer, "id", 1L);
        dealer.setCode("D-1");
        dealer.setName("Dealer");
        dealer.setOutstandingBalance(new BigDecimal("450.00"));
        dealer.setReceivableAccount(receivable);

        Supplier supplier = new Supplier();
        ReflectionTestUtils.setField(supplier, "id", 2L);
        supplier.setCode("S-1");
        supplier.setName("Supplier");
        supplier.setOutstandingBalance(new BigDecimal("250.00"));
        supplier.setPayableAccount(payable);

        when(accountRepository.findByCompanyOrderByCodeAsc(company)).thenReturn(List.of(receivable, payable));
        when(dealerRepository.findByCompanyOrderByNameAsc(company)).thenReturn(List.of(dealer));
        when(supplierRepository.findByCompanyOrderByNameAsc(company)).thenReturn(List.of(supplier));
        when(dealerLedgerRepository.aggregateBalances(company, List.of(1L)))
                .thenReturn(List.of(new DealerBalanceView(1L, new BigDecimal("430.00"))));
        when(supplierLedgerRepository.aggregateBalances(company, List.of(2L)))
                .thenReturn(List.of(new SupplierBalanceView(2L, new BigDecimal("260.00"))));

        ReconciliationService.SubledgerReconciliationReport report = reconciliationService.reconcileSubledgerBalances();

        assertThat(report.dealerReconciliation().variance()).isEqualByComparingTo("70.00");
        assertThat(report.supplierReconciliation().variance()).isEqualByComparingTo("40.00");
        assertThat(report.combinedVariance()).isEqualByComparingTo("110.00");
        assertThat(report.reconciled()).isFalse();
    }
}
