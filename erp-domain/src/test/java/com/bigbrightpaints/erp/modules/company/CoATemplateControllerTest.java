package com.bigbrightpaints.erp.modules.company;

import static org.assertj.core.api.Assertions.assertThat;

import com.bigbrightpaints.erp.test.AbstractIntegrationTest;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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

class CoATemplateControllerTest extends AbstractIntegrationTest {

    private static final String ROOT_COMPANY_CODE = "ROOT";
    private static final String SUPER_ADMIN_EMAIL = "super-admin@bbp.com";
    private static final String ADMIN_EMAIL = "admin@bbp.com";
    private static final String PASSWORD = "admin123";

    @Autowired
    private TestRestTemplate rest;

    @BeforeEach
    void seedUsers() {
        dataSeeder.ensureUser(ADMIN_EMAIL, PASSWORD, "Admin", "ACME", List.of("ROLE_ADMIN"));
        dataSeeder.ensureUser(SUPER_ADMIN_EMAIL, PASSWORD, "Super Admin", ROOT_COMPANY_CODE,
                List.of("ROLE_SUPER_ADMIN", "ROLE_ADMIN"));
    }

    @Test
    void listTemplates_returnsAllSeededTemplates() {
        String superAdminToken = loginToken(SUPER_ADMIN_EMAIL, ROOT_COMPANY_CODE);
        ResponseEntity<Map> response = rest.exchange(
                "/api/v1/superadmin/tenants/coa-templates",
                HttpMethod.GET,
                new HttpEntity<>(headers(superAdminToken, ROOT_COMPANY_CODE)),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> templates = (List<Map<String, Object>>) response.getBody().get("data");
        assertThat(templates)
                .extracting(row -> row.get("code").toString().toUpperCase(Locale.ROOT))
                .contains("GENERIC", "INDIAN_STANDARD", "MANUFACTURING");
        assertThat(templates)
                .allSatisfy(row -> assertThat(Integer.parseInt(row.get("accountCount").toString()))
                        .isBetween(50, 100));
    }

    @Test
    void listTemplates_requiresSuperAdminAuthority() {
        String adminToken = loginToken(ADMIN_EMAIL, "ACME");
        ResponseEntity<Map> response = rest.exchange(
                "/api/v1/superadmin/tenants/coa-templates",
                HttpMethod.GET,
                new HttpEntity<>(headers(adminToken, "ACME")),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    private HttpHeaders headers(String token, String companyCode) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Company-Code", companyCode);
        return headers;
    }

    private String loginToken(String email, String companyCode) {
        Map<String, Object> request = Map.of(
                "email", email,
                "password", PASSWORD,
                "companyCode", companyCode);
        ResponseEntity<Map> response = rest.postForEntity("/api/v1/auth/login", request, Map.class);
        return (String) response.getBody().get("accessToken");
    }
}
