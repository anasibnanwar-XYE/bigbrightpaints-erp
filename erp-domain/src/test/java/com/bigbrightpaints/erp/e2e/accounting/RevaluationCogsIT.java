package com.bigbrightpaints.erp.e2e.accounting;

import com.bigbrightpaints.erp.core.security.CompanyContextHolder;
import com.bigbrightpaints.erp.modules.accounting.domain.Account;
import com.bigbrightpaints.erp.modules.accounting.domain.AccountRepository;
import com.bigbrightpaints.erp.modules.accounting.domain.AccountType;
import com.bigbrightpaints.erp.modules.accounting.dto.InventoryRevaluationRequest;
import com.bigbrightpaints.erp.modules.accounting.service.AccountingFacade;
import com.bigbrightpaints.erp.modules.accounting.service.AccountingService;
import com.bigbrightpaints.erp.modules.company.domain.Company;
import com.bigbrightpaints.erp.modules.company.domain.CompanyRepository;
import com.bigbrightpaints.erp.modules.inventory.domain.FinishedGood;
import com.bigbrightpaints.erp.modules.inventory.domain.FinishedGoodBatch;
import com.bigbrightpaints.erp.modules.inventory.domain.FinishedGoodBatchRepository;
import com.bigbrightpaints.erp.modules.inventory.domain.FinishedGoodRepository;
import com.bigbrightpaints.erp.modules.production.domain.ProductionBrand;
import com.bigbrightpaints.erp.modules.production.domain.ProductionBrandRepository;
import com.bigbrightpaints.erp.modules.production.domain.ProductionProduct;
import com.bigbrightpaints.erp.modules.production.domain.ProductionProductRepository;
import com.bigbrightpaints.erp.modules.sales.domain.Dealer;
import com.bigbrightpaints.erp.modules.sales.domain.DealerRepository;
import com.bigbrightpaints.erp.modules.sales.domain.SalesOrder;
import com.bigbrightpaints.erp.modules.sales.domain.SalesOrderRepository;
import com.bigbrightpaints.erp.modules.sales.dto.SalesOrderItemRequest;
import com.bigbrightpaints.erp.modules.sales.dto.SalesOrderRequest;
import com.bigbrightpaints.erp.modules.sales.service.SalesService;
import com.bigbrightpaints.erp.test.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Inventory revaluation adjusts batch cost and COGS after sale")
public class RevaluationCogsIT extends AbstractIntegrationTest {

    private static final String COMPANY_CODE = "REVALCOGS";
    private static final String ADMIN_EMAIL = "reval@bbp.com";
    private static final String ADMIN_PASSWORD = "reval123";

    @Autowired private CompanyRepository companyRepository;
    @Autowired private AccountRepository accountRepository;
    @Autowired private FinishedGoodRepository finishedGoodRepository;
    @Autowired private FinishedGoodBatchRepository finishedGoodBatchRepository;
    @Autowired private ProductionBrandRepository productionBrandRepository;
    @Autowired private ProductionProductRepository productionProductRepository;
    @Autowired private DealerRepository dealerRepository;
    @Autowired private SalesOrderRepository salesOrderRepository;
    @Autowired private AccountingService accountingService;
    @Autowired private AccountingFacade accountingFacade;
    @Autowired private SalesService salesService;

    private Company company;
    private Account inventory;
    private Account cogs;
    private Account revenue;
    private Account reval;
    private Account ar;
    private ProductionBrand brand;
    private FinishedGood fg;

    @BeforeEach
    void setup() {
        dataSeeder.ensureUser(ADMIN_EMAIL, ADMIN_PASSWORD, "Reval Admin", COMPANY_CODE,
                List.of("ROLE_ADMIN", "ROLE_ACCOUNTING", "ROLE_SALES"));
        CompanyContextHolder.setCompanyId(COMPANY_CODE);
        company = companyRepository.findByCodeIgnoreCase(COMPANY_CODE).orElseThrow();
        brand = productionBrandRepository.findByCompanyAndCodeIgnoreCase(company, "BR-REV")
                .orElseGet(() -> {
                    ProductionBrand b = new ProductionBrand();
                    b.setCompany(company);
                    b.setCode("BR-REV");
                    b.setName("Reval Brand");
                    return productionBrandRepository.save(b);
                });
        inventory = ensureAccount("FG-INV-R", "FG Inventory", AccountType.ASSET);
        cogs = ensureAccount("COGS-R", "COGS", AccountType.EXPENSE);
        revenue = ensureAccount("REV-R", "Revenue", AccountType.REVENUE);
        reval = ensureAccount("REVAL-R", "Revaluation", AccountType.EQUITY);
        ar = ensureAccount("AR-R", "Accounts Receivable", AccountType.ASSET);
        fg = ensureFinishedGood("FG-REV", inventory.getId(), cogs.getId(), revenue.getId());
        ensureProductionProduct(fg);
        seedBatch(fg, new BigDecimal("10"), new BigDecimal("100"));
    }

    @Test
    void revaluation_updates_batch_cost_and_cogs() {
        // Revalue +20 total => +2 per unit
        accountingService.revalueInventory(new InventoryRevaluationRequest(
                inventory.getId(),
                reval.getId(),
                new BigDecimal("20.00"),
                "Reval up",
                LocalDate.now(),
                "REVAL-TEST",
                null,
                true
        ));
        FinishedGoodBatch batch = finishedGoodBatchRepository.findAll().get(0);
        assertThat(batch.getUnitCost()).isEqualByComparingTo("102.000000");

        Dealer dealer = ensureDealer();
        List<SalesOrderItemRequest> items = List.of(
                new SalesOrderItemRequest(fg.getProductCode(), "FG", new BigDecimal("2"), new BigDecimal("150"), null)
        );
        SalesOrderRequest order = new SalesOrderRequest(
                dealer.getId(),
                new BigDecimal("300"),
                "INR",
                null,
                items,
                null,
                null,
                null,
                null
        );
        var savedOrder = salesService.createOrder(order);
        SalesOrder orderEntity = salesOrderRepository.findByCompanyAndId(company, savedOrder.id()).orElseThrow();
        BigDecimal cost = batch.getUnitCost().multiply(new BigDecimal("2"));
        accountingFacade.postCOGS(orderEntity.getOrderNumber(), cogs.getId(), inventory.getId(), cost, "COGS for " + orderEntity.getOrderNumber());

        Account cogsAfter = accountRepository.findByCompanyAndCodeIgnoreCase(company, cogs.getCode()).orElseThrow();
        assertThat(cogsAfter.getBalance()).isEqualByComparingTo(new BigDecimal("204.00"));
    }

    private Account ensureAccount(String code, String name, AccountType type) {
        return accountRepository.findByCompanyAndCodeIgnoreCase(company, code)
                .orElseGet(() -> {
                    Account a = new Account();
                    a.setCompany(company);
                    a.setCode(code);
                    a.setName(name);
                    a.setType(type);
                    a.setBalance(BigDecimal.ZERO);
                    return accountRepository.save(a);
                });
    }

    private FinishedGood ensureFinishedGood(String code, Long invId, Long cogsId, Long revId) {
        return finishedGoodRepository.findByCompanyAndProductCode(company, code)
                .orElseGet(() -> {
                    FinishedGood f = new FinishedGood();
                    f.setCompany(company);
                    f.setName(code);
                    f.setProductCode(code);
                    f.setCurrentStock(BigDecimal.ZERO);
                    f.setReservedStock(BigDecimal.ZERO);
                    f.setValuationAccountId(invId);
                    f.setCogsAccountId(cogsId);
                    f.setRevenueAccountId(revId);
                    f.setTaxAccountId(revId);
                    return finishedGoodRepository.save(f);
                });
    }

    private void ensureProductionProduct(FinishedGood good) {
        productionProductRepository.findByCompanyAndSkuCode(company, good.getProductCode())
                .orElseGet(() -> {
                    ProductionProduct p = new ProductionProduct();
                    p.setCompany(company);
                    p.setBrand(brand);
                    p.setProductName(good.getName());
                    p.setSkuCode(good.getProductCode());
                    p.setCategory("FINISHED_GOOD");
                    p.setUnitOfMeasure("UNIT");
                    return productionProductRepository.save(p);
                });
    }

    private void seedBatch(FinishedGood fg, BigDecimal qty, BigDecimal unitCost) {
        FinishedGoodBatch batch = new FinishedGoodBatch();
        batch.setFinishedGood(fg);
        batch.setBatchCode("B-" + UUID.randomUUID());
        batch.setQuantityTotal(qty);
        batch.setQuantityAvailable(qty);
        batch.setUnitCost(unitCost);
        batch.setManufacturedAt(Instant.now());
        finishedGoodBatchRepository.save(batch);
        fg.setCurrentStock(fg.getCurrentStock().add(qty));
        finishedGoodRepository.save(fg);
    }

    private Dealer ensureDealer() {
        return dealerRepository.findByCompanyAndCodeIgnoreCase(company, "REV-DEAL")
                .orElseGet(() -> {
                    Dealer d = new Dealer();
                    d.setCompany(company);
                    d.setCode("REV-DEAL");
                    d.setName("Reval Dealer");
                    d.setReceivableAccount(ar);
                    return dealerRepository.save(d);
                });
    }
}
