package com.bigbrightpaints.erp.performance;

import com.bigbrightpaints.erp.core.security.CompanyContextHolder;
import com.bigbrightpaints.erp.modules.accounting.domain.Account;
import com.bigbrightpaints.erp.modules.accounting.domain.AccountRepository;
import com.bigbrightpaints.erp.modules.accounting.domain.AccountType;
import com.bigbrightpaints.erp.modules.company.domain.Company;
import com.bigbrightpaints.erp.modules.sales.domain.Dealer;
import com.bigbrightpaints.erp.modules.sales.domain.DealerRepository;
import com.bigbrightpaints.erp.modules.sales.domain.SalesOrder;
import com.bigbrightpaints.erp.modules.sales.domain.SalesOrderItem;
import com.bigbrightpaints.erp.modules.sales.domain.SalesOrderRepository;
import com.bigbrightpaints.erp.modules.sales.dto.SalesOrderDto;
import com.bigbrightpaints.erp.modules.sales.service.SalesService;
import com.bigbrightpaints.erp.test.AbstractIntegrationTest;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@TestPropertySource(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
class PerformanceBudgetIT extends AbstractIntegrationTest {

    private static final String COMPANY_CODE = "PERF-BUDGET";
    private static final String ADMIN_EMAIL = "perf-admin@bbp.com";
    private static final String ADMIN_PASSWORD = "admin123";
    private static final Duration REPORT_BUDGET = Duration.ofSeconds(3);

    @Autowired
    private SalesService salesService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private SalesOrderRepository salesOrderRepository;

    @Autowired
    private DealerRepository dealerRepository;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private TestRestTemplate rest;

    private Company company;

    @BeforeEach
    void seedFixtures() {
        dataSeeder.ensureUser(ADMIN_EMAIL, ADMIN_PASSWORD, "Perf Admin", COMPANY_CODE, List.of("ROLE_ADMIN"));
        company = dataSeeder.ensureCompany(COMPANY_CODE, "Perf Budget Co");
    }

    @AfterEach
    void clearCompanyContext() {
        CompanyContextHolder.clear();
    }

    @Test
    void salesOrderListQueryCountIsBounded() {
        Dealer dealer = dealerRepository.findByCompanyAndCodeIgnoreCase(company, "FIX-DEALER")
                .orElseThrow(() -> new IllegalStateException("Fixture dealer missing for " + COMPANY_CODE));
        seedOrders(company, dealer, 5);

        CompanyContextHolder.setCompanyId(COMPANY_CODE);

        Statistics stats = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        stats.setStatisticsEnabled(true);
        stats.clear();

        List<SalesOrderDto> orders = salesService.listOrders(null, 0, 25);

        assertThat(orders).hasSizeGreaterThanOrEqualTo(5);
        assertThat(stats.getPrepareStatementCount()).isLessThanOrEqualTo(6L);
    }

    @Test
    void balanceSheetReportCompletesWithinBudget() {
        ResponseEntity<Map> response = timedGet(
                "/api/v1/accounting/reports/balance-sheet/hierarchy",
                authHeaders(),
                REPORT_BUDGET);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    void trialBalanceReportCompletesWithinBudget() {
        ResponseEntity<Map> response = timedGet(
                "/api/v1/reports/trial-balance",
                authHeaders(),
                REPORT_BUDGET);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    void dealerStatementCompletesWithinBudget() {
        Dealer dealer = dealerRepository.findByCompanyAndCodeIgnoreCase(company, "FIX-DEALER")
                .orElseThrow(() -> new IllegalStateException("Fixture dealer missing for " + COMPANY_CODE));
        ResponseEntity<Map> response = timedGet(
                "/api/v1/accounting/statements/dealers/" + dealer.getId(),
                authHeaders(),
                REPORT_BUDGET);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    void accountStatementReportCompletesWithinBudget() {
        ResponseEntity<Map> response = timedGet(
                "/api/v1/reports/account-statement",
                authHeaders(),
                REPORT_BUDGET);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    void inventoryReconciliationCompletesWithinBudget() {
        ResponseEntity<Map> response = timedGet(
                "/api/v1/reports/inventory-reconciliation",
                authHeaders(),
                REPORT_BUDGET);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    void reconciliationDashboardCompletesWithinBudget() {
        Account bankAccount = accountRepository.findByCompanyAndCodeIgnoreCase(company, "CASH")
                .orElseGet(() -> {
                    Account account = new Account();
                    account.setCompany(company);
                    account.setCode("CASH");
                    account.setName("Cash");
                    account.setType(AccountType.ASSET);
                    return accountRepository.save(account);
                });
        String url = String.format("/api/v1/reports/reconciliation-dashboard?bankAccountId=%d",
                bankAccount.getId());
        ResponseEntity<Map> response = timedGet(url, authHeaders(), REPORT_BUDGET);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
    }

    private String loginToken() {
        Map<String, Object> req = Map.of(
                "email", ADMIN_EMAIL,
                "password", ADMIN_PASSWORD,
                "companyCode", COMPANY_CODE
        );
        ResponseEntity<Map> response = rest.postForEntity("/api/v1/auth/login", req, Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> body = response.getBody();
        assertThat(body).isNotNull();
        return body.get("accessToken").toString();
    }

    private HttpHeaders authHeaders() {
        String token = loginToken();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Company-Id", COMPANY_CODE);
        return headers;
    }

    private ResponseEntity<Map> timedGet(String url, HttpHeaders headers, Duration budget) {
        long start = System.nanoTime();
        ResponseEntity<Map> response = rest.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class);
        Duration elapsed = Duration.ofNanos(System.nanoTime() - start);
        assertThat(elapsed).isLessThanOrEqualTo(budget);
        return response;
    }

    private void seedOrders(Company company, Dealer dealer, int count) {
        for (int i = 0; i < count; i++) {
            SalesOrder order = new SalesOrder();
            order.setCompany(company);
            order.setDealer(dealer);
            order.setOrderNumber("SO-Q-" + i + "-" + System.nanoTime());
            order.setStatus("BOOKED");
            order.setTotalAmount(new BigDecimal("100.00"));
            order.setSubtotalAmount(new BigDecimal("100.00"));
            order.setGstTotal(BigDecimal.ZERO);
            order.setCurrency("INR");

            SalesOrderItem item = new SalesOrderItem();
            item.setSalesOrder(order);
            item.setProductCode("FG-FIXTURE");
            item.setDescription("Fixture item");
            item.setQuantity(BigDecimal.ONE);
            item.setUnitPrice(new BigDecimal("100.00"));
            item.setLineSubtotal(new BigDecimal("100.00"));
            item.setLineTotal(new BigDecimal("100.00"));
            item.setGstRate(BigDecimal.ZERO);
            item.setGstAmount(BigDecimal.ZERO);
            order.getItems().add(item);

            salesOrderRepository.save(order);
        }
    }
}
