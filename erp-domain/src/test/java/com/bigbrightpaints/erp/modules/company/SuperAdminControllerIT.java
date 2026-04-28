package com.bigbrightpaints.erp.modules.company;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

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
  void addClientOptionsAndDraftCreateAreStrictAndBranchFree() {
    String superAdminToken = loginToken(SUPER_ADMIN_EMAIL, ROOT_COMPANY_CODE);
    HttpHeaders superAdminHeaders = headers(superAdminToken, ROOT_COMPANY_CODE);

    ResponseEntity<Map> optionsResponse =
        rest.exchange(
            "/api/v1/superadmin/tenants/new",
            HttpMethod.GET,
            new HttpEntity<>(superAdminHeaders),
            Map.class);

    assertThat(optionsResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    @SuppressWarnings("unchecked")
    Map<String, Object> options = (Map<String, Object>) optionsResponse.getBody().get("data");
    assertThat(options)
        .containsKeys(
            "company",
            "owner",
            "commercial",
            "quotas",
            "modules",
            "support",
            "createModes",
            "seedPolicy");
    assertThat(options.toString().toLowerCase(Locale.ROOT)).doesNotContain("branch", "warehouse");

    String code = "M4IT" + System.nanoTime();
    String ownerEmail = "owner-" + code.toLowerCase(Locale.ROOT) + "@example.com";
    Map<String, Object> payload = addClientPayload(code, ownerEmail, "DRAFT");

    ResponseEntity<Map> createResponse =
        rest.exchange(
            "/api/v1/superadmin/tenants",
            HttpMethod.POST,
            new HttpEntity<>(payload, superAdminHeaders),
            Map.class);

    assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    @SuppressWarnings("unchecked")
    Map<String, Object> created = (Map<String, Object>) createResponse.getBody().get("data");
    assertThat(created).containsEntry("status", "DRAFT");
    @SuppressWarnings("unchecked")
    Map<String, Object> activation = (Map<String, Object>) created.get("activation");
    assertThat(activation).containsEntry("status", "NOT_SENT");
    assertThat(created.toString()).doesNotContain("temporaryPassword", "credentialsEmailSent");

    assertTenantListAndProfileReadBackStatus(superAdminHeaders, code, "DRAFT", ownerEmail);

    Map<String, Object> invalidPayload = new java.util.LinkedHashMap<>(payload);
    invalidPayload.put("warehouse", Map.of("name", "Forbidden"));
    ResponseEntity<Map> invalidResponse =
        rest.exchange(
            "/api/v1/superadmin/tenants",
            HttpMethod.POST,
            new HttpEntity<>(invalidPayload, superAdminHeaders),
            Map.class);
    assertThat(invalidResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  void addClientSendActivationReadBackRemainsPendingActivation() {
    String superAdminToken = loginToken(SUPER_ADMIN_EMAIL, ROOT_COMPANY_CODE);
    HttpHeaders superAdminHeaders = headers(superAdminToken, ROOT_COMPANY_CODE);
    String code = "M4ACT" + System.nanoTime();
    String ownerEmail = "owner-" + code.toLowerCase(Locale.ROOT) + "@example.com";

    ResponseEntity<Map> createResponse =
        rest.exchange(
            "/api/v1/superadmin/tenants",
            HttpMethod.POST,
            new HttpEntity<>(
                addClientPayload(code, ownerEmail, "SEND_ACTIVATION"), superAdminHeaders),
            Map.class);

    assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    @SuppressWarnings("unchecked")
    Map<String, Object> created = (Map<String, Object>) createResponse.getBody().get("data");
    assertThat(created).containsEntry("status", "PENDING_ACTIVATION");
    @SuppressWarnings("unchecked")
    Map<String, Object> activation = (Map<String, Object>) created.get("activation");
    assertThat(activation).containsEntry("status", "SENT");

    assertTenantListAndProfileReadBackStatus(
        superAdminHeaders, code, "PENDING_ACTIVATION", ownerEmail);
  }

  @Test
  void addClientNearConcurrentDuplicateCreatesExactlyOneTenantAndOwner() throws Exception {
    String superAdminToken = loginToken(SUPER_ADMIN_EMAIL, ROOT_COMPANY_CODE);
    HttpHeaders superAdminHeaders = headers(superAdminToken, ROOT_COMPANY_CODE);
    String marker = "M4DUP" + System.nanoTime();
    String ownerEmail = "owner-" + marker.toLowerCase(Locale.ROOT) + "@example.com";
    Map<String, Object> payload = addClientPayload(marker, ownerEmail, "DRAFT");
    Callable<ResponseEntity<Map>> createCall =
        () ->
            rest.exchange(
                "/api/v1/superadmin/tenants",
                HttpMethod.POST,
                new HttpEntity<>(payload, superAdminHeaders),
                Map.class);
    ExecutorService executor = Executors.newFixedThreadPool(2);
    try {
      List<Future<ResponseEntity<Map>>> futures =
          executor.invokeAll(List.of(createCall, createCall));
      ResponseEntity<Map> first = futures.get(0).get();
      ResponseEntity<Map> second = futures.get(1).get();

      assertThat(List.of(first.getStatusCode(), second.getStatusCode()))
          .containsExactlyInAnyOrder(HttpStatus.CREATED, HttpStatus.CONFLICT);
      assertThat(countRows("select count(*) from companies where lower(code) = lower(?)", marker))
          .isOne();
      assertThat(
              countRows("select count(*) from app_users where lower(email) = lower(?)", ownerEmail))
          .isOne();
    } finally {
      executor.shutdownNow();
      assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
    }
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
    assertThat(invalidSizeResponse.getBody()).containsEntry("success", false);

    ResponseEntity<Map> invalidPageResponse =
        rest.exchange(
            "/api/v1/superadmin/tenants?page=-1",
            HttpMethod.GET,
            new HttpEntity<>(superAdminHeaders),
            Map.class);
    assertThat(invalidPageResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(invalidPageResponse.getBody()).containsEntry("success", false);

    ResponseEntity<Map> invalidSortResponse =
        rest.exchange(
            "/api/v1/superadmin/tenants?sort=privateLedger,asc",
            HttpMethod.GET,
            new HttpEntity<>(superAdminHeaders),
            Map.class);
    assertThat(invalidSortResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

    ResponseEntity<Map> overflowPageResponse =
        rest.exchange(
            "/api/v1/superadmin/tenants?page=2147483647&size=100&sort=companyCode,asc",
            HttpMethod.GET,
            new HttpEntity<>(superAdminHeaders),
            Map.class);
    assertThat(overflowPageResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    @SuppressWarnings("unchecked")
    Map<String, Object> overflowPage =
        (Map<String, Object>) overflowPageResponse.getBody().get("data");
    assertThat(overflowPage).containsEntry("page", Integer.MAX_VALUE).containsEntry("size", 100);
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> overflowTenants =
        (List<Map<String, Object>>) overflowPage.get("content");
    assertThat(overflowTenants).isEmpty();

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
  void superAdminTenantListAndProfileUseProductionCanonicalStatusSource() {
    String superAdminToken = loginToken(SUPER_ADMIN_EMAIL, ROOT_COMPANY_CODE);
    HttpHeaders superAdminHeaders = headers(superAdminToken, ROOT_COMPANY_CODE);
    List<String> statuses =
        List.of(
            "DRAFT",
            "PENDING_ACTIVATION",
            "SETUP_PENDING",
            "TRIAL_ACTIVE",
            "ACTIVE",
            "GRACE",
            "SUSPENDED_READ_ONLY",
            "SUSPENDED_BLOCKED",
            "CANCELED",
            "ARCHIVED",
            "SEED_FAILED");
    for (int index = 0; index < statuses.size(); index++) {
      upsertStatusTenant(statuses.get(index), index);
    }

    for (int index = 0; index < statuses.size(); index++) {
      String status = statuses.get(index);
      String code = statusTenantCode(index);
      ResponseEntity<Map> response =
          rest.exchange(
              "/api/v1/superadmin/tenants?status="
                  + status.toLowerCase(Locale.ROOT)
                  + "&q="
                  + code
                  + "&page=0&size=20&sort=companyCode,asc",
              HttpMethod.GET,
              new HttpEntity<>(superAdminHeaders),
              Map.class);
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      @SuppressWarnings("unchecked")
      Map<String, Object> page = (Map<String, Object>) response.getBody().get("data");
      @SuppressWarnings("unchecked")
      List<Map<String, Object>> tenants = (List<Map<String, Object>>) page.get("content");
      assertThat(tenants)
          .anySatisfy(
              tenant -> {
                assertThat(tenant.get("companyCode")).isEqualTo(code);
                assertThat(tenant.get("status")).isEqualTo(status);
                assertThat(tenant.get("lifecycleState")).isEqualTo(status);
              });
    }

    Company seedFailedTenant =
        companyRepository
            .findByCodeIgnoreCase(statusTenantCode(statuses.indexOf("SEED_FAILED")))
            .orElseThrow();
    ResponseEntity<Map> detailResponse =
        rest.exchange(
            "/api/v1/superadmin/tenants/" + seedFailedTenant.getId(),
            HttpMethod.GET,
            new HttpEntity<>(superAdminHeaders),
            Map.class);
    assertThat(detailResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    @SuppressWarnings("unchecked")
    Map<String, Object> detail = (Map<String, Object>) detailResponse.getBody().get("data");
    assertThat(detail.get("status")).isEqualTo("SEED_FAILED");
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

  private Company upsertStatusTenant(String status, int index) {
    String code = statusTenantCode(index);
    Company company = companyRepository.findByCodeIgnoreCase(code).orElseGet(Company::new);
    company.setCode(code);
    company.setName("M3 " + status + " Tenant");
    company.setTimezone("UTC");
    company.setStateCode("KA");
    configureTenantStatusState(company, status, index);
    return companyRepository.save(company);
  }

  private String statusTenantCode(int index) {
    return "M3STATUS" + index;
  }

  private void configureTenantStatusState(Company company, String status, int index) {
    company.setLifecycleState(CompanyLifecycleState.ACTIVE);
    company.setLifecycleReason(null);
    company.setOnboardingAdminEmail(null);
    company.setOnboardingAdminUserId(null);
    company.setOnboardingCredentialsEmailedAt(null);
    company.setOnboardingCompletedAt(Instant.parse("2026-03-26T09:00:00Z"));
    switch (status) {
      case "DRAFT" -> {
        company.setOnboardingCompletedAt(null);
        company.setOnboardingAdminEmail("draft-" + index + "@example.com");
      }
      case "PENDING_ACTIVATION" -> {
        company.setOnboardingCompletedAt(null);
        company.setOnboardingAdminEmail("pending-" + index + "@example.com");
        company.setOnboardingCredentialsEmailedAt(Instant.parse("2026-03-26T10:00:00Z"));
      }
      case "SETUP_PENDING" -> company.setLifecycleReason(status);
      case "TRIAL_ACTIVE", "GRACE", "SEED_FAILED" -> company.setLifecycleReason(status);
      case "SUSPENDED_READ_ONLY", "SUSPENDED_BLOCKED" -> {
        company.setLifecycleState(CompanyLifecycleState.SUSPENDED);
        company.setLifecycleReason(status);
      }
      case "CANCELED", "ARCHIVED" -> {
        company.setLifecycleState(CompanyLifecycleState.DEACTIVATED);
        company.setLifecycleReason(status);
      }
      default -> {
        // ACTIVE uses the default completed onboarding and active lifecycle state.
      }
    }
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

  private Map<String, Object> addClientPayload(String code, String ownerEmail, String createMode) {
    return Map.of(
        "company",
        Map.of(
            "name",
            "M4 IT Client",
            "code",
            code,
            "timezone",
            "Asia/Kolkata",
            "stateCode",
            "KA",
            "baseCurrency",
            "INR",
            "defaultGstRate",
            18,
            "coaTemplateCode",
            "SME"),
        "owner",
        Map.of("email", ownerEmail, "displayName", "Owner"),
        "commercial",
        Map.of(
            "planId",
            "TRIAL",
            "billingStatus",
            "MANUAL",
            "trialDays",
            14,
            "supportTier",
            "STANDARD"),
        "quotas",
        Map.of(
            "maxActiveUsers",
            10,
            "maxApiRequests",
            10000,
            "maxStorageBytes",
            1073741824,
            "maxConcurrentRequests",
            8,
            "softLimitEnabled",
            false,
            "hardLimitEnabled",
            true),
        "modules",
        Map.of("enabled", List.of("ACCOUNTING", "SALES")),
        "support",
        Map.of("notes", "safe note", "tags", List.of("M4")),
        "createMode",
        createMode);
  }

  private void assertTenantListAndProfileReadBackStatus(
      HttpHeaders superAdminHeaders, String code, String expectedStatus, String ownerEmail) {
    ResponseEntity<Map> listResponse =
        rest.exchange(
            "/api/v1/superadmin/tenants?q=" + code + "&page=0&size=10&sort=companyCode,asc",
            HttpMethod.GET,
            new HttpEntity<>(superAdminHeaders),
            Map.class);
    assertThat(listResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    @SuppressWarnings("unchecked")
    Map<String, Object> page = (Map<String, Object>) listResponse.getBody().get("data");
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> tenants = (List<Map<String, Object>>) page.get("content");
    Map<String, Object> tenantRow =
        tenants.stream()
            .filter(tenant -> code.equals(tenant.get("companyCode")))
            .findFirst()
            .orElseThrow();
    assertThat(tenantRow).containsEntry("status", expectedStatus);
    @SuppressWarnings("unchecked")
    Map<String, Object> mainAdmin = (Map<String, Object>) tenantRow.get("mainAdmin");
    assertThat(mainAdmin).containsEntry("email", ownerEmail);

    Number tenantId = (Number) tenantRow.get("companyId");
    ResponseEntity<Map> profileResponse =
        rest.exchange(
            "/api/v1/superadmin/tenants/" + tenantId.longValue(),
            HttpMethod.GET,
            new HttpEntity<>(superAdminHeaders),
            Map.class);
    assertThat(profileResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    @SuppressWarnings("unchecked")
    Map<String, Object> profile = (Map<String, Object>) profileResponse.getBody().get("data");
    assertThat(profile).containsEntry("status", expectedStatus);
    @SuppressWarnings("unchecked")
    Map<String, Object> profileMainAdmin = (Map<String, Object>) profile.get("mainAdmin");
    assertThat(profileMainAdmin).containsEntry("email", ownerEmail);
  }

  private String readLifecycleState(Long companyId) {
    return jdbcTemplate.queryForObject(
        "select lifecycle_state from companies where id = ?", String.class, companyId);
  }

  private Long countRows(String sql, String value) {
    return jdbcTemplate.queryForObject(sql, Long.class, value);
  }
}
