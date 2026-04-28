package com.bigbrightpaints.erp.modules.company;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
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

import com.bigbrightpaints.erp.test.AbstractIntegrationTest;

class SuperAdminPlanTemplateLifecycleIT extends AbstractIntegrationTest {

  private static final String ROOT_COMPANY_CODE = "ROOT";
  private static final String SUPER_ADMIN_EMAIL = "plan-template-superadmin@bbp.com";
  private static final String PASSWORD = "admin123";

  @Autowired private TestRestTemplate rest;
  @Autowired private JdbcTemplate jdbcTemplate;

  private String superAdminToken;

  @BeforeEach
  void seedSuperAdmin() {
    dataSeeder.ensureUser(
        SUPER_ADMIN_EMAIL,
        PASSWORD,
        "Plan Template Super Admin",
        ROOT_COMPANY_CODE,
        List.of("ROLE_SUPER_ADMIN", "ROLE_ADMIN"));
    superAdminToken = loginToken(SUPER_ADMIN_EMAIL, ROOT_COMPANY_CODE);
  }

  @Test
  void canonicalPlansAreListableWithExactCommercialSchema() {
    ResponseEntity<Map> response =
        rest.exchange(
            "/api/v1/superadmin/plans",
            HttpMethod.GET,
            new HttpEntity<>(superAdminHeaders()),
            Map.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    @SuppressWarnings("unchecked")
    Map<String, Object> body = (Map<String, Object>) response.getBody();
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> plans = (List<Map<String, Object>>) body.get("data");
    assertThat(plans)
        .extracting(plan -> plan.get("stableId"))
        .contains("TRIAL", "STARTER", "GROWTH", "ENTERPRISE", "CUSTOM");
    Map<String, Object> trial =
        plans.stream()
            .filter(plan -> "TRIAL".equals(plan.get("stableId")))
            .findFirst()
            .orElseThrow();
    assertThat(trial)
        .containsKeys(
            "stableId",
            "displayName",
            "status",
            "version",
            "effectiveFrom",
            "cadence",
            "priceMinorUnits",
            "currency",
            "trialDurationDays",
            "supportTier",
            "featureFlags",
            "defaultLimits",
            "assignedTenants",
            "mutationPolicy");
    @SuppressWarnings("unchecked")
    Map<String, Object> limits = (Map<String, Object>) trial.get("defaultLimits");
    assertThat(limits)
        .containsKeys(
            "maxActiveUsers",
            "maxApiRequests",
            "maxStorageBytes",
            "maxPdfExports",
            "maxEmails",
            "maxJobs",
            "burstRequestsPerMinute",
            "maxConcurrentRequests",
            "zeroMeansUnlimited");
  }

  @Test
  void planTemplateCreateUpdateArchiveAreVersionedReadableAndAudited() {
    String stableId = "M8-" + Long.toString(System.nanoTime(), 36).toUpperCase(Locale.ROOT);
    Map<String, Object> createPayload =
        planPayload(stableId, "M8 Custom Plan", 12_345L, "PRIORITY", "create test plan");

    ResponseEntity<Map> createResponse =
        rest.exchange(
            "/api/v1/superadmin/plans",
            HttpMethod.POST,
            new HttpEntity<>(createPayload, superAdminHeaders()),
            Map.class);

    assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    @SuppressWarnings("unchecked")
    Map<String, Object> created = (Map<String, Object>) createResponse.getBody().get("data");
    assertThat(created)
        .containsEntry("stableId", stableId)
        .containsEntry("displayName", "M8 Custom Plan")
        .containsEntry("status", "ACTIVE")
        .containsEntry("version", 1)
        .containsKey("auditEventId");
    assertAuditReason((Number) created.get("auditEventId"), "plan-template-created");

    Map<String, Object> updatePayload =
        planPayload(stableId, "M8 Custom Plan Updated", 22_222L, "DEDICATED", "update test plan");
    ResponseEntity<Map> updateResponse =
        rest.exchange(
            "/api/v1/superadmin/plans/" + stableId,
            HttpMethod.PUT,
            new HttpEntity<>(updatePayload, superAdminHeaders()),
            Map.class);

    assertThat(updateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    @SuppressWarnings("unchecked")
    Map<String, Object> updated = (Map<String, Object>) updateResponse.getBody().get("data");
    assertThat(updated)
        .containsEntry("stableId", stableId)
        .containsEntry("displayName", "M8 Custom Plan Updated")
        .containsEntry("version", 2)
        .containsEntry("status", "ACTIVE");
    @SuppressWarnings("unchecked")
    Map<String, Object> policy = (Map<String, Object>) updated.get("mutationPolicy");
    assertThat(policy)
        .containsEntry("versioning", "NEW_VERSION_PER_UPDATE")
        .containsEntry("assignedTenantBehavior", "ASSIGNED_TENANTS_FOLLOW_LATEST_EFFECTIVE_VERSION")
        .containsEntry("subscriptionPricePolicy", "SNAPSHOT_UNCHANGED_UNTIL_EXPLICIT_REPRICE");
    assertAuditReason((Number) updated.get("auditEventId"), "plan-template-updated");

    ResponseEntity<Map> archiveResponse =
        rest.exchange(
            "/api/v1/superadmin/plans/" + stableId + "/archive",
            HttpMethod.POST,
            new HttpEntity<>(Map.of("reason", "archive test plan"), superAdminHeaders()),
            Map.class);

    assertThat(archiveResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    @SuppressWarnings("unchecked")
    Map<String, Object> archived = (Map<String, Object>) archiveResponse.getBody().get("data");
    assertThat(archived)
        .containsEntry("stableId", stableId)
        .containsEntry("status", "ARCHIVED")
        .containsEntry("version", 2)
        .containsKey("archivedAt");
    assertAuditReason((Number) archived.get("auditEventId"), "plan-template-archived");

    ResponseEntity<Map> activeList =
        rest.exchange(
            "/api/v1/superadmin/plans",
            HttpMethod.GET,
            new HttpEntity<>(superAdminHeaders()),
            Map.class);
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> activePlans =
        (List<Map<String, Object>>) activeList.getBody().get("data");
    assertThat(activePlans).extracting(plan -> plan.get("stableId")).doesNotContain(stableId);

    ResponseEntity<Map> archivedRead =
        rest.exchange(
            "/api/v1/superadmin/plans/" + stableId + "?includeArchived=true",
            HttpMethod.GET,
            new HttpEntity<>(superAdminHeaders()),
            Map.class);
    assertThat(archivedRead.getStatusCode()).isEqualTo(HttpStatus.OK);
    @SuppressWarnings("unchecked")
    Map<String, Object> archivedData = (Map<String, Object>) archivedRead.getBody().get("data");
    assertThat(archivedData)
        .containsEntry("stableId", stableId)
        .containsEntry("status", "ARCHIVED")
        .containsEntry("version", 2);
  }

  @Test
  void assignedTenantBehaviorIsReportedForArchivedCanonicalTemplate() {
    ResponseEntity<Map> response =
        rest.exchange(
            "/api/v1/superadmin/plans/TRIAL",
            HttpMethod.GET,
            new HttpEntity<>(superAdminHeaders()),
            Map.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    @SuppressWarnings("unchecked")
    Map<String, Object> trial = (Map<String, Object>) response.getBody().get("data");
    @SuppressWarnings("unchecked")
    Map<String, Object> assignedTenants = (Map<String, Object>) trial.get("assignedTenants");
    Number assignedCount =
        jdbcTemplate.queryForObject(
            "select count(*) from companies where upper(commercial_plan_id) = 'TRIAL'",
            Number.class);
    assertThat(assignedTenants)
        .containsEntry("count", assignedCount.intValue())
        .containsEntry("archiveBehavior", "PLAN_HISTORY_REMAINS_READABLE");
  }

  private Map<String, Object> planPayload(
      String stableId,
      String displayName,
      long priceMinorUnits,
      String supportTier,
      String reason) {
    return Map.ofEntries(
        Map.entry("stableId", stableId),
        Map.entry("displayName", displayName),
        Map.entry("cadence", "MONTHLY"),
        Map.entry("priceMinorUnits", priceMinorUnits),
        Map.entry("currency", "INR"),
        Map.entry("trialDurationDays", 21),
        Map.entry("supportTier", supportTier),
        Map.entry("effectiveFrom", Instant.now().minusSeconds(1).toString()),
        Map.entry("featureFlags", Map.of("ACCOUNTING", true, "PORTAL", false)),
        Map.entry(
            "defaultLimits",
            Map.of(
                "maxActiveUsers",
                25,
                "maxApiRequests",
                50_000,
                "maxStorageBytes",
                5_368_709_120L,
                "maxPdfExports",
                1_000,
                "maxEmails",
                2_000,
                "maxJobs",
                500,
                "burstRequestsPerMinute",
                120,
                "maxConcurrentRequests",
                12)),
        Map.entry("reason", reason));
  }

  private void assertAuditReason(Number auditEventId, String reason) {
    assertThat(auditEventId).isNotNull();
    Integer matches =
        jdbcTemplate.queryForObject(
            "select count(*) from audit_log_metadata where audit_log_id = ? and metadata_key ="
                + " 'reason' and metadata_value = ?",
            Integer.class,
            auditEventId.longValue(),
            reason);
    assertThat(matches).isEqualTo(1);
  }

  private HttpHeaders superAdminHeaders() {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(superAdminToken);
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("X-Company-Code", ROOT_COMPANY_CODE);
    return headers;
  }

  private String loginToken(String email, String companyCode) {
    ResponseEntity<Map> response =
        rest.postForEntity(
            "/api/v1/auth/login",
            Map.of("email", email, "password", PASSWORD, "companyCode", companyCode),
            Map.class);
    return (String) response.getBody().get("accessToken");
  }
}
