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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Regression: Catalog -> FinishedGood invariants")
class ProductionCatalogFinishedGoodInvariantIT extends AbstractIntegrationTest {

    private static final String COMPANY_CODE_PREFIX = "LF-015";

    @Autowired private CompanyRepository companyRepository;
    @Autowired private AccountRepository accountRepository;
    @Autowired private ProductionCatalogService productionCatalogService;
    @Autowired private FinishedGoodRepository finishedGoodRepository;

    private final String companyCode = COMPANY_CODE_PREFIX + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    private Company company;
    private Account inventoryAccount;
    private Account cogsAccount;
    private Account revenueAccount;
    private Account taxAccount;

    @BeforeEach
    void setUp() {
        company = dataSeeder.ensureCompany(companyCode, companyCode + " Ltd");
        CompanyContextHolder.setCompanyId(companyCode);

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
        String token = uniqueToken();
        String productName = "LF-015 Product " + token;
        String skuCode = "FG-LF015-" + token;
        ProductCreateRequest request = new ProductCreateRequest(
                null,
                "LF-015 Brand",
                null,
                productName,
                "FINISHED_GOOD",
                "WHITE",
                "1L",
                "UNIT",
                skuCode,
                new BigDecimal("100.00"),
                new BigDecimal("18.00"),
                null,
                null,
                null
        );

        ProductionProductDto product = productionCatalogService.createProduct(request);

        FinishedGood fg = finishedGoodRepository.findByCompanyAndProductCode(company, skuCode)
                .orElseThrow();

        assertThat(fg.getName()).isEqualTo(productName);
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
        String token = uniqueToken();
        String sourceName = "LF-015 Sync Product " + token;
        String updatedName = "LF-015 Sync Product Renamed " + token;
        String skuCode = "FG-LF015-SYNC-" + token;
        ProductionProductDto created = productionCatalogService.createProduct(new ProductCreateRequest(
                null,
                "LF-015 Brand",
                null,
                sourceName,
                "FINISHED_GOOD",
                "BLUE",
                "1L",
                "UNIT",
                skuCode,
                new BigDecimal("90.00"),
                new BigDecimal("18.00"),
                null,
                null,
                null
        ));

        productionCatalogService.updateProduct(created.id(), new ProductUpdateRequest(
                updatedName,
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

        FinishedGood fg = finishedGoodRepository.findByCompanyAndProductCode(company, skuCode)
                .orElseThrow();

        assertThat(fg.getName()).isEqualTo(updatedName);
        assertThat(fg.getUnit()).isEqualTo("LITER");
    }

    @Test
    void createProductRejectsReservedSemiFinishedSuffix() {
        String token = uniqueToken();
        ProductCreateRequest request = new ProductCreateRequest(
                null,
                "LF-015 Brand",
                null,
                "LF-015 Bulk Collision Product " + token,
                "FINISHED_GOOD",
                "WHITE",
                "1L",
                "UNIT",
                "FG-LF015-" + token + "-BULK",
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

    private String uniqueToken() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
