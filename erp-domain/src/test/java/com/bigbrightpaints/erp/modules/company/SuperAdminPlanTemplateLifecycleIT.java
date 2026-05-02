package com.bigbrightpaints.erp.modules.company;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.LinkedHashMap;
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

  @Test
  void planTemplateCreateRejectsIncompleteInvalidOrResponseOnlyDefaultLimitsWithoutPersistence() {
    for (String field : defaultLimitFields()) {
      String stableId = uniqueStableId("MISS");
      Map<String, Object> payload =
          planPayload(stableId, "Missing " + field, 12_345L, "PRIORITY", "missing " + field);
      defaultLimits(payload).remove(field);

      ResponseEntity<Map> response =
          rest.exchange(
              "/api/v1/superadmin/plans",
              HttpMethod.POST,
              new HttpEntity<>(payload, superAdminHeaders()),
              Map.class);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      assertPlanRowCount(stableId, 0);
    }

    for (String field : defaultLimitFields()) {
      String stableId = uniqueStableId("NEG");
      Map<String, Object> payload =
          planPayload(stableId, "Negative " + field, 12_345L, "PRIORITY", "negative " + field);
      defaultLimits(payload).put(field, -1);

      ResponseEntity<Map> response =
          rest.exchange(
              "/api/v1/superadmin/plans",
              HttpMethod.POST,
              new HttpEntity<>(payload, superAdminHeaders()),
              Map.class);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      assertPlanRowCount(stableId, 0);
    }

    assertCreateDefaultLimitsFieldRejected("ZERO", "zeroMeansUnlimited", true);
    assertCreateDefaultLimitsFieldRejected("UNKNOWN", "unexpectedLimit", 10);
  }

  @Test
  void planTemplateUpdateRejectsIncompleteInvalidOrResponseOnlyDefaultLimitsWithoutNewVersion() {
    String stableId = uniqueStableId("UPD");
    ResponseEntity<Map> createResponse =
        rest.exchange(
            "/api/v1/superadmin/plans",
            HttpMethod.POST,
            new HttpEntity<>(
                planPayload(stableId, "Update Guard", 12_345L, "PRIORITY", "create for update"),
                superAdminHeaders()),
            Map.class);
    assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertLatestVersion(stableId, 1);

    for (String field : defaultLimitFields()) {
      Map<String, Object> payload =
          planPayload(stableId, "Missing Update " + field, 22_222L, "DEDICATED", "missing update");
      defaultLimits(payload).remove(field);

      ResponseEntity<Map> response =
          rest.exchange(
              "/api/v1/superadmin/plans/" + stableId,
              HttpMethod.PUT,
              new HttpEntity<>(payload, superAdminHeaders()),
              Map.class);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      assertPlanRowCount(stableId, 1);
      assertLatestVersion(stableId, 1);
    }

    for (String field : defaultLimitFields()) {
      Map<String, Object> payload =
          planPayload(
              stableId, "Negative Update " + field, 22_222L, "DEDICATED", "negative update");
      defaultLimits(payload).put(field, -1);

      ResponseEntity<Map> response =
          rest.exchange(
              "/api/v1/superadmin/plans/" + stableId,
              HttpMethod.PUT,
              new HttpEntity<>(payload, superAdminHeaders()),
              Map.class);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      assertPlanRowCount(stableId, 1);
      assertLatestVersion(stableId, 1);
    }

    assertUpdateDefaultLimitsFieldRejected(stableId, "zeroMeansUnlimited", true);
    assertUpdateDefaultLimitsFieldRejected(stableId, "unexpectedLimit", 10);
  }

  @Test
  void planTemplateFeatureRegistryRejectsUnsupportedAndDisabledCoreKeysWithoutPersistence() {
    String unsupportedStableId = uniqueStableId("FEAT");
    Map<String, Object> unsupportedPayload =
        planPayload(
            unsupportedStableId,
            "Unsupported Feature",
            12_345L,
            "PRIORITY",
            "reject unsupported feature");
    unsupportedPayload.put("featureFlags", Map.of("ACCOUNTING", true, "UNKNOWN_FEATURE", true));

    ResponseEntity<Map> unsupportedResponse =
        rest.exchange(
            "/api/v1/superadmin/plans",
            HttpMethod.POST,
            new HttpEntity<>(unsupportedPayload, superAdminHeaders()),
            Map.class);

    assertThat(unsupportedResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertPlanRowCount(unsupportedStableId, 0);

    String coreStableId = uniqueStableId("CORE");
    Map<String, Object> disabledCorePayload =
        planPayload(
            coreStableId, "Disabled Core", 12_345L, "PRIORITY", "reject disabled core feature");
    disabledCorePayload.put("featureFlags", Map.of("ACCOUNTING", false, "PORTAL", true));

    ResponseEntity<Map> disabledCoreResponse =
        rest.exchange(
            "/api/v1/superadmin/plans",
            HttpMethod.POST,
            new HttpEntity<>(disabledCorePayload, superAdminHeaders()),
            Map.class);

    assertThat(disabledCoreResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertPlanRowCount(coreStableId, 0);

    String updateStableId = uniqueStableId("COREUPD");
    ResponseEntity<Map> createResponse =
        rest.exchange(
            "/api/v1/superadmin/plans",
            HttpMethod.POST,
            new HttpEntity<>(
                planPayload(
                    updateStableId,
                    "Feature Registry Update Guard",
                    12_345L,
                    "PRIORITY",
                    "create for feature update"),
                superAdminHeaders()),
            Map.class);
    assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

    Map<String, Object> updatePayload =
        planPayload(
            updateStableId,
            "Feature Registry Update Guard v2",
            22_222L,
            "DEDICATED",
            "reject update feature");
    updatePayload.put("featureFlags", Map.of("INVENTORY", false, "PORTAL", true));

    ResponseEntity<Map> updateResponse =
        rest.exchange(
            "/api/v1/superadmin/plans/" + updateStableId,
            HttpMethod.PUT,
            new HttpEntity<>(updatePayload, superAdminHeaders()),
            Map.class);

    assertThat(updateResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertPlanRowCount(updateStableId, 1);
    assertLatestVersion(updateStableId, 1);
  }

  @Test
  void planTemplateCreateAndUpdateStillAllowExplicitZeroLimitReadback() {
    String stableId = uniqueStableId("ZERO");
    Map<String, Object> createPayload =
        planPayload(stableId, "Zero Limit Plan", 0L, "STANDARD", "create zero limits");
    defaultLimits(createPayload).replaceAll((key, value) -> 0);

    ResponseEntity<Map> createResponse =
        rest.exchange(
            "/api/v1/superadmin/plans",
            HttpMethod.POST,
            new HttpEntity<>(createPayload, superAdminHeaders()),
            Map.class);

    assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    @SuppressWarnings("unchecked")
    Map<String, Object> created = (Map<String, Object>) createResponse.getBody().get("data");
    @SuppressWarnings("unchecked")
    Map<String, Object> createdLimits = (Map<String, Object>) created.get("defaultLimits");
    assertThat(createdLimits)
        .containsEntry("maxActiveUsers", 0)
        .containsEntry("zeroMeansUnlimited", true);

    Map<String, Object> updatePayload =
        planPayload(stableId, "Zero Limit Plan Updated", 0L, "STANDARD", "update zero limits");
    defaultLimits(updatePayload).replaceAll((key, value) -> 0);
    ResponseEntity<Map> updateResponse =
        rest.exchange(
            "/api/v1/superadmin/plans/" + stableId,
            HttpMethod.PUT,
            new HttpEntity<>(updatePayload, superAdminHeaders()),
            Map.class);

    assertThat(updateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    @SuppressWarnings("unchecked")
    Map<String, Object> updated = (Map<String, Object>) updateResponse.getBody().get("data");
    @SuppressWarnings("unchecked")
    Map<String, Object> updatedLimits = (Map<String, Object>) updated.get("defaultLimits");
    assertThat(updatedLimits)
        .containsEntry("maxActiveUsers", 0)
        .containsEntry("zeroMeansUnlimited", true);
    assertLatestVersion(stableId, 2);
  }

  private Map<String, Object> planPayload(
      String stableId,
      String displayName,
      long priceMinorUnits,
      String supportTier,
      String reason) {
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("stableId", stableId);
    payload.put("displayName", displayName);
    payload.put("cadence", "MONTHLY");
    payload.put("priceMinorUnits", priceMinorUnits);
    payload.put("currency", "INR");
    payload.put("trialDurationDays", 21);
    payload.put("supportTier", supportTier);
    payload.put("effectiveFrom", Instant.now().minusSeconds(1).toString());
    payload.put("featureFlags", Map.of("ACCOUNTING", true, "PORTAL", false));
    payload.put("defaultLimits", defaultLimitPayload());
    payload.put("reason", reason);
    return payload;
  }

  private Map<String, Object> defaultLimitPayload() {
    Map<String, Object> limits = new LinkedHashMap<>();
    limits.put("maxActiveUsers", 25);
    limits.put("maxApiRequests", 50_000);
    limits.put("maxStorageBytes", 5_368_709_120L);
    limits.put("maxPdfExports", 1_000);
    limits.put("maxEmails", 2_000);
    limits.put("maxJobs", 500);
    limits.put("burstRequestsPerMinute", 120);
    limits.put("maxConcurrentRequests", 12);
    return limits;
  }

  private List<String> defaultLimitFields() {
    return List.of(
        "maxActiveUsers",
        "maxApiRequests",
        "maxStorageBytes",
        "maxPdfExports",
        "maxEmails",
        "maxJobs",
        "burstRequestsPerMinute",
        "maxConcurrentRequests");
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> defaultLimits(Map<String, Object> payload) {
    return (Map<String, Object>) payload.get("defaultLimits");
  }

  private void assertCreateDefaultLimitsFieldRejected(String prefix, String field, Object value) {
    String stableId = uniqueStableId(prefix);
    Map<String, Object> payload =
        planPayload(stableId, "Rejected " + field, 12_345L, "PRIORITY", "reject " + field);
    defaultLimits(payload).put(field, value);

    ResponseEntity<Map> response =
        rest.exchange(
            "/api/v1/superadmin/plans",
            HttpMethod.POST,
            new HttpEntity<>(payload, superAdminHeaders()),
            Map.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertPlanRowCount(stableId, 0);
  }

  private void assertUpdateDefaultLimitsFieldRejected(String stableId, String field, Object value) {
    Map<String, Object> payload =
        planPayload(stableId, "Rejected Update " + field, 22_222L, "DEDICATED", "reject update");
    defaultLimits(payload).put(field, value);

    ResponseEntity<Map> response =
        rest.exchange(
            "/api/v1/superadmin/plans/" + stableId,
            HttpMethod.PUT,
            new HttpEntity<>(payload, superAdminHeaders()),
            Map.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertPlanRowCount(stableId, 1);
    assertLatestVersion(stableId, 1);
  }

  private String uniqueStableId(String prefix) {
    return "M8-" + prefix + "-" + Long.toString(System.nanoTime(), 36).toUpperCase(Locale.ROOT);
  }

  private void assertPlanRowCount(String stableId, int expectedCount) {
    Integer count =
        jdbcTemplate.queryForObject(
            "select count(*) from super_admin_plan_templates where stable_id = ?",
            Integer.class,
            stableId);
    assertThat(count).isEqualTo(expectedCount);
  }

  private void assertLatestVersion(String stableId, int expectedVersion) {
    Integer latestVersion =
        jdbcTemplate.queryForObject(
            "select max(template_version) from super_admin_plan_templates where stable_id = ?",
            Integer.class,
            stableId);
    assertThat(latestVersion).isEqualTo(expectedVersion);
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
