package com.bigbrightpaints.erp.modules.admin;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import com.bigbrightpaints.erp.test.AbstractIntegrationTest;
import com.bigbrightpaints.erp.test.support.ErpApiRoutes;

@Tag("critical")
class AdminDashboardSecurityIT extends AbstractIntegrationTest {

  private static final String COMPANY_CODE = "ADMIN-DASH";
  private static final String PASSWORD = "AdminDash123!";
  private static final String ADMIN_EMAIL = "dashboard-admin@bbp.com";
  private static final String ACCOUNTING_EMAIL = "dashboard-accounting@bbp.com";
  private static final String SUPER_ADMIN_EMAIL = "dashboard-superadmin@bbp.com";

  @Autowired private TestRestTemplate rest;

  @BeforeEach
  void setUpUsers() {
    dataSeeder.ensureUser(ADMIN_EMAIL, PASSWORD, "Dashboard Admin", COMPANY_CODE, List.of("ROLE_ADMIN"));
    dataSeeder.ensureUser(
        ACCOUNTING_EMAIL,
        PASSWORD,
        "Dashboard Accounting",
        COMPANY_CODE,
        List.of("ROLE_ACCOUNTING"));
    dataSeeder.ensureUser(
        SUPER_ADMIN_EMAIL,
        PASSWORD,
        "Dashboard Super Admin",
        COMPANY_CODE,
        List.of("ROLE_SUPER_ADMIN", "ROLE_ADMIN"));
  }

  @Test
  void dashboard_allows_only_tenant_admin_role() {
    ResponseEntity<Map> adminResponse =
        rest.exchange(
            ErpApiRoutes.ADMIN_DASHBOARD,
            HttpMethod.GET,
            new HttpEntity<>(headersFor(ADMIN_EMAIL)),
            Map.class);
    assertThat(adminResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(adminResponse.getBody()).isNotNull();
    assertThat(adminResponse.getBody().get("success")).isEqualTo(Boolean.TRUE);
    @SuppressWarnings("unchecked")
    Map<String, Object> data = (Map<String, Object>) adminResponse.getBody().get("data");
    assertThat(data).isNotNull();
    assertThat(data).containsKeys("approvalSummary", "userSummary", "supportSummary", "tenantRuntime");

    ResponseEntity<Map> accountingResponse =
        rest.exchange(
            ErpApiRoutes.ADMIN_DASHBOARD,
            HttpMethod.GET,
            new HttpEntity<>(headersFor(ACCOUNTING_EMAIL)),
            Map.class);
    assertThat(accountingResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

    ResponseEntity<Map> superAdminResponse =
        rest.exchange(
            ErpApiRoutes.ADMIN_DASHBOARD,
            HttpMethod.GET,
            new HttpEntity<>(headersFor(SUPER_ADMIN_EMAIL)),
            Map.class);
    assertThat(superAdminResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
  }

  @Test
  void dashboard_hides_privileged_identity_counts_and_activity() {
    // Seed a same-tenant privileged actor event that must stay hidden from tenant-admin dashboard.
    headersFor(SUPER_ADMIN_EMAIL);

    ResponseEntity<Map> adminResponse =
        rest.exchange(
            ErpApiRoutes.ADMIN_DASHBOARD,
            HttpMethod.GET,
            new HttpEntity<>(headersFor(ADMIN_EMAIL)),
            Map.class);
    assertThat(adminResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(adminResponse.getBody()).isNotNull();

    @SuppressWarnings("unchecked")
    Map<String, Object> data = (Map<String, Object>) adminResponse.getBody().get("data");
    assertThat(data).isNotNull();

    @SuppressWarnings("unchecked")
    Map<String, Object> userSummary = (Map<String, Object>) data.get("userSummary");
    assertThat(userSummary).isNotNull();
    assertThat(((Number) userSummary.get("totalUsers")).longValue()).isEqualTo(1L);
    assertThat(((Number) userSummary.get("enabledUsers")).longValue()).isEqualTo(1L);
    assertThat(((Number) userSummary.get("disabledUsers")).longValue()).isEqualTo(0L);

    @SuppressWarnings("unchecked")
    List<Map<String, Object>> recentActivity = (List<Map<String, Object>>) data.get("recentActivity");
    assertThat(recentActivity).isNotNull();
    assertThat(recentActivity)
        .extracting(item -> String.valueOf(item.get("actor")).trim().toLowerCase())
        .doesNotContain(SUPER_ADMIN_EMAIL.toLowerCase());
  }

  private HttpHeaders headersFor(String email) {
    ResponseEntity<Map> loginResponse =
        rest.postForEntity(
            "/api/v1/auth/login",
            Map.of(
                "email", email,
                "password", PASSWORD,
                "companyCode", COMPANY_CODE),
            Map.class);
    assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(loginResponse.getBody()).isNotNull();

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setBearerAuth(String.valueOf(loginResponse.getBody().get("accessToken")));
    headers.set("X-Company-Code", COMPANY_CODE);
    return headers;
  }
}
