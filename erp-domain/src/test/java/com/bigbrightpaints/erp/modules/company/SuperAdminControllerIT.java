package com.bigbrightpaints.erp.modules.company;

import static org.assertj.core.api.Assertions.assertThat;

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
import org.springframework.jdbc.core.JdbcTemplate;

import com.bigbrightpaints.erp.modules.company.domain.Company;
import com.bigbrightpaints.erp.modules.company.domain.CompanyLifecycleState;
import com.bigbrightpaints.erp.modules.company.domain.CompanyRepository;
import com.bigbrightpaints.erp.test.AbstractIntegrationTest;

class SuperAdminControllerIT extends AbstractIntegrationTest {

  private static final String COMPANY_CODE = "ACME";
  private static final String ROOT_COMPANY_CODE = "PLATFORM";
  private static final String ADMIN_EMAIL = "admin@bbp.com";
  private static final String SUPER_ADMIN_EMAIL = "super-admin@bbp.com";
  private static final String LOGIN_CREDENTIAL = "admin123";

  @Autowired private TestRestTemplate rest;

  @Autowired private CompanyRepository companyRepository;

  @Autowired private JdbcTemplate jdbcTemplate;

  @BeforeEach
  void seedUsers() {
    dataSeeder.ensureUser(
        ADMIN_EMAIL, LOGIN_CREDENTIAL, "Admin", COMPANY_CODE, List.of("ROLE_ADMIN"));
    dataSeeder.ensureUser(
        SUPER_ADMIN_EMAIL,
        LOGIN_CREDENTIAL,
        "Super Admin",
        ROOT_COMPANY_CODE,
        List.of("ROLE_SUPER_ADMIN", "ROLE_ADMIN"));
    companyRepository
        .findByCodeIgnoreCase(COMPANY_CODE)
        .ifPresent(
            company -> {
              company.setLifecycleState(CompanyLifecycleState.ACTIVE);
              company.setLifecycleReason(null);
              companyRepository.save(company);
            });
  }

  @Test
  void dashboard_requiresSuperAdminAuthority() {
    String adminToken = loginToken(ADMIN_EMAIL, COMPANY_CODE);
    ResponseEntity<Map> forbidden =
        rest.exchange(
            "/api/v1/superadmin/dashboard",
            HttpMethod.GET,
            new HttpEntity<>(headers(adminToken, COMPANY_CODE)),
            Map.class);

    assertThat(forbidden.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

    String superAdminToken = loginToken(SUPER_ADMIN_EMAIL, ROOT_COMPANY_CODE);
    ResponseEntity<Map> allowed =
        rest.exchange(
            "/api/v1/superadmin/dashboard",
            HttpMethod.GET,
            new HttpEntity<>(headers(superAdminToken, ROOT_COMPANY_CODE)),
            Map.class);

    assertThat(allowed.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(allowed.getBody()).isNotNull();
    assertThat(allowed.getBody()).containsKey("data");
    @SuppressWarnings("unchecked")
    Map<String, Object> dashboard = (Map<String, Object>) allowed.getBody().get("data");
    assertThat(dashboard)
        .containsKeys(
            "totalClients",
            "activeClients",
            "trialClients",
            "suspendedClients",
            "mrrMinorUnits",
            "arrMinorUnits",
            "openSupportTickets",
            "openBugs",
            "storageBytes",
            "serverCostMinorUnits",
            "failedJobs",
            "apiErrorHealthBasisPoints",
            "riskClients");
  }

  @Test
  void superAdmin_profileReadUpdatePasswordAndSessionControlsAreSafe() {
    String superAdminToken = loginToken(SUPER_ADMIN_EMAIL, ROOT_COMPANY_CODE);
    HttpHeaders superAdminHeaders = headers(superAdminToken, ROOT_COMPANY_CODE);

    ResponseEntity<Map> profileResponse =
        rest.exchange(
            "/api/v1/superadmin/profile",
            HttpMethod.GET,
            new HttpEntity<>(superAdminHeaders),
            Map.class);
    assertThat(profileResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    @SuppressWarnings("unchecked")
    Map<String, Object> profile = (Map<String, Object>) profileResponse.getBody().get("data");
    assertThat(profile)
        .containsKeys(
            "displayName",
            "email",
            "phone",
            "avatarUrl",
            "timezone",
            "language",
            "sessions",
            "lastLoginAt");
    assertThat(profile).doesNotContainKeys("passwordHash", "token", "roles", "authorities");

    ResponseEntity<Map> updateResponse =
        rest.exchange(
            "/api/v1/superadmin/profile",
            HttpMethod.PUT,
            new HttpEntity<>(
                Map.of(
                    "displayName",
                    "Super Admin Updated",
                    "phone",
                    "+15550000000",
                    "avatarUrl",
                    "https://cdn.bigbrightpaints.example/avatar.png",
                    "timezone",
                    "Asia/Kolkata",
                    "language",
                    "en"),
                superAdminHeaders),
            Map.class);
    assertThat(updateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    @SuppressWarnings("unchecked")
    Map<String, Object> updated = (Map<String, Object>) updateResponse.getBody().get("data");
    assertThat(updated.get("displayName")).isEqualTo("Super Admin Updated");
    assertThat(updated.get("timezone")).isEqualTo("Asia/Kolkata");

    ResponseEntity<Map> forbiddenUpdate =
        rest.exchange(
            "/api/v1/superadmin/profile",
            HttpMethod.PUT,
            new HttpEntity<>(
                "{\"role\":\"ROLE_ADMIN\",\"passwordHash\":\"must-not-change\"}",
                superAdminHeaders),
            Map.class);
    assertThat(forbiddenUpdate.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

    ResponseEntity<Map> sessionsResponse =
        rest.exchange(
            "/api/v1/superadmin/profile/sessions",
            HttpMethod.GET,
            new HttpEntity<>(superAdminHeaders),
            Map.class);
    assertThat(sessionsResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> sessions =
        (List<Map<String, Object>>) sessionsResponse.getBody().get("data");
    assertThat(sessions).isNotEmpty();
    String sessionId = sessions.get(0).get("sessionId").toString();
    assertThat(sessions.get(0)).containsEntry("ipAddress", "redacted");

    ResponseEntity<Map> revokeResponse =
        rest.exchange(
            "/api/v1/superadmin/profile/sessions/" + sessionId + "/revoke",
            HttpMethod.POST,
            new HttpEntity<>(superAdminHeaders),
            Map.class);
    assertThat(revokeResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

    String passwordToken = loginToken(SUPER_ADMIN_EMAIL, ROOT_COMPANY_CODE);
    ResponseEntity<Map> passwordResponse =
        rest.exchange(
            "/api/v1/superadmin/profile/password",
            HttpMethod.POST,
            new HttpEntity<>(
                Map.of(
                    "currentPassword",
                    LOGIN_CREDENTIAL,
                    "newPassword",
                    "Changed!2026",
                    "confirmPassword",
                    "Changed!2026"),
                headers(passwordToken, ROOT_COMPANY_CODE)),
            Map.class);
    assertThat(passwordResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    @SuppressWarnings("unchecked")
    Map<String, Object> passwordData = (Map<String, Object>) passwordResponse.getBody().get("data");
    assertThat(passwordData.get("sessionPolicy")).isEqualTo("all-user-sessions-revoked");

    ResponseEntity<Map> oldTokenProbe =
        rest.exchange(
            "/api/v1/superadmin/profile",
            HttpMethod.GET,
            new HttpEntity<>(headers(passwordToken, ROOT_COMPANY_CODE)),
            Map.class);
    assertThat(oldTokenProbe.getStatusCode()).isIn(HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN);
  }

  @Test
  void superAdmin_settingsAreGroupedRedactedValidatedAndAudited() {
    String superAdminToken = loginToken(SUPER_ADMIN_EMAIL, ROOT_COMPANY_CODE);
    HttpHeaders superAdminHeaders = headers(superAdminToken, ROOT_COMPANY_CODE);

    ResponseEntity<Map> settingsResponse =
        rest.exchange(
            "/api/v1/superadmin/settings",
            HttpMethod.GET,
            new HttpEntity<>(superAdminHeaders),
            Map.class);
    assertThat(settingsResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    @SuppressWarnings("unchecked")
    Map<String, Object> settings = (Map<String, Object>) settingsResponse.getBody().get("data");
    assertThat(settings).containsKeys("access", "mail", "workflow", "security");
    @SuppressWarnings("unchecked")
    Map<String, Object> access = (Map<String, Object>) settings.get("access");
    @SuppressWarnings("unchecked")
    Map<String, Object> authCode = (Map<String, Object>) access.get("authCode");
    assertThat(authCode.get("value")).isEqualTo("<redacted>");

    ResponseEntity<Map> updateResponse =
        rest.exchange(
            "/api/v1/superadmin/settings",
            HttpMethod.PUT,
            new HttpEntity<>(
                Map.of(
                    "workflow",
                    Map.of("exportApprovalRequired", true),
                    "mail",
                    Map.of("sendPasswordReset", true)),
                superAdminHeaders),
            Map.class);
    assertThat(updateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

    ResponseEntity<Map> unknownKeyResponse =
        rest.exchange(
            "/api/v1/superadmin/settings",
            HttpMethod.PUT,
            new HttpEntity<>("{\"unknownGroup\":{\"enabled\":true}}", superAdminHeaders),
            Map.class);
    assertThat(unknownKeyResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  void superAdmin_canUpdateLifecycle_listTenants_andReadTenantDetail() {
    Company tenant = companyRepository.findByCodeIgnoreCase(COMPANY_CODE).orElseThrow();
    String superAdminToken = loginToken(SUPER_ADMIN_EMAIL, ROOT_COMPANY_CODE);
    HttpHeaders superAdminHeaders = headers(superAdminToken, ROOT_COMPANY_CODE);

    ResponseEntity<Map> suspendResponse =
        rest.exchange(
            "/api/v1/superadmin/tenants/" + tenant.getId() + "/lifecycle",
            HttpMethod.PUT,
            new HttpEntity<>(
                Map.of("state", "SUSPENDED", "reason", "ops-review"), superAdminHeaders),
            Map.class);
    assertThat(suspendResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(readLifecycleState(tenant.getId())).isEqualTo("SUSPENDED");

    ResponseEntity<Map> tenantsResponse =
        rest.exchange(
            "/api/v1/superadmin/tenants?status=SUSPENDED&q=acme&page=0&size=5&sort=companyCode,asc",
            HttpMethod.GET,
            new HttpEntity<>(superAdminHeaders),
            Map.class);
    assertThat(tenantsResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    @SuppressWarnings("unchecked")
    Map<String, Object> page = (Map<String, Object>) tenantsResponse.getBody().get("data");
    assertThat(page).containsEntry("page", 0).containsEntry("size", 5);
    assertThat(page.get("totalElements")).isNotNull();
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> tenants = (List<Map<String, Object>>) page.get("content");
    assertThat(tenants)
        .extracting(row -> row.get("companyCode").toString().toUpperCase(Locale.ROOT))
        .contains(COMPANY_CODE);
    assertThat(tenants.get(0))
        .containsKeys("status", "plan", "billingStatus", "usage", "trialEndsAt", "health");
    assertThat(tenants.get(0)).doesNotContainKeys("invoice", "ledger", "inventory", "salary");

    ResponseEntity<Map> invalidSizeResponse =
        rest.exchange(
            "/api/v1/superadmin/tenants?size=101",
            HttpMethod.GET,
            new HttpEntity<>(superAdminHeaders),
            Map.class);
    assertThat(invalidSizeResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

    ResponseEntity<Map> invalidSortResponse =
        rest.exchange(
            "/api/v1/superadmin/tenants?sort=privateLedger,asc",
            HttpMethod.GET,
            new HttpEntity<>(superAdminHeaders),
            Map.class);
    assertThat(invalidSortResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

    ResponseEntity<Map> detailResponse =
        rest.exchange(
            "/api/v1/superadmin/tenants/" + tenant.getId(),
            HttpMethod.GET,
            new HttpEntity<>(superAdminHeaders),
            Map.class);
    assertThat(detailResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    @SuppressWarnings("unchecked")
    Map<String, Object> detail = (Map<String, Object>) detailResponse.getBody().get("data");
    assertThat(detail.get("companyCode")).isEqualTo(COMPANY_CODE);
    assertThat(detail.get("lifecycleState")).isEqualTo("SUSPENDED");
    assertThat(detail.get("status")).isEqualTo("SUSPENDED_BLOCKED");
    assertThat(detail)
        .containsKeys(
            "overview",
            "onboarding",
            "plan",
            "usage",
            "billing",
            "support",
            "bugs",
            "audit",
            "settings");
    assertThat(detail).doesNotContainKeys("passwordHash", "token", "ledgerEntries", "invoices");

    ResponseEntity<Map> deactivateResponse =
        rest.exchange(
            "/api/v1/superadmin/tenants/" + tenant.getId() + "/lifecycle",
            HttpMethod.PUT,
            new HttpEntity<>(
                Map.of("state", "DEACTIVATED", "reason", "security-incident"), superAdminHeaders),
            Map.class);
    assertThat(deactivateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(readLifecycleState(tenant.getId())).isEqualTo("DEACTIVATED");

    ResponseEntity<Map> activateResponse =
        rest.exchange(
            "/api/v1/superadmin/tenants/" + tenant.getId() + "/lifecycle",
            HttpMethod.PUT,
            new HttpEntity<>(Map.of("state", "ACTIVE", "reason", "recovered"), superAdminHeaders),
            Map.class);
    assertThat(activateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(readLifecycleState(tenant.getId())).isEqualTo("ACTIVE");
  }

  @Test
  void superAdmin_lifecycle_update_rejects_retired_legacy_states() {
    Company tenant = companyRepository.findByCodeIgnoreCase(COMPANY_CODE).orElseThrow();
    String superAdminToken = loginToken(SUPER_ADMIN_EMAIL, ROOT_COMPANY_CODE);
    HttpHeaders superAdminHeaders = headers(superAdminToken, ROOT_COMPANY_CODE);

    ResponseEntity<Map> holdResponse =
        rest.exchange(
            "/api/v1/superadmin/tenants/" + tenant.getId() + "/lifecycle",
            HttpMethod.PUT,
            new HttpEntity<>(Map.of("state", "HOLD", "reason", "legacy-client"), superAdminHeaders),
            Map.class);

    assertThat(holdResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(readLifecycleState(tenant.getId())).isEqualTo("ACTIVE");

    ResponseEntity<Map> blockedResponse =
        rest.exchange(
            "/api/v1/superadmin/tenants/" + tenant.getId() + "/lifecycle",
            HttpMethod.PUT,
            new HttpEntity<>(
                Map.of("state", "BLOCKED", "reason", "legacy-client"), superAdminHeaders),
            Map.class);

    assertThat(blockedResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(readLifecycleState(tenant.getId())).isEqualTo("ACTIVE");
  }

  @Test
  void superAdmin_canConfigureTenantModules_andLimits() {
    Company tenant = companyRepository.findByCodeIgnoreCase(COMPANY_CODE).orElseThrow();
    String superAdminToken = loginToken(SUPER_ADMIN_EMAIL, ROOT_COMPANY_CODE);
    HttpHeaders superAdminHeaders = headers(superAdminToken, ROOT_COMPANY_CODE);

    ResponseEntity<Map> modulesResponse =
        rest.exchange(
            "/api/v1/superadmin/tenants/" + tenant.getId() + "/modules",
            HttpMethod.PUT,
            new HttpEntity<>(Map.of("enabledModules", List.of("PORTAL")), superAdminHeaders),
            Map.class);
    assertThat(modulesResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    Company updatedModules = companyRepository.findById(tenant.getId()).orElseThrow();
    assertThat(updatedModules.getEnabledModules()).containsExactly("PORTAL");

    ResponseEntity<Map> limitsResponse =
        rest.exchange(
            "/api/v1/superadmin/tenants/" + tenant.getId() + "/limits",
            HttpMethod.PUT,
            new HttpEntity<>(
                Map.of(
                    "quotaMaxActiveUsers", 120,
                    "quotaMaxApiRequests", 3000,
                    "quotaMaxStorageBytes", 2_097_152,
                    "quotaMaxConcurrentRequests", 7,
                    "quotaSoftLimitEnabled", true,
                    "quotaHardLimitEnabled", false),
                superAdminHeaders),
            Map.class);
    assertThat(limitsResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    @SuppressWarnings("unchecked")
    Map<String, Object> limits = (Map<String, Object>) limitsResponse.getBody().get("data");
    assertThat(limits.get("quotaMaxConcurrentRequests")).isEqualTo(7);
  }

  private HttpHeaders headers(String token, String companyCode) {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("X-Company-Code", companyCode);
    return headers;
  }

  private String loginToken(String email, String companyCode) {
    Map<String, Object> request =
        Map.of(
            "email", email,
            "password", LOGIN_CREDENTIAL,
            "companyCode", companyCode);
    ResponseEntity<Map> response = rest.postForEntity("/api/v1/auth/login", request, Map.class);
    return (String) response.getBody().get("accessToken");
  }

  private String readLifecycleState(Long companyId) {
    return jdbcTemplate.queryForObject(
        "select lifecycle_state from companies where id = ?", String.class, companyId);
  }
}
