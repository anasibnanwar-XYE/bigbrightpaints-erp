package com.bigbrightpaints.erp.orchestrator;

import com.bigbrightpaints.erp.modules.accounting.domain.Account;
import com.bigbrightpaints.erp.modules.accounting.domain.AccountRepository;
import com.bigbrightpaints.erp.modules.accounting.domain.AccountType;
import com.bigbrightpaints.erp.modules.company.domain.Company;
import com.bigbrightpaints.erp.modules.company.domain.CompanyRepository;
import com.bigbrightpaints.erp.modules.hr.domain.PayrollRunRepository;
import com.bigbrightpaints.erp.modules.sales.domain.SalesOrder;
import com.bigbrightpaints.erp.orchestrator.repository.OutboxEvent;
import com.bigbrightpaints.erp.orchestrator.repository.OutboxEventRepository;
import com.bigbrightpaints.erp.test.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class OrchestratorControllerIT extends AbstractIntegrationTest {

    private static final String COMPANY_CODE = "ACME";
    private static final String ORCH_EMAIL = "orch@bbp.com";
    private static final String ORCH_PASSWORD = "orch123";
    private static final String ADMIN_EMAIL = "orch-admin@bbp.com";
    private static final String ADMIN_PASSWORD = "Admin123!";

    @Autowired
    private TestRestTemplate rest;
    @Autowired
    private OutboxEventRepository outboxEventRepository;
    @Autowired
    private PayrollRunRepository payrollRunRepository;
    @Autowired
    private CompanyRepository companyRepository;
    @Autowired
    private AccountRepository accountRepository;

    private Long seededOrderId;
    private Long payrollCashAccountId;
    private Long payrollExpenseAccountId;

    @BeforeEach
    void seed() {
        dataSeeder.ensureUser(
                ORCH_EMAIL,
                ORCH_PASSWORD,
                "Orchestrator",
                COMPANY_CODE,
                List.of("ROLE_SALES", "orders.approve", "ROLE_FACTORY", "factory.dispatch", "ROLE_ACCOUNTING", "payroll.run")
        );
        dataSeeder.ensureUser(
                ADMIN_EMAIL,
                ADMIN_PASSWORD,
                "Orchestrator Admin",
                COMPANY_CODE,
                List.of("ROLE_ADMIN")
        );
        SalesOrder order = dataSeeder.ensureSalesOrder(COMPANY_CODE, "SO-" + System.nanoTime(), new BigDecimal("5000"));
        seededOrderId = order.getId();
        ensurePayrollAccounts();
    }

    private void ensurePayrollAccounts() {
        Company company = companyRepository.findByCodeIgnoreCase(COMPANY_CODE).orElseThrow();
        payrollCashAccountId = ensureAccount(company, "CASH-PAYROLL", "Payroll Cash", AccountType.ASSET).getId();
        payrollExpenseAccountId = ensureAccount(company, "EXP-PAYROLL", "Payroll Expense", AccountType.EXPENSE).getId();
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

    @Test
    void approve_order_creates_outbox_event() {
        String token = loginToken();
        HttpHeaders headers = authHeaders(token);
        long before = outboxEventRepository.count();

        Map<String, Object> body = Map.of(
                "orderId", String.valueOf(seededOrderId),
                "approvedBy", "orch@bbp.com",
                "totalAmount", new BigDecimal("5000")
        );

        ResponseEntity<Map> approveResponse = rest.exchange(
                "/api/v1/orchestrator/orders/" + seededOrderId + "/approve",
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                Map.class);

        assertThat(approveResponse.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(approveResponse.getBody()).containsKey("traceId");
        assertThat(outboxEventRepository.count()).isEqualTo(before + 1);
        long pending = outboxEventRepository.countByStatusAndDeadLetterFalse(OutboxEvent.Status.PENDING);
        long retrying = outboxEventRepository
                .countByStatusAndDeadLetterFalseAndRetryCountGreaterThan(OutboxEvent.Status.PENDING, 0);
        long deadLetters = outboxEventRepository.countByStatusAndDeadLetterTrue(OutboxEvent.Status.FAILED);
        System.out.println("M6 API evidence outbox counts: pending=" + pending
                + " retrying=" + retrying + " deadLetters=" + deadLetters);
    }

    @Test
    void payroll_run_creates_payroll_entry() {
        String token = loginToken();
        HttpHeaders headers = authHeaders(token);
        long beforeRuns = payrollRunRepository.count();

        Map<String, Object> body = Map.of(
                "payrollDate", LocalDate.now(),
                "initiatedBy", "orch@bbp.com",
                "debitAccountId", payrollExpenseAccountId,
                "creditAccountId", payrollCashAccountId,
                "postingAmount", new BigDecimal("5000")
        );

        ResponseEntity<Map> response = rest.exchange(
                "/api/v1/orchestrator/payroll/run",
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(response.getBody()).containsKey("traceId");
        assertThat(payrollRunRepository.count()).isGreaterThan(beforeRuns);
    }

    @Test
    void admin_health_endpoints_return_snapshot() {
        String token = loginToken(ADMIN_EMAIL, ADMIN_PASSWORD);
        HttpHeaders headers = authHeaders(token);

        ResponseEntity<Map> eventHealth = rest.exchange(
                "/api/v1/orchestrator/health/events",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class);
        System.out.println("M6 API evidence orchestrator health events: status=" + eventHealth.getStatusCode()
                + " body=" + eventHealth.getBody());
        assertThat(eventHealth.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(eventHealth.getBody()).isNotNull();
        assertThat(eventHealth.getBody()).containsKeys("pendingEvents", "retryingEvents", "deadLetters");

        ResponseEntity<Map> integrationsHealth = rest.exchange(
                "/api/v1/orchestrator/health/integrations",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class);
        System.out.println("M6 API evidence orchestrator health integrations: status=" + integrationsHealth.getStatusCode()
                + " body=" + integrationsHealth.getBody());
        assertThat(integrationsHealth.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(integrationsHealth.getBody()).isNotNull();
        assertThat(integrationsHealth.getBody()).containsKeys("orders", "plans", "accounts", "employees");
    }

    @Test
    void health_endpoints_require_admin() {
        String token = loginToken();
        HttpHeaders headers = authHeaders(token);

        ResponseEntity<Map> eventHealth = rest.exchange(
                "/api/v1/orchestrator/health/events",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class);
        System.out.println("M6 API evidence orchestrator health events forbidden: status=" + eventHealth.getStatusCode()
                + " body=" + eventHealth.getBody());
        assertThat(eventHealth.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        ResponseEntity<Map> integrationsHealth = rest.exchange(
                "/api/v1/orchestrator/health/integrations",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class);
        System.out.println("M6 API evidence orchestrator health integrations forbidden: status="
                + integrationsHealth.getStatusCode() + " body=" + integrationsHealth.getBody());
        assertThat(integrationsHealth.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void trace_endpoint_requires_admin() {
        String token = loginToken();
        HttpHeaders headers = authHeaders(token);

        ResponseEntity<Map> response = rest.exchange(
                "/api/v1/orchestrator/traces/trace-denied",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class);
        System.out.println("M6 API evidence orchestrator trace forbidden: status=" + response.getStatusCode()
                + " body=" + response.getBody());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void admin_trace_endpoint_returns_events() {
        String token = loginToken(ADMIN_EMAIL, ADMIN_PASSWORD);
        HttpHeaders headers = authHeaders(token);

        Map<String, Object> body = Map.of(
                "orderId", String.valueOf(seededOrderId),
                "approvedBy", ADMIN_EMAIL,
                "totalAmount", new BigDecimal("5000")
        );

        ResponseEntity<Map> approveResponse = rest.exchange(
                "/api/v1/orchestrator/orders/" + seededOrderId + "/approve",
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                Map.class);

        assertThat(approveResponse.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(approveResponse.getBody()).containsKey("traceId");
        String traceId = approveResponse.getBody().get("traceId").toString();

        ResponseEntity<Map> traceResponse = rest.exchange(
                "/api/v1/orchestrator/traces/" + traceId,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class);
        System.out.println("M6 API evidence orchestrator trace: status=" + traceResponse.getStatusCode()
                + " body=" + traceResponse.getBody());
        assertThat(traceResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(traceResponse.getBody()).containsKey("events");
    }

    private String loginToken() {
        return loginToken(ORCH_EMAIL, ORCH_PASSWORD);
    }

    private String loginToken(String email, String password) {
        Map<String, Object> req = Map.of(
                "email", email,
                "password", password,
                "companyCode", COMPANY_CODE
        );
        return (String) rest.postForEntity("/api/v1/auth/login", req, Map.class).getBody().get("accessToken");
    }

    private HttpHeaders authHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("X-Company-Id", COMPANY_CODE);
        return headers;
    }
}
