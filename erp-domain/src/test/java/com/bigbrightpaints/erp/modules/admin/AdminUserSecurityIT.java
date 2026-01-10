package com.bigbrightpaints.erp.modules.admin;

import com.bigbrightpaints.erp.modules.auth.domain.UserAccount;
import com.bigbrightpaints.erp.modules.company.domain.Company;
import com.bigbrightpaints.erp.modules.sales.domain.Dealer;
import com.bigbrightpaints.erp.modules.sales.domain.DealerRepository;
import com.bigbrightpaints.erp.test.AbstractIntegrationTest;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class AdminUserSecurityIT extends AbstractIntegrationTest {

    private static final String COMPANY = "SECADMIN";
    private static final String OTHER_COMPANY = "SECADMIN2";
    private static final String ADMIN_EMAIL = "admin-sec@bbp.com";
    private static final String ADMIN_PASSWORD = "Admin123!";
    private static final String DEALER_EMAIL = "dealer-sec@bbp.com";
    private static final String DEALER_PASSWORD = "Dealer123!";

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private DealerRepository dealerRepository;

    private UserAccount otherCompanyUser;
    private Dealer portalDealer;
    private Dealer sameCompanyOtherDealer;
    private Dealer otherCompanyDealer;

    @BeforeEach
    void setUp() {
        Company company = dataSeeder.ensureCompany(COMPANY, COMPANY + " Ltd");
        Company otherCompany = dataSeeder.ensureCompany(OTHER_COMPANY, OTHER_COMPANY + " Ltd");
        dataSeeder.ensureUser(ADMIN_EMAIL, ADMIN_PASSWORD, "Security Admin", COMPANY,
                List.of("ROLE_ADMIN"));
        UserAccount dealerUser = dataSeeder.ensureUser(DEALER_EMAIL, DEALER_PASSWORD, "Security Dealer", COMPANY,
                List.of("ROLE_DEALER"));
        otherCompanyUser = dataSeeder.ensureUser("other-admin@bbp.com", "Other123!", "Other Admin", OTHER_COMPANY,
                List.of("ROLE_ADMIN"));
        portalDealer = ensureDealer(company, "SEC-PORTAL", "Security Portal Dealer", dealerUser);
        sameCompanyOtherDealer = ensureDealer(company, "SEC-ALT", "Security Other Dealer", null);
        otherCompanyDealer = ensureDealer(otherCompany, "SEC-XCO", "Security X-Company Dealer", null);
    }

    @Test
    void admin_users_requires_authentication() {
        ResponseEntity<Map> response = rest.exchange(
                "/api/v1/admin/users",
                HttpMethod.GET,
                new HttpEntity<>(new HttpHeaders()),
                Map.class);
        System.out.println("M5 API evidence admin users unauth: status=" + response.getStatusCode()
                + " body=" + response.getBody());
        assertThat(response.getStatusCode()).isIn(HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN);
    }

    @Test
    void admin_users_requires_admin_role() {
        String token = login(DEALER_EMAIL, DEALER_PASSWORD, COMPANY);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        ResponseEntity<Map> response = rest.exchange(
                "/api/v1/admin/users",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class);
        System.out.println("M5 API evidence admin users role mismatch: status=" + response.getStatusCode()
                + " body=" + response.getBody());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void admin_user_update_blocks_cross_company_access() {
        String token = login(ADMIN_EMAIL, ADMIN_PASSWORD, COMPANY);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> payload = Map.of("displayName", "Other Admin Updated");

        ResponseEntity<Map> response = rest.exchange(
                "/api/v1/admin/users/" + otherCompanyUser.getId(),
                HttpMethod.PUT,
                new HttpEntity<>(payload, headers),
                Map.class);
        System.out.println("M5 API evidence admin update cross-company: status=" + response.getStatusCode()
                + " body=" + response.getBody());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void dealer_portal_allows_own_ledger_view() {
        String token = login(DEALER_EMAIL, DEALER_PASSWORD, COMPANY);
        HttpHeaders headers = authHeaders(token, COMPANY);
        ResponseEntity<Map> response = rest.exchange(
                "/api/v1/dealer-portal/ledger",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class);
        System.out.println("M5 API evidence dealer portal ledger: status=" + response.getStatusCode()
                + " body=" + response.getBody());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void dealer_portal_blocks_cross_dealer_ledger_access() {
        String token = login(DEALER_EMAIL, DEALER_PASSWORD, COMPANY);
        HttpHeaders headers = authHeaders(token, COMPANY);
        ResponseEntity<Map> response = rest.exchange(
                "/api/v1/dealers/" + sameCompanyOtherDealer.getId() + "/ledger",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class);
        System.out.println("M5 API evidence dealer cross-dealer ledger: status=" + response.getStatusCode()
                + " body=" + response.getBody());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void dealer_portal_blocks_cross_company_ledger_access() {
        String token = login(DEALER_EMAIL, DEALER_PASSWORD, COMPANY);
        HttpHeaders headers = authHeaders(token, COMPANY);
        ResponseEntity<Map> response = rest.exchange(
                "/api/v1/dealers/" + otherCompanyDealer.getId() + "/ledger",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class);
        System.out.println("M5 API evidence dealer cross-company ledger: status=" + response.getStatusCode()
                + " body=" + response.getBody());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void dealer_portal_blocks_sales_order_create() {
        String token = login(DEALER_EMAIL, DEALER_PASSWORD, COMPANY);
        HttpHeaders headers = authHeaders(token, COMPANY);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> payload = Map.of(
                "dealerId", portalDealer.getId(),
                "totalAmount", new BigDecimal("1000.00"),
                "currency", "INR",
                "notes", "Portal order attempt",
                "items", List.of(
                        Map.of(
                                "productCode", "PORTAL-ITEM",
                                "quantity", new BigDecimal("1"),
                                "unitPrice", new BigDecimal("1000.00")
                        )
                )
        );

        ResponseEntity<Map> response = rest.exchange(
                "/api/v1/sales/orders",
                HttpMethod.POST,
                new HttpEntity<>(payload, headers),
                Map.class);
        System.out.println("M5 API evidence dealer order forbidden: status=" + response.getStatusCode()
                + " body=" + response.getBody());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    private Dealer ensureDealer(Company company, String code, String name, UserAccount portalUser) {
        Dealer dealer = dealerRepository.findByCompanyAndCodeIgnoreCase(company, code)
                .orElseGet(() -> {
                    Dealer created = new Dealer();
                    created.setCompany(company);
                    created.setCode(code);
                    created.setName(name);
                    created.setCreditLimit(new BigDecimal("100000"));
                    return created;
                });
        dealer.setCompany(company);
        dealer.setCode(code);
        dealer.setName(name);
        if (portalUser != null) {
            dealer.setPortalUser(portalUser);
        }
        return dealerRepository.save(dealer);
    }

    private HttpHeaders authHeaders(String token, String companyCode) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.set("X-Company-Id", companyCode);
        return headers;
    }

    private String login(String email, String password, String companyCode) {
        Map<String, Object> body = Map.of(
                "email", email,
                "password", password,
                "companyCode", companyCode
        );
        ResponseEntity<Map> loginResp = rest.postForEntity("/api/v1/auth/login", body, Map.class);
        assertThat(loginResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> payload = loginResp.getBody();
        assertThat(payload).isNotNull();
        String token = payload.get("accessToken").toString();
        assertThat(token).isNotBlank();
        return token;
    }
}
