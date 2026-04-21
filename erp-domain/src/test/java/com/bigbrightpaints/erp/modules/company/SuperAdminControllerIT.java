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

import com.bigbrightpaints.erp.core.security.AuthScopeService;
import com.bigbrightpaints.erp.modules.company.domain.Company;
import com.bigbrightpaints.erp.modules.company.domain.CompanyLifecycleState;
import com.bigbrightpaints.erp.modules.company.domain.CompanyRepository;
import com.bigbrightpaints.erp.test.AbstractIntegrationTest;

class SuperAdminControllerIT extends AbstractIntegrationTest {

  private static final String COMPANY_CODE = "ACME";
  private static final String ROOT_COMPANY_CODE = "ROOT";
  private static final String ADMIN_EMAIL = "admin@bbp.com";
  private static final String SUPER_ADMIN_EMAIL = "super-admin@bbp.com";
  private static final String PASSWORD = "admin123";
  private static final List<String> TENANT_BUSINESS_DATASET_KEYS =
      List.of(
          "orders",
          "invoices",
          "journalEntries",
          "dealerLedgers",
          "catalogInventory",
          "payrollRecords",
          "manufacturingRecords");

  @Autowired private TestRestTemplate rest;

  @Autowired private CompanyRepository companyRepository;

  @Autowired private JdbcTemplate jdbcTemplate;

  @BeforeEach
  void seedUsers() {
    dataSeeder.ensureUser(ADMIN_EMAIL, PASSWORD, "Admin", COMPANY_CODE, List.of("ROLE_ADMIN"));
    dataSeeder.ensureUser(
        SUPER_ADMIN_EMAIL,
        PASSWORD,
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
    Map<String, Object> dashboardData = (Map<String, Object>) allowed.getBody().get("data");
    assertThat(dashboardData).containsKey("billingSummary");
    assertContainsOnlyControlPlaneDashboardFields(dashboardData);
  }

  @Test
  void dashboard_and_tenant_inventory_are_denied_for_tenant_scoped_super_admin() {
    dataSeeder.ensureUser(
        SUPER_ADMIN_EMAIL,
        PASSWORD,
        "Tenant Scoped Super Admin",
        COMPANY_CODE,
        List.of("ROLE_SUPER_ADMIN"));

    String tenantScopedSuperAdminToken = loginToken(SUPER_ADMIN_EMAIL, COMPANY_CODE);

    ResponseEntity<Map> dashboardDenied =
        rest.exchange(
            "/api/v1/superadmin/dashboard",
            HttpMethod.GET,
            new HttpEntity<>(headers(tenantScopedSuperAdminToken, COMPANY_CODE)),
            Map.class);
    assertThat(dashboardDenied.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertPlatformOnlyDenial(dashboardDenied);

    ResponseEntity<Map> listDenied =
        rest.exchange(
            "/api/v1/superadmin/tenants",
            HttpMethod.GET,
            new HttpEntity<>(headers(tenantScopedSuperAdminToken, COMPANY_CODE)),
            Map.class);
    assertThat(listDenied.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertPlatformOnlyDenial(listDenied);

    Long tenantId = companyRepository.findByCodeIgnoreCase(COMPANY_CODE).orElseThrow().getId();
    ResponseEntity<Map> detailDenied =
        rest.exchange(
            "/api/v1/superadmin/tenants/" + tenantId,
            HttpMethod.GET,
            new HttpEntity<>(headers(tenantScopedSuperAdminToken, COMPANY_CODE)),
            Map.class);
    assertThat(detailDenied.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertPlatformOnlyDenial(detailDenied);
  }

  @Test
  void login_me_dashboard_tenants_flow_works_for_platform_scoped_super_admin() {
    String platformOnlySuperAdminEmail = "platform-flow-super-admin@bbp.com";
    dataSeeder.ensureUser(
        platformOnlySuperAdminEmail,
        PASSWORD,
        "Platform Flow Super Admin",
        AuthScopeService.DEFAULT_PLATFORM_AUTH_CODE,
        List.of("ROLE_SUPER_ADMIN"));

    String token =
        loginToken(platformOnlySuperAdminEmail, AuthScopeService.DEFAULT_PLATFORM_AUTH_CODE);
    HttpHeaders authHeaders = headers(token, AuthScopeService.DEFAULT_PLATFORM_AUTH_CODE);

    ResponseEntity<Map> meResponse =
        rest.exchange("/api/v1/auth/me", HttpMethod.GET, new HttpEntity<>(authHeaders), Map.class);
    assertThat(meResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(meResponse.getBody()).isNotNull();
    @SuppressWarnings("unchecked")
    Map<String, Object> meData = (Map<String, Object>) meResponse.getBody().get("data");
    assertThat(meData).isNotNull();
    assertThat(meData.get("companyCode")).isEqualTo(AuthScopeService.DEFAULT_PLATFORM_AUTH_CODE);

    ResponseEntity<Map> dashboardResponse =
        rest.exchange(
            "/api/v1/superadmin/dashboard",
            HttpMethod.GET,
            new HttpEntity<>(authHeaders),
            Map.class);
    assertThat(dashboardResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

    ResponseEntity<Map> tenantsResponse =
        rest.exchange(
            "/api/v1/superadmin/tenants",
            HttpMethod.GET,
            new HttpEntity<>(authHeaders),
            Map.class);
    assertThat(tenantsResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
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
            "/api/v1/superadmin/tenants?status=SUSPENDED",
            HttpMethod.GET,
            new HttpEntity<>(superAdminHeaders),
            Map.class);
    assertThat(tenantsResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> tenants =
        (List<Map<String, Object>>) tenantsResponse.getBody().get("data");
    assertThat(tenants)
        .extracting(row -> row.get("companyCode").toString().toUpperCase(Locale.ROOT))
        .contains(COMPANY_CODE);
    Map<String, Object> matchedTenant =
        tenants.stream()
            .filter(
                row ->
                    COMPANY_CODE.equalsIgnoreCase(String.valueOf(row.get("companyCode"))))
            .findFirst()
            .orElseThrow();
    assertContainsOnlyControlPlaneTenantSummaryFields(matchedTenant);

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
  void superAdmin_dashboard_list_and_detail_payloads_stay_control_plane_only() {
    Company tenant = companyRepository.findByCodeIgnoreCase(COMPANY_CODE).orElseThrow();
    String superAdminToken = loginToken(SUPER_ADMIN_EMAIL, ROOT_COMPANY_CODE);
    HttpHeaders superAdminHeaders = headers(superAdminToken, ROOT_COMPANY_CODE);

    ResponseEntity<Map> dashboardResponse =
        rest.exchange(
            "/api/v1/superadmin/dashboard",
            HttpMethod.GET,
            new HttpEntity<>(superAdminHeaders),
            Map.class);
    assertThat(dashboardResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    @SuppressWarnings("unchecked")
    Map<String, Object> dashboardData = (Map<String, Object>) dashboardResponse.getBody().get("data");
    assertContainsOnlyControlPlaneDashboardFields(dashboardData);

    ResponseEntity<Map> tenantsResponse =
        rest.exchange(
            "/api/v1/superadmin/tenants?status=ACTIVE",
            HttpMethod.GET,
            new HttpEntity<>(superAdminHeaders),
            Map.class);
    assertThat(tenantsResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> tenantRows =
        (List<Map<String, Object>>) tenantsResponse.getBody().get("data");
    assertThat(tenantRows).isNotEmpty();
    tenantRows.forEach(this::assertContainsOnlyControlPlaneTenantSummaryFields);

    ResponseEntity<Map> detailResponse =
        rest.exchange(
            "/api/v1/superadmin/tenants/" + tenant.getId(),
            HttpMethod.GET,
            new HttpEntity<>(superAdminHeaders),
            Map.class);
    assertThat(detailResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    @SuppressWarnings("unchecked")
    Map<String, Object> detailData = (Map<String, Object>) detailResponse.getBody().get("data");
    assertContainsOnlyControlPlaneTenantDetailFields(detailData);
  }

  @Test
  void superAdmin_canConfigureTenantModules_limits_andBillingPlan() {
    Company tenant = companyRepository.findByCodeIgnoreCase(COMPANY_CODE).orElseThrow();
    String superAdminToken = loginToken(SUPER_ADMIN_EMAIL, ROOT_COMPANY_CODE);
    HttpHeaders superAdminHeaders = headers(superAdminToken, ROOT_COMPANY_CODE);

    ResponseEntity<Map> baselineDashboardResponse =
        rest.exchange(
            "/api/v1/superadmin/dashboard",
            HttpMethod.GET,
            new HttpEntity<>(superAdminHeaders),
            Map.class);
    assertThat(baselineDashboardResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    @SuppressWarnings("unchecked")
    Map<String, Object> baselineDashboardData =
        (Map<String, Object>) baselineDashboardResponse.getBody().get("data");
    @SuppressWarnings("unchecked")
    Map<String, Object> baselineBillingSummary =
        (Map<String, Object>) baselineDashboardData.get("billingSummary");

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

    ResponseEntity<Map> billingPlanResponse =
        rest.exchange(
            "/api/v1/superadmin/tenants/" + tenant.getId() + "/billing-plan",
            HttpMethod.PUT,
            new HttpEntity<>(
                Map.of(
                    "planCode", "GROWTH",
                    "planName", "Growth Plan",
                    "currency", "USD",
                    "monthlyRate", 149.99,
                    "annualRate", 1799.88,
                    "seats", 35),
                superAdminHeaders),
            Map.class);
    assertThat(billingPlanResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    @SuppressWarnings("unchecked")
    Map<String, Object> billingPlan =
        (Map<String, Object>) billingPlanResponse.getBody().get("data");
    assertThat(billingPlan.get("planCode")).isEqualTo("GROWTH");
    assertThat(billingPlan.get("currency")).isEqualTo("USD");
    assertThat(billingPlan.get("monthlyRate")).isEqualTo(149.99);
    assertThat(billingPlan.get("annualRate")).isEqualTo(1799.88);
    assertThat(billingPlan.get("seats")).isEqualTo(35);

    ResponseEntity<Map> detailResponse =
        rest.exchange(
            "/api/v1/superadmin/tenants/" + tenant.getId(),
            HttpMethod.GET,
            new HttpEntity<>(superAdminHeaders),
            Map.class);
    assertThat(detailResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    @SuppressWarnings("unchecked")
    Map<String, Object> detailData = (Map<String, Object>) detailResponse.getBody().get("data");
    @SuppressWarnings("unchecked")
    Map<String, Object> detailBillingPlan = (Map<String, Object>) detailData.get("billingPlan");
    assertThat(detailBillingPlan).isNotNull();
    assertThat(detailBillingPlan.get("planCode")).isEqualTo("GROWTH");
    assertThat(detailBillingPlan.get("planName")).isEqualTo("Growth Plan");
    assertThat(detailBillingPlan.get("currency")).isEqualTo("USD");
    assertThat(detailBillingPlan.get("monthlyRate")).isEqualTo(149.99);
    assertThat(detailBillingPlan.get("annualRate")).isEqualTo(1799.88);
    assertThat(detailBillingPlan.get("seats")).isEqualTo(35);
    assertThat(detailBillingPlan.get("updatedBy")).isEqualTo(SUPER_ADMIN_EMAIL);
    assertContainsOnlyControlPlaneTenantDetailFields(detailData);

    ResponseEntity<Map> postDashboardResponse =
        rest.exchange(
            "/api/v1/superadmin/dashboard",
            HttpMethod.GET,
            new HttpEntity<>(superAdminHeaders),
            Map.class);
    assertThat(postDashboardResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    @SuppressWarnings("unchecked")
    Map<String, Object> postDashboardData =
        (Map<String, Object>) postDashboardResponse.getBody().get("data");
    @SuppressWarnings("unchecked")
    Map<String, Object> postBillingSummary =
        (Map<String, Object>) postDashboardData.get("billingSummary");
    assertThat(postBillingSummary).isNotNull();
    assertContainsOnlyControlPlaneDashboardFields(postDashboardData);
    assertThat(postBillingSummary)
        .containsKeys(
            "totalMonthlyRecurringRevenue", "totalAnnualRecurringRevenue", "billedTenantCount");

    double baselineMonthly =
        baselineBillingSummary == null
            ? 0.0
            : ((Number) baselineBillingSummary.get("totalMonthlyRecurringRevenue")).doubleValue();
    double baselineAnnual =
        baselineBillingSummary == null
            ? 0.0
            : ((Number) baselineBillingSummary.get("totalAnnualRecurringRevenue")).doubleValue();
    double postMonthly =
        ((Number) postBillingSummary.get("totalMonthlyRecurringRevenue")).doubleValue();
    double postAnnual =
        ((Number) postBillingSummary.get("totalAnnualRecurringRevenue")).doubleValue();

    assertThat(postMonthly).isGreaterThanOrEqualTo(149.99);
    assertThat(postAnnual).isGreaterThanOrEqualTo(1799.88);
    assertThat(postMonthly).isGreaterThanOrEqualTo(baselineMonthly);
    assertThat(postAnnual).isGreaterThanOrEqualTo(baselineAnnual);
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
            "password", PASSWORD,
            "companyCode", companyCode);
    ResponseEntity<Map> response = rest.postForEntity("/api/v1/auth/login", request, Map.class);
    return (String) response.getBody().get("accessToken");
  }

  private void assertContainsOnlyControlPlaneDashboardFields(Map<String, Object> dashboardData) {
    assertThat(dashboardData).isNotNull();
    assertThat(dashboardData)
        .containsKeys(
            "totalTenants",
            "activeTenants",
            "suspendedTenants",
            "deactivatedTenants",
            "totalActiveUsers",
            "totalActiveUserQuota",
            "totalAuditStorageBytes",
            "totalStorageQuotaBytes",
            "totalCurrentConcurrentRequests",
            "totalConcurrentRequestQuota",
            "billingSummary",
            "tenants")
        .doesNotContainKeys(TENANT_BUSINESS_DATASET_KEYS.toArray(String[]::new));
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> dashboardTenants =
        (List<Map<String, Object>>) dashboardData.get("tenants");
    assertThat(dashboardTenants).isNotNull();
    dashboardTenants.forEach(this::assertContainsOnlyControlPlaneTenantOverviewFields);
  }

  private void assertContainsOnlyControlPlaneTenantOverviewFields(
      Map<String, Object> tenantOverview) {
    assertThat(tenantOverview)
        .containsKeys(
            "companyId",
            "companyCode",
            "companyName",
            "region",
            "lifecycleState",
            "lifecycleReason",
            "activeUsers",
            "activeUserQuota",
            "auditStorageBytes",
            "storageQuotaBytes",
            "currentConcurrentRequests",
            "concurrentRequestQuota",
            "apiActivityCount",
            "apiRequestQuota",
            "apiErrorCount",
            "apiErrorRateInBasisPoints",
            "quotaSoftLimitEnabled",
            "quotaHardLimitEnabled",
            "activeUserUtilizationInBasisPoints",
            "auditStorageUtilizationInBasisPoints",
            "concurrentRequestUtilizationInBasisPoints")
        .doesNotContainKeys(TENANT_BUSINESS_DATASET_KEYS.toArray(String[]::new));
  }

  private void assertContainsOnlyControlPlaneTenantSummaryFields(
      Map<String, Object> tenantSummary) {
    assertThat(tenantSummary)
        .containsKeys(
            "companyId",
            "companyCode",
            "companyName",
            "timezone",
            "lifecycleState",
            "lifecycleReason",
            "activeUserCount",
            "quotaMaxActiveUsers",
            "apiActivityCount",
            "quotaMaxApiRequests",
            "auditStorageBytes",
            "quotaMaxStorageBytes",
            "currentConcurrentRequests",
            "quotaMaxConcurrentRequests",
            "enabledModules",
            "mainAdmin",
            "billingPlan",
            "lastActivityAt")
        .doesNotContainKeys(TENANT_BUSINESS_DATASET_KEYS.toArray(String[]::new));
  }

  private void assertContainsOnlyControlPlaneTenantDetailFields(
      Map<String, Object> tenantDetail) {
    assertThat(tenantDetail)
        .containsKeys(
            "companyId",
            "companyCode",
            "companyName",
            "timezone",
            "stateCode",
            "lifecycleState",
            "lifecycleReason",
            "enabledModules",
            "onboarding",
            "mainAdmin",
            "limits",
            "usage",
            "billingPlan",
            "supportContext",
            "supportTimeline",
            "availableActions")
        .doesNotContainKeys(TENANT_BUSINESS_DATASET_KEYS.toArray(String[]::new));
  }

  @SuppressWarnings("unchecked")
  private void assertPlatformOnlyDenial(ResponseEntity<Map> response) {
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().get("success")).isEqualTo(Boolean.FALSE);
    Map<String, Object> error = (Map<String, Object>) response.getBody().get("data");
    assertThat(error).isNotNull();
    assertThat(error.get("code")).isEqualTo("AUTH_004");
    assertThat(error.get("reason")).isEqualTo("SUPER_ADMIN_PLATFORM_ONLY");
    assertThat(error.get("reasonDetail"))
        .isEqualTo(
            "Super Admin is limited to platform control-plane operations and cannot execute tenant"
                + " business workflows");
  }

  private String readLifecycleState(Long companyId) {
    return jdbcTemplate.queryForObject(
        "select lifecycle_state from companies where id = ?", String.class, companyId);
  }
}
