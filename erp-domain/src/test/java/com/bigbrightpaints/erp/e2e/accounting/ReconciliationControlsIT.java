package com.bigbrightpaints.erp.e2e.accounting;

import com.bigbrightpaints.erp.core.security.CompanyContextHolder;
import com.bigbrightpaints.erp.modules.accounting.domain.Account;
import com.bigbrightpaints.erp.modules.accounting.domain.AccountRepository;
import com.bigbrightpaints.erp.modules.accounting.domain.AccountType;
import com.bigbrightpaints.erp.modules.accounting.dto.AccountRequest;
import com.bigbrightpaints.erp.modules.accounting.dto.JournalEntryRequest;
import com.bigbrightpaints.erp.modules.accounting.service.AccountingService;
import com.bigbrightpaints.erp.modules.accounting.service.ReconciliationService;
import com.bigbrightpaints.erp.modules.company.domain.Company;
import com.bigbrightpaints.erp.modules.company.domain.CompanyRepository;
import com.bigbrightpaints.erp.modules.reports.dto.InventoryValuationDto;
import com.bigbrightpaints.erp.modules.reports.dto.ReconciliationSummaryDto;
import com.bigbrightpaints.erp.modules.reports.service.ReportService;
import com.bigbrightpaints.erp.test.AbstractIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class ReconciliationControlsIT extends AbstractIntegrationTest {

    private static final String COMPANY_CODE = "RECON-E2E";

    @Autowired
    private ReportService reportService;

    @Autowired
    private ReconciliationService reconciliationService;

    @Autowired
    private AccountingService accountingService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @BeforeEach
    void setUp() {
        CompanyContextHolder.setCompanyId(COMPANY_CODE);
        dataSeeder.ensureCompany(COMPANY_CODE, "Reconciliation Co");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        CompanyContextHolder.clear();
    }

    @Test
    void inventoryReconciliationReturnsZeroVarianceAfterBalancingLedger() {
        Company company = companyRepository.findByCodeIgnoreCase(COMPANY_CODE).orElseThrow();
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken("tester", "n/a"));

        InventoryValuationDto valuation = reportService.inventoryValuation();
        BigDecimal totalValue = valuation.totalValue() == null ? BigDecimal.ZERO : valuation.totalValue();

        if (totalValue.compareTo(BigDecimal.ZERO) > 0) {
            Account inventory = accountRepository.findByCompanyAndCodeIgnoreCase(company, "INV").orElseThrow();
            Account offset = accountRepository.findByCompanyAndCodeIgnoreCase(company, "OPEN-BAL")
                    .orElseGet(() -> accountRepository.findById(
                                    accountingService.createAccount(new AccountRequest(
                                            "OPEN-BAL", "Opening Balance", AccountType.EQUITY)).id())
                            .orElseThrow());

            accountingService.createJournalEntry(new JournalEntryRequest(
                    "OPEN-INV-" + COMPANY_CODE + "-" + System.nanoTime(),
                    LocalDate.now(),
                    "Seed inventory balance",
                    null,
                    null,
                    false,
                    List.of(
                            new JournalEntryRequest.JournalLineRequest(
                                    inventory.getId(), "Opening inventory", totalValue, BigDecimal.ZERO),
                            new JournalEntryRequest.JournalLineRequest(
                                    offset.getId(), "Offset", BigDecimal.ZERO, totalValue)
                    )
            ));
        }

        ReconciliationSummaryDto summary = reportService.inventoryReconciliation();
        assertThat(summary.variance()).as("inventory variance").isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(summary.physicalInventoryValue())
                .as("inventory valuation matches ledger")
                .isEqualByComparingTo(summary.ledgerInventoryBalance());
    }

    @Test
    void arAndApReconciliationsReturnZeroVarianceForSeededCompany() {
        ReconciliationService.ReconciliationResult ar = reconciliationService.reconcileArWithDealerLedger();
        assertThat(ar.isReconciled()).isTrue();
        assertThat(ar.variance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(ar.arAccountCount()).isPositive();
        assertThat(ar.dealerCount()).isPositive();

        ReconciliationService.SupplierReconciliationResult ap = reconciliationService.reconcileApWithSupplierLedger();
        assertThat(ap.isReconciled()).isTrue();
        assertThat(ap.variance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(ap.apAccountCount()).isPositive();
        assertThat(ap.supplierCount()).isPositive();
    }
}
