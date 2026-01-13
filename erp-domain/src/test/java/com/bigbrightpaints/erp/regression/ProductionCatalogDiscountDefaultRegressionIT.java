package com.bigbrightpaints.erp.regression;

import com.bigbrightpaints.erp.modules.accounting.domain.Account;
import com.bigbrightpaints.erp.modules.accounting.domain.AccountRepository;
import com.bigbrightpaints.erp.modules.accounting.domain.AccountType;
import com.bigbrightpaints.erp.modules.company.domain.Company;
import com.bigbrightpaints.erp.modules.company.domain.CompanyRepository;
import com.bigbrightpaints.erp.test.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Regression: Product creation should tolerate null discount default")
class ProductionCatalogDiscountDefaultRegressionIT extends AbstractIntegrationTest {

    private static final String COMPANY_CODE = "LF-014";
    private static final String ADMIN_EMAIL = "lf014@erp.test";
    private static final String ADMIN_PASSWORD = "lf014";

    @Autowired private TestRestTemplate rest;
    @Autowired private CompanyRepository companyRepository;
    @Autowired private AccountRepository accountRepository;

    private HttpHeaders headers;

    @BeforeEach
    void setUp() {
        dataSeeder.ensureUser(ADMIN_EMAIL, ADMIN_PASSWORD, "LF-014 Admin", COMPANY_CODE,
                List.of("ROLE_ADMIN", "ROLE_ACCOUNTING"));

        Company company = companyRepository.findByCodeIgnoreCase(COMPANY_CODE).orElseThrow();
        Account inventory = ensureAccount(company, "INV-FG", "FG Inventory", AccountType.ASSET);
        Account cogs = ensureAccount(company, "COGS", "Cost of Goods Sold", AccountType.COGS);
        Account revenue = ensureAccount(company, "REV", "Revenue", AccountType.REVENUE);
        Account tax = ensureAccount(company, "TAX", "Tax Payable", AccountType.LIABILITY);

        company.setDefaultInventoryAccountId(inventory.getId());
        company.setDefaultCogsAccountId(cogs.getId());
        company.setDefaultRevenueAccountId(revenue.getId());
        company.setDefaultDiscountAccountId(null);
        company.setDefaultTaxAccountId(tax.getId());
        companyRepository.save(company);

        headers = createHeaders(login());
    }

    @Test
    void createFinishedGoodWithNullDiscountDefaultDoesNotError() {
        String brandName = "LF-014 Brand " + UUID.randomUUID();
        Map<String, Object> request = Map.of(
                "brandName", brandName,
                "productName", "LF-014 Product " + UUID.randomUUID(),
                "category", "FINISHED_GOOD",
                "unitOfMeasure", "UNIT",
                "basePrice", new BigDecimal("100.00"),
                "gstRate", new BigDecimal("18.00")
        );

        ResponseEntity<Map> response = rest.exchange(
                "/api/v1/accounting/catalog/products",
                HttpMethod.POST,
                new HttpEntity<>(request, headers),
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("success", true);
    }

    private Account ensureAccount(Company company, String code, String name, AccountType type) {
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

    private String login() {
        Map<String, Object> request = Map.of(
                "email", ADMIN_EMAIL,
                "password", ADMIN_PASSWORD,
                "companyCode", COMPANY_CODE
        );
        ResponseEntity<Map> response = rest.postForEntity("/api/v1/auth/login", request, Map.class);
        return response.getBody().get("accessToken").toString();
    }

    private HttpHeaders createHeaders(String token) {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setBearerAuth(token);
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        httpHeaders.set("X-Company-Id", COMPANY_CODE);
        return httpHeaders;
    }
}
