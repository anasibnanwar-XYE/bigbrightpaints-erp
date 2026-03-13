package com.bigbrightpaints.erp.truthsuite.o2c;

import com.bigbrightpaints.erp.core.security.CompanyContextHolder;
import com.bigbrightpaints.erp.modules.accounting.domain.Account;
import com.bigbrightpaints.erp.modules.accounting.domain.AccountRepository;
import com.bigbrightpaints.erp.modules.accounting.domain.AccountType;
import com.bigbrightpaints.erp.modules.accounting.domain.JournalEntry;
import com.bigbrightpaints.erp.modules.accounting.domain.JournalEntryRepository;
import com.bigbrightpaints.erp.modules.company.domain.Company;
import com.bigbrightpaints.erp.modules.company.domain.CompanyRepository;
import com.bigbrightpaints.erp.modules.inventory.domain.FinishedGood;
import com.bigbrightpaints.erp.modules.inventory.domain.FinishedGoodRepository;
import com.bigbrightpaints.erp.modules.inventory.domain.PackagingSlip;
import com.bigbrightpaints.erp.modules.inventory.domain.PackagingSlipRepository;
import com.bigbrightpaints.erp.modules.inventory.dto.FinishedGoodBatchRequest;
import com.bigbrightpaints.erp.modules.inventory.dto.FinishedGoodRequest;
import com.bigbrightpaints.erp.modules.inventory.service.FinishedGoodsService;
import com.bigbrightpaints.erp.modules.invoice.domain.Invoice;
import com.bigbrightpaints.erp.modules.invoice.domain.InvoiceRepository;
import com.bigbrightpaints.erp.modules.production.domain.ProductionBrand;
import com.bigbrightpaints.erp.modules.production.domain.ProductionBrandRepository;
import com.bigbrightpaints.erp.modules.production.domain.ProductionProduct;
import com.bigbrightpaints.erp.modules.production.domain.ProductionProductRepository;
import com.bigbrightpaints.erp.modules.sales.domain.Dealer;
import com.bigbrightpaints.erp.modules.sales.domain.DealerRepository;
import com.bigbrightpaints.erp.modules.sales.domain.SalesOrder;
import com.bigbrightpaints.erp.modules.sales.domain.SalesOrderRepository;
import com.bigbrightpaints.erp.modules.sales.dto.DispatchConfirmRequest;
import com.bigbrightpaints.erp.modules.sales.dto.DispatchConfirmResponse;
import com.bigbrightpaints.erp.modules.sales.dto.SalesOrderItemRequest;
import com.bigbrightpaints.erp.modules.sales.dto.SalesOrderRequest;
import com.bigbrightpaints.erp.modules.sales.service.SalesService;
import com.bigbrightpaints.erp.orchestrator.service.IntegrationCoordinator;
import com.bigbrightpaints.erp.test.AbstractIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("critical")
@Tag("reconciliation")
@TestPropertySource(properties = {
        "erp.auto-approval.enabled=false",
        "orchestrator.factory-dispatch.enabled=true"
})
// Temporary characterization — removal target for remove-noncanonical-dispatch-path feature
class TS_O2COrchestratorDispatchCharacterizationTest extends AbstractIntegrationTest {

    @Autowired private IntegrationCoordinator integrationCoordinator;
    @Autowired private CompanyRepository companyRepository;
    @Autowired private AccountRepository accountRepository;
    @Autowired private DealerRepository dealerRepository;
    @Autowired private ProductionBrandRepository productionBrandRepository;
    @Autowired private ProductionProductRepository productionProductRepository;
    @Autowired private FinishedGoodsService finishedGoodsService;
    @Autowired private FinishedGoodRepository finishedGoodRepository;
    @Autowired private SalesService salesService;
    @Autowired private SalesOrderRepository salesOrderRepository;
    @Autowired private PackagingSlipRepository packagingSlipRepository;
    @Autowired private InvoiceRepository invoiceRepository;
    @Autowired private JournalEntryRepository journalEntryRepository;

    @AfterEach
    void clearCompanyContext() {
        CompanyContextHolder.clear();
    }

    @Test
    void postDispatchJournal_createsDispatchPrefixedJournalOutsideCanonicalSlipInvoiceChain() {
        DispatchFixture fixture = bootstrapDispatchFixture("TS-ORCH-DISPATCH");
        DispatchConfirmResponse canonicalDispatch = salesService.confirmDispatch(fixture.salesRequest());
        PackagingSlip canonicalSlip = packagingSlipRepository.findByIdAndCompany(fixture.slip().getId(), fixture.company()).orElseThrow();
        Invoice canonicalInvoice = invoiceRepository.findById(canonicalDispatch.finalInvoiceId()).orElseThrow();
        String batchId = "LEGACY-" + shortId();

        integrationCoordinator.postDispatchJournal(batchId, fixture.company().getCode(), new BigDecimal("77.00"));

        JournalEntry orchestratorJournal = journalEntryRepository
                .findByCompanyAndReferenceNumber(fixture.company(), "DISPATCH-" + batchId)
                .orElseThrow();

        assertThat(orchestratorJournal.getReferenceNumber()).isEqualTo("DISPATCH-" + batchId);
        assertThat(orchestratorJournal.getSourceReference()).isEqualTo("DISPATCH-" + batchId);
        assertThat(orchestratorJournal.getSourceModule()).isEqualTo("ORCHESTRATOR");
        assertThat(orchestratorJournal.getId())
                .isNotEqualTo(canonicalSlip.getJournalEntryId())
                .isNotEqualTo(canonicalSlip.getCogsJournalEntryId())
                .isNotEqualTo(canonicalInvoice.getJournalEntry().getId());
        assertThat(invoiceRepository.findByCompanyAndJournalEntry(fixture.company(), orchestratorJournal)).isEmpty();
        assertThat(packagingSlipRepository.findByCompanyOrderByCreatedAtDesc(fixture.company()))
                .allSatisfy(slip -> {
                    assertThat(slip.getJournalEntryId()).isNotEqualTo(orchestratorJournal.getId());
                    assertThat(slip.getCogsJournalEntryId()).isNotEqualTo(orchestratorJournal.getId());
                });
        assertThat(journalEntryRepository.findByCompanyAndReferenceNumberStartingWith(fixture.company(), "DISPATCH-"))
                .extracting(JournalEntry::getId)
                .containsExactly(orchestratorJournal.getId());
    }

    private DispatchFixture bootstrapDispatchFixture(String prefix) {
        String companyCode = prefix + "-" + shortId();
        Company company = bootstrapCompany(companyCode, "UTC");
        Map<String, Account> accounts = ensureCoreAccounts(company);
        Dealer dealer = ensureDealer(company, accounts.get("AR"));
        FinishedGood finishedGood = ensureFinishedGoodWithCatalog(company, accounts, "FG-" + shortId(), BigDecimal.ZERO);
        finishedGoodsService.registerBatch(new FinishedGoodBatchRequest(
                finishedGood.getId(), "BATCH-" + shortId(), new BigDecimal("50"), new BigDecimal("10.00"), Instant.now(), null));

        SalesOrder order = createOrder(company, dealer, finishedGood.getProductCode(), new BigDecimal("10"), new BigDecimal("12.34"));
        finishedGoodsService.reserveForOrder(order);
        PackagingSlip slip = packagingSlipRepository.findByCompanyAndSalesOrderId(company, order.getId()).orElseThrow();

        DispatchConfirmRequest salesRequest = new DispatchConfirmRequest(
                slip.getId(),
                order.getId(),
                slip.getLines().stream()
                        .map(line -> new DispatchConfirmRequest.DispatchLine(
                                line.getId(),
                                null,
                                line.getOrderedQuantity() != null ? line.getOrderedQuantity() : line.getQuantity(),
                                null,
                                null,
                                null,
                                null,
                                null))
                        .toList(),
                "truthsuite orchestrator dispatch characterization",
                "truthsuite-user",
                false,
                null,
                null
        );

        return new DispatchFixture(company, order, slip, salesRequest);
    }

    private Company bootstrapCompany(String companyCode, String timezone) {
        dataSeeder.ensureCompany(companyCode, companyCode + " Ltd");
        CompanyContextHolder.setCompanyId(companyCode);
        Company company = companyRepository.findByCodeIgnoreCase(companyCode).orElseThrow();
        company.setTimezone(timezone);
        company.setBaseCurrency("INR");
        companyRepository.save(company);
        return company;
    }

    private Map<String, Account> ensureCoreAccounts(Company company) {
        Account ar = ensureAccount(company, "AR", "Accounts Receivable", AccountType.ASSET);
        Account inv = ensureAccount(company, "INV", "Inventory", AccountType.ASSET);
        Account cogs = ensureAccount(company, "COGS", "COGS", AccountType.COGS);
        Account rev = ensureAccount(company, "REV", "Revenue", AccountType.REVENUE);
        Account disc = ensureAccount(company, "DISC", "Discounts", AccountType.EXPENSE);
        Account gstOut = ensureAccount(company, "GST-OUT", "GST Output", AccountType.LIABILITY);
        Account gstIn = ensureAccount(company, "GST-IN", "GST Input", AccountType.ASSET);

        updateCompanyDefaults(company, inv, cogs, rev, disc, gstOut, gstIn);

        return Map.of(
                "AR", ar,
                "INV", inv,
                "COGS", cogs,
                "REV", rev,
                "DISC", disc,
                "GST_OUT", gstOut,
                "GST_IN", gstIn
        );
    }

    private void updateCompanyDefaults(Company company,
                                       Account inv,
                                       Account cogs,
                                       Account rev,
                                       Account disc,
                                       Account gstOut,
                                       Account gstIn) {
        if (company == null || company.getId() == null) {
            return;
        }
        for (int attempt = 0; attempt < 2; attempt++) {
            Company fresh = companyRepository.findById(company.getId()).orElseThrow();
            boolean updated = false;
            if (fresh.getDefaultInventoryAccountId() == null) {
                fresh.setDefaultInventoryAccountId(inv.getId());
                updated = true;
            }
            if (fresh.getDefaultCogsAccountId() == null) {
                fresh.setDefaultCogsAccountId(cogs.getId());
                updated = true;
            }
            if (fresh.getDefaultRevenueAccountId() == null) {
                fresh.setDefaultRevenueAccountId(rev.getId());
                updated = true;
            }
            if (fresh.getDefaultDiscountAccountId() == null) {
                fresh.setDefaultDiscountAccountId(disc.getId());
                updated = true;
            }
            if (fresh.getDefaultTaxAccountId() == null) {
                fresh.setDefaultTaxAccountId(gstOut.getId());
                updated = true;
            }
            if (fresh.getGstInputTaxAccountId() == null) {
                fresh.setGstInputTaxAccountId(gstIn.getId());
                updated = true;
            }
            if (fresh.getGstOutputTaxAccountId() == null) {
                fresh.setGstOutputTaxAccountId(gstOut.getId());
                updated = true;
            }
            if (fresh.getGstPayableAccountId() == null) {
                fresh.setGstPayableAccountId(gstOut.getId());
                updated = true;
            }
            if (!updated) {
                return;
            }
            try {
                companyRepository.save(fresh);
                return;
            } catch (ObjectOptimisticLockingFailureException ex) {
                if (attempt == 1) {
                    throw ex;
                }
            }
        }
    }

    private Account ensureAccount(Company company, String code, String name, AccountType type) {
        return accountRepository.findByCompanyAndCodeIgnoreCase(company, code)
                .orElseGet(() -> {
                    Account account = new Account();
                    account.setCompany(company);
                    account.setCode(code);
                    account.setName(name);
                    account.setType(type);
                    account.setActive(true);
                    account.setBalance(BigDecimal.ZERO);
                    return accountRepository.save(account);
                });
    }

    private Dealer ensureDealer(Company company, Account arAccount) {
        return dealerRepository.findByCompanyAndCodeIgnoreCase(company, "TS-DEALER")
                .orElseGet(() -> {
                    Dealer dealer = new Dealer();
                    dealer.setCompany(company);
                    dealer.setCode("TS-DEALER");
                    dealer.setName("Truthsuite Dealer");
                    dealer.setStatus("ACTIVE");
                    dealer.setReceivableAccount(arAccount);
                    return dealerRepository.save(dealer);
                });
    }

    private FinishedGood ensureFinishedGoodWithCatalog(Company company,
                                                       Map<String, Account> accounts,
                                                       String sku,
                                                       BigDecimal gstRate) {
        FinishedGoodRequest request = new FinishedGoodRequest(
                sku,
                sku + " Name",
                "UNIT",
                "FIFO",
                accounts.get("INV").getId(),
                accounts.get("COGS").getId(),
                accounts.get("REV").getId(),
                accounts.get("DISC").getId(),
                accounts.get("GST_OUT").getId()
        );
        FinishedGood finishedGood = finishedGoodRepository.findByCompanyAndProductCode(company, sku)
                .orElseGet(() -> {
                    var dto = finishedGoodsService.createFinishedGood(request);
                    return finishedGoodRepository.findById(dto.id()).orElseThrow();
                });
        ensureCatalogProduct(company, finishedGood, gstRate);
        return finishedGood;
    }

    private void ensureCatalogProduct(Company company, FinishedGood finishedGood, BigDecimal gstRate) {
        ProductionBrand brand = productionBrandRepository.findByCompanyAndCodeIgnoreCase(company, "TS-BRAND")
                .orElseGet(() -> {
                    ProductionBrand created = new ProductionBrand();
                    created.setCompany(company);
                    created.setCode("TS-BRAND");
                    created.setName("Truthsuite Brand");
                    return productionBrandRepository.save(created);
                });
        productionProductRepository.findByCompanyAndSkuCode(company, finishedGood.getProductCode())
                .orElseGet(() -> {
                    ProductionProduct product = new ProductionProduct();
                    product.setCompany(company);
                    product.setBrand(brand);
                    product.setSkuCode(finishedGood.getProductCode());
                    product.setProductName(finishedGood.getName());
                    product.setBasePrice(new BigDecimal("10.00"));
                    product.setCategory("GENERAL");
                    product.setSizeLabel("STD");
                    product.setDefaultColour("NA");
                    product.setMinDiscountPercent(BigDecimal.ZERO);
                    product.setMinSellingPrice(BigDecimal.ZERO);
                    product.setMetadata(new java.util.HashMap<>());
                    product.setGstRate(gstRate);
                    product.setUnitOfMeasure("UNIT");
                    product.setActive(true);
                    return productionProductRepository.save(product);
                });
    }

    private SalesOrder createOrder(Company company,
                                   Dealer dealer,
                                   String productCode,
                                   BigDecimal quantity,
                                   BigDecimal unitPrice) {
        CompanyContextHolder.setCompanyId(company.getCode());
        BigDecimal totalAmount = unitPrice.multiply(quantity).setScale(2, RoundingMode.HALF_UP);
        var orderDto = salesService.createOrder(new SalesOrderRequest(
                dealer.getId(),
                totalAmount,
                "INR",
                "truthsuite orchestrator dispatch characterization",
                List.of(new SalesOrderItemRequest(productCode, "Truthsuite Item", quantity, unitPrice, BigDecimal.ZERO)),
                "EXCLUSIVE",
                null,
                null,
                UUID.randomUUID().toString()
        ));
        return salesOrderRepository.findById(orderDto.id()).orElseThrow();
    }

    private static String shortId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private record DispatchFixture(
            Company company,
            SalesOrder order,
            PackagingSlip slip,
            DispatchConfirmRequest salesRequest
    ) {}
}
