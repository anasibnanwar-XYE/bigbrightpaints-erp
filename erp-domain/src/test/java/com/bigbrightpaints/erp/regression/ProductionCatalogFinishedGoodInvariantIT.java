package com.bigbrightpaints.erp.regression;

import com.bigbrightpaints.erp.core.security.CompanyContextHolder;
import com.bigbrightpaints.erp.modules.accounting.domain.Account;
import com.bigbrightpaints.erp.modules.accounting.domain.AccountRepository;
import com.bigbrightpaints.erp.modules.accounting.domain.AccountType;
import com.bigbrightpaints.erp.modules.company.domain.Company;
import com.bigbrightpaints.erp.modules.company.domain.CompanyRepository;
import com.bigbrightpaints.erp.modules.inventory.domain.FinishedGood;
import com.bigbrightpaints.erp.modules.inventory.domain.FinishedGoodRepository;
import com.bigbrightpaints.erp.modules.production.dto.ProductCreateRequest;
import com.bigbrightpaints.erp.modules.production.dto.ProductUpdateRequest;
import com.bigbrightpaints.erp.modules.production.dto.ProductionProductDto;
import com.bigbrightpaints.erp.modules.production.service.ProductionCatalogService;
import com.bigbrightpaints.erp.test.AbstractIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Regression: Catalog -> FinishedGood invariants")
class ProductionCatalogFinishedGoodInvariantIT extends AbstractIntegrationTest {

    private static final String COMPANY_CODE = "LF-015";

    @Autowired private CompanyRepository companyRepository;
    @Autowired private AccountRepository accountRepository;
    @Autowired private ProductionCatalogService productionCatalogService;
    @Autowired private FinishedGoodRepository finishedGoodRepository;

    private Company company;
    private Account inventoryAccount;
    private Account cogsAccount;
    private Account revenueAccount;
    private Account taxAccount;

    @BeforeEach
    void setUp() {
        company = dataSeeder.ensureCompany(COMPANY_CODE, COMPANY_CODE + " Ltd");
        CompanyContextHolder.setCompanyId(COMPANY_CODE);

        inventoryAccount = ensureAccount("INV", "Inventory", AccountType.ASSET);
        cogsAccount = ensureAccount("COGS", "COGS", AccountType.COGS);
        revenueAccount = ensureAccount("REV", "Revenue", AccountType.REVENUE);
        taxAccount = ensureAccount("GST-OUT", "GST Output", AccountType.LIABILITY);

        company.setDefaultInventoryAccountId(inventoryAccount.getId());
        company.setDefaultCogsAccountId(cogsAccount.getId());
        company.setDefaultRevenueAccountId(revenueAccount.getId());
        company.setDefaultTaxAccountId(taxAccount.getId());
        companyRepository.save(company);
    }

    @AfterEach
    void clearContext() {
        CompanyContextHolder.clear();
    }

    @Test
    void createProductAutoProvisionsFinishedGood() {
        ProductCreateRequest request = new ProductCreateRequest(
                null,
                "LF-015 Brand",
                null,
                "LF-015 Product",
                "FINISHED_GOOD",
                "WHITE",
                "1L",
                "UNIT",
                "FG-LF015-001",
                new BigDecimal("100.00"),
                new BigDecimal("18.00"),
                null,
                null,
                null
        );

        ProductionProductDto product = productionCatalogService.createProduct(request);

        FinishedGood fg = finishedGoodRepository.findByCompanyAndProductCode(company, product.skuCode())
                .orElseThrow();

        assertThat(fg.getName()).isEqualTo(product.productName());
        assertThat(fg.getUnit()).isEqualTo("UNIT");
        assertThat(fg.getValuationAccountId()).isEqualTo(inventoryAccount.getId());
        assertThat(fg.getCogsAccountId()).isEqualTo(cogsAccount.getId());
        assertThat(fg.getRevenueAccountId()).isEqualTo(revenueAccount.getId());
        assertThat(fg.getTaxAccountId()).isEqualTo(taxAccount.getId());
        assertThat(fg.getCurrentStock()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(fg.getReservedStock()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void updateProductSynchronizesFinishedGoodNameAndUnit() {
        ProductionProductDto created = productionCatalogService.createProduct(new ProductCreateRequest(
                null,
                "LF-015 Brand",
                null,
                "LF-015 Sync Product",
                "FINISHED_GOOD",
                "BLUE",
                "1L",
                "UNIT",
                "FG-LF015-002",
                new BigDecimal("90.00"),
                new BigDecimal("18.00"),
                null,
                null,
                null
        ));

        productionCatalogService.updateProduct(created.id(), new ProductUpdateRequest(
                "LF-015 Sync Product Renamed",
                null,
                null,
                null,
                "LITER",
                null,
                null,
                null,
                null,
                null
        ));

        FinishedGood fg = finishedGoodRepository.findByCompanyAndProductCode(company, created.skuCode())
                .orElseThrow();

        assertThat(fg.getName()).isEqualTo("LF-015 Sync Product Renamed");
        assertThat(fg.getUnit()).isEqualTo("LITER");
    }

    @Test
    void createProductRejectsReservedSemiFinishedSuffix() {
        ProductCreateRequest request = new ProductCreateRequest(
                null,
                "LF-015 Brand",
                null,
                "LF-015 Bulk Collision Product",
                "FINISHED_GOOD",
                "WHITE",
                "1L",
                "UNIT",
                "FG-LF015-BULK",
                new BigDecimal("100.00"),
                new BigDecimal("18.00"),
                null,
                null,
                null
        );

        assertThatThrownBy(() -> productionCatalogService.createProduct(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("reserved");
    }

    private Account ensureAccount(String code, String name, AccountType type) {
        return accountRepository.findByCompanyAndCodeIgnoreCase(company, code)
                .orElseGet(() -> {
                    Account account = new Account();
                    account.setCompany(company);
                    account.setCode(code);
                    account.setName(name);
                    account.setType(type);
                    return accountRepository.save(account);
                });
    }
}
