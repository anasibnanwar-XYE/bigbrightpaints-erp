package com.bigbrightpaints.erp.modules.company;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Collections;
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
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import com.bigbrightpaints.erp.modules.auth.service.AuthTokenDigests;
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

  @Autowired private JavaMailSender mailSender;

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
            "recurringRevenueByCurrency",
            "recurringRevenueAggregationPolicy",
            "recurringRevenueCurrencyCount",
            "openSupportTickets",
            "openBugs",
            "storageBytes",
            "serverCostMinorUnits",
            "failedJobs",
            "apiErrorHealthBasisPoints",
            "riskClients");
  }

  @Test
  void datadogStatusUsesCredentialSafeDegradedMode() {
    String superAdminToken = loginToken(SUPER_ADMIN_EMAIL, ROOT_COMPANY_CODE);
    ResponseEntity<Map> response =
        rest.exchange(
            "/api/v1/superadmin/observability/datadog/status",
            HttpMethod.GET,
            new HttpEntity<>(headers(superAdminToken, ROOT_COMPANY_CODE)),
            Map.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    @SuppressWarnings("unchecked")
    Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
    assertThat(data)
        .containsEntry("provider", "DATADOG")
        .containsEntry("credentialsExposed", false)
        .containsEntry("requiredForCoreFlows", false);
    assertThat(data.get("safeTagKeys").toString())
        .contains("route", "status_class", "actor_role", "actor_hash", "tenant_hash")
        .doesNotContain("request_body", "query_string", "email");
    assertThat(data.toString().toLowerCase(Locale.ROOT))
        .doesNotContain("dd_api_key", "sentry_auth_token", "bearer ", "admin123");
  }

  @Test
  void platformHealthRequiresSuperAdminAndReturnsRedactedComponentStatuses() {
    String adminToken = loginToken(ADMIN_EMAIL, COMPANY_CODE);
    ResponseEntity<Map> forbidden =
        rest.exchange(
            "/api/v1/superadmin/infra/health",
            HttpMethod.GET,
            new HttpEntity<>(headers(adminToken, COMPANY_CODE)),
            Map.class);

    assertThat(forbidden.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

    String superAdminToken = loginToken(SUPER_ADMIN_EMAIL, ROOT_COMPANY_CODE);
    ResponseEntity<Map> allowed =
        rest.exchange(
            "/api/v1/superadmin/infra/health",
            HttpMethod.GET,
            new HttpEntity<>(headers(superAdminToken, ROOT_COMPANY_CODE)),
            Map.class);

    assertThat(allowed.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(allowed.getBody()).isNotNull();
    @SuppressWarnings("unchecked")
    Map<String, Object> data = (Map<String, Object>) allowed.getBody().get("data");
    assertThat(data)
        .containsKeys(
            "checkedAt",
            "overallStatus",
            "traceId",
            "appReadiness",
            "database",
            "rabbitMq",
            "queue",
            "email",
            "sentry",
            "datadog",
            "backup",
            "failedJobs",
            "components",
            "redactionPolicy");
    assertThat(data.get("components").toString())
        .contains(
            "appReadiness",
            "database",
            "rabbitMq",
            "queue",
            "email",
            "sentry",
            "datadog",
            "backup",
            "failedJobs");
    String body = allowed.getBody().toString().toLowerCase(Locale.ROOT);
    assertThat(body)
        .doesNotContain(
            "admin123",
            "bearer ",
            "sentry_auth_token",
            "dd_api_key",
            "jdbc:",
            "amqp://",
            "stacktrace",
            "payload",
            "arguments");
  }

  @Test
  void infraCostSnapshotsScoreTenantsFromAggregateUsageAndAuditCorrectionsArchive() {
    String adminToken = loginToken(ADMIN_EMAIL, COMPANY_CODE);
    ResponseEntity<Map> forbidden =
        rest.exchange(
            "/api/v1/superadmin/infra/costs/snapshots",
            HttpMethod.POST,
            new HttpEntity<>(
                infraCostPayload("APP_SERVER", 1_000L, "Initial safe platform cost"),
                headers(adminToken, COMPANY_CODE)),
            Map.class);
    assertThat(forbidden.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

    String superAdminToken = loginToken(SUPER_ADMIN_EMAIL, ROOT_COMPANY_CODE);
    HttpHeaders superAdminHeaders = headers(superAdminToken, ROOT_COMPANY_CODE);
    List<String> components =
        List.of("APP_SERVER", "DATABASE", "STORAGE", "EMAIL", "BACKUP", "MONITORING");
    Long firstSnapshotId = null;
    for (String component : components) {
      ResponseEntity<Map> create =
          rest.exchange(
              "/api/v1/superadmin/infra/costs/snapshots",
              HttpMethod.POST,
              new HttpEntity<>(
                  infraCostPayload(component, 1_000L, "Initial safe platform cost"),
                  superAdminHeaders),
              Map.class);
      assertThat(create.getStatusCode()).isEqualTo(HttpStatus.CREATED);
      @SuppressWarnings("unchecked")
      Map<String, Object> snapshot = (Map<String, Object>) create.getBody().get("data");
      assertThat(snapshot)
          .containsEntry("component", component)
          .containsEntry("amountMinorUnits", 1_000)
          .containsEntry("currency", "INR")
          .containsEntry("status", "ACTIVE")
          .containsEntry("correctionCount", 0);
      assertInfraCostAuditReason(
          (Number) snapshot.get("auditEventId"), "infra-cost-snapshot-created");
      if (firstSnapshotId == null) {
        firstSnapshotId = ((Number) snapshot.get("snapshotId")).longValue();
      }
    }

    ResponseEntity<Map> invalidAmount =
        rest.exchange(
            "/api/v1/superadmin/infra/costs/snapshots",
            HttpMethod.POST,
            new HttpEntity<>(
                infraCostPayload("APP_SERVER", -1L, "Invalid amount probe"), superAdminHeaders),
            Map.class);
    assertThat(invalidAmount.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

    ResponseEntity<Map> privateTextProbe =
        rest.exchange(
            "/api/v1/superadmin/infra/costs/snapshots",
            HttpMethod.POST,
            new HttpEntity<>(
                infraCostPayload("APP_SERVER", 1_000L, "Contains invoice private data"),
                superAdminHeaders),
            Map.class);
    assertThat(privateTextProbe.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

    ResponseEntity<Map> correction =
        rest.exchange(
            "/api/v1/superadmin/infra/costs/snapshots/" + firstSnapshotId,
            HttpMethod.PUT,
            new HttpEntity<>(
                infraCostPayload("APP_SERVER", 1_500L, "Corrected safe platform cost"),
                superAdminHeaders),
            Map.class);
    assertThat(correction.getStatusCode()).isEqualTo(HttpStatus.OK);
    @SuppressWarnings("unchecked")
    Map<String, Object> corrected = (Map<String, Object>) correction.getBody().get("data");
    assertThat(corrected)
        .containsEntry("amountMinorUnits", 1_500)
        .containsEntry("correctionCount", 1)
        .containsEntry("status", "ACTIVE");
    assertInfraCostAuditReason(
        (Number) corrected.get("auditEventId"), "infra-cost-snapshot-corrected");

    ResponseEntity<Map> corrections =
        rest.exchange(
            "/api/v1/superadmin/infra/costs/snapshots/" + firstSnapshotId + "/corrections",
            HttpMethod.GET,
            new HttpEntity<>(superAdminHeaders),
            Map.class);
    assertThat(corrections.getStatusCode()).isEqualTo(HttpStatus.OK);
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> correctionRows =
        (List<Map<String, Object>>) corrections.getBody().get("data");
    assertThat(correctionRows).hasSize(1);
    assertThat(correctionRows.getFirst())
        .containsEntry("previousAmountMinorUnits", 1_000)
        .containsEntry("newAmountMinorUnits", 1_500);

    ResponseEntity<Map> dashboard =
        rest.exchange(
            "/api/v1/superadmin/infra/costs?currency=INR",
            HttpMethod.GET,
            new HttpEntity<>(superAdminHeaders),
            Map.class);
    assertThat(dashboard.getStatusCode()).isEqualTo(HttpStatus.OK);
    @SuppressWarnings("unchecked")
    Map<String, Object> data = (Map<String, Object>) dashboard.getBody().get("data");
    assertThat(data).containsEntry("currency", "INR").containsEntry("totalCostMinorUnits", 6_500);
    assertThat(data.get("latestComponentCosts").toString())
        .contains("APP_SERVER", "DATABASE", "STORAGE", "EMAIL", "BACKUP", "MONITORING");
    assertThat(data.get("aggregateUsage").toString()).contains("USERS", "STORAGE", "API_CALLS");
    assertThat(data.get("tenantCostScores").toString())
        .contains("tenantCode", "costScoreBasisPoints");
    @SuppressWarnings("unchecked")
    Map<String, Object> privacy = (Map<String, Object>) data.get("privacy");
    assertThat(privacy).containsEntry("aggregateUsageOnly", true);
    assertThat(dashboard.getBody().toString().toLowerCase(Locale.ROOT))
        .doesNotContain(
            "admin123",
            "bearer ",
            "password",
            "token",
            "invoice",
            "ledger",
            "inventory",
            "salary",
            "vendor",
            "customer",
            "gst return");

    ResponseEntity<Map> archive =
        rest.exchange(
            "/api/v1/superadmin/infra/costs/snapshots/" + firstSnapshotId + "/archive",
            HttpMethod.POST,
            new HttpEntity<>(Map.of("reason", "Archive safe obsolete cost"), superAdminHeaders),
            Map.class);
    assertThat(archive.getStatusCode()).isEqualTo(HttpStatus.OK);
    @SuppressWarnings("unchecked")
    Map<String, Object> archived = (Map<String, Object>) archive.getBody().get("data");
    assertThat(archived).containsEntry("status", "ARCHIVED");
    assertInfraCostAuditReason(
        (Number) archived.get("auditEventId"), "infra-cost-snapshot-archived");

    ResponseEntity<Map> activeList =
        rest.exchange(
            "/api/v1/superadmin/infra/costs/snapshots?currency=INR",
            HttpMethod.GET,
            new HttpEntity<>(superAdminHeaders),
            Map.class);
    assertThat(activeList.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(activeList.getBody().get("data").toString())
        .doesNotContain("snapshotId=" + firstSnapshotId);

    ResponseEntity<Map> includeArchived =
        rest.exchange(
            "/api/v1/superadmin/infra/costs/snapshots?currency=INR&includeArchived=true",
            HttpMethod.GET,
            new HttpEntity<>(superAdminHeaders),
            Map.class);
    assertThat(includeArchived.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(includeArchived.getBody().get("data").toString()).contains("ARCHIVED");
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
    assertThat(invalidResponse.getBody().toString()).contains("warehouse", "Unsupported field");
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
  void activationActionsVerifyAndCompleteUseSafeLinksAndSingleUseTokens() {
    org.mockito.Mockito.reset(mailSender);
    String superAdminToken = loginToken(SUPER_ADMIN_EMAIL, ROOT_COMPANY_CODE);
    HttpHeaders superAdminHeaders = headers(superAdminToken, ROOT_COMPANY_CODE);
    superAdminHeaders.set("Host", "evil.example");
    superAdminHeaders.set("Forwarded", "host=evil.example;proto=https");
    superAdminHeaders.set("X-Forwarded-Host", "evil.example");
    superAdminHeaders.set("X-Forwarded-Proto", "https");
    String code = "M5ACT" + Long.toString(System.nanoTime(), 36).toUpperCase(Locale.ROOT);
    String ownerEmail = "owner-" + code.toLowerCase(Locale.ROOT) + "@example.com";

    ResponseEntity<Map> draftResponse =
        rest.exchange(
            "/api/v1/superadmin/tenants",
            HttpMethod.POST,
            new HttpEntity<>(addClientPayload(code, ownerEmail, "DRAFT"), superAdminHeaders),
            Map.class);
    assertThat(draftResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    @SuppressWarnings("unchecked")
    Map<String, Object> draft = (Map<String, Object>) draftResponse.getBody().get("data");
    Number tenantId = (Number) draft.get("tenantId");

    ResponseEntity<Map> sendResponse =
        rest.exchange(
            "/api/v1/superadmin/tenants/" + tenantId.longValue() + "/activation/send",
            HttpMethod.POST,
            new HttpEntity<>(superAdminHeaders),
            Map.class);

    assertThat(sendResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    @SuppressWarnings("unchecked")
    Map<String, Object> sent = (Map<String, Object>) sendResponse.getBody().get("data");
    assertThat(sent).containsEntry("activationStatus", "SENT");
    assertThat(sent.toString()).doesNotContain("temporaryPassword", "OwnerActivated123!");

    org.mockito.ArgumentCaptor<SimpleMailMessage> messageCaptor =
        org.mockito.ArgumentCaptor.forClass(SimpleMailMessage.class);
    org.mockito.Mockito.verify(mailSender).send(messageCaptor.capture());
    SimpleMailMessage activationEmail = messageCaptor.getValue();
    assertThat(activationEmail.getSubject()).contains("Activate");
    assertThat(activationEmail.getText())
        .contains("http://localhost:3004/activate-client?token=")
        .contains("No password or temporary credential is included")
        .doesNotContain("evil.example", "temporary password");

    ResponseEntity<Map> copyResponse =
        rest.exchange(
            "/api/v1/superadmin/tenants/" + tenantId.longValue() + "/activation/copy",
            HttpMethod.POST,
            new HttpEntity<>(superAdminHeaders),
            Map.class);
    assertThat(copyResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    @SuppressWarnings("unchecked")
    Map<String, Object> copied = (Map<String, Object>) copyResponse.getBody().get("data");
    String copiedUrl = copied.get("activationUrl").toString();
    String copiedToken = copiedUrl.substring(copiedUrl.indexOf("token=") + "token=".length());
    assertThat(copiedUrl).startsWith("http://localhost:3004/activate-client?token=");
    assertThat(
            countRows(
                "select count(*) from tenant_activation_tokens where company_id = ?"
                    + " and token_digest = ?",
                tenantId.longValue(),
                activationDigest(copiedToken)))
        .isOne();
    assertThat(
            countRows(
                "select count(*) from tenant_activation_tokens where company_id = ?"
                    + " and token_digest = ? and digest_algorithm = ? and digest_version = ?",
                tenantId.longValue(),
                activationDigest(copiedToken),
                AuthTokenDigests.DIGEST_ALGORITHM,
                AuthTokenDigests.DIGEST_VERSION))
        .isOne();
    assertThat(
            countRows(
                "select count(*) from information_schema.columns where table_name = ?"
                    + " and column_name in ('token', 'activation_token', 'activation_url',"
                    + " 'activation_link')",
                "tenant_activation_tokens"))
        .isZero();
    org.mockito.Mockito.verifyNoMoreInteractions(mailSender);

    String emailedToken =
        activationEmail
            .getText()
            .substring(activationEmail.getText().indexOf("token=") + 6)
            .lines()
            .findFirst()
            .orElseThrow();
    ResponseEntity<Map> supersededVerify =
        rest.exchange(
            "/api/v1/auth/activation/verify?token=" + emailedToken,
            HttpMethod.GET,
            new HttpEntity<>(new HttpHeaders()),
            Map.class);
    assertThat(supersededVerify.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

    ResponseEntity<Map> verifyResponse =
        rest.exchange(
            "/api/v1/auth/activation/verify?token=" + copiedToken,
            HttpMethod.GET,
            new HttpEntity<>(new HttpHeaders()),
            Map.class);
    assertThat(verifyResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    @SuppressWarnings("unchecked")
    Map<String, Object> verifyData = (Map<String, Object>) verifyResponse.getBody().get("data");
    assertThat(verifyData).containsEntry("companyCode", code).containsKey("requiredSetupSteps");
    assertThat(verifyData.toString()).doesNotContain(copiedToken, "passwordHash");

    ResponseEntity<Map> weakComplete =
        rest.exchange(
            "/api/v1/auth/activation/complete",
            HttpMethod.POST,
            new HttpEntity<>(
                Map.of("token", copiedToken, "newPassword", "weak", "confirmPassword", "weak"),
                jsonHeaders()),
            Map.class);
    assertThat(weakComplete.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

    ResponseEntity<Map> completeResponse =
        rest.exchange(
            "/api/v1/auth/activation/complete",
            HttpMethod.POST,
            new HttpEntity<>(
                Map.of(
                    "token",
                    copiedToken,
                    "newPassword",
                    "OwnerActivated123!",
                    "confirmPassword",
                    "OwnerActivated123!"),
                jsonHeaders()),
            Map.class);
    assertThat(completeResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    @SuppressWarnings("unchecked")
    Map<String, Object> completeData = (Map<String, Object>) completeResponse.getBody().get("data");
    assertThat(completeData)
        .containsEntry("ownerState", "ACTIVE")
        .containsEntry("tenantStatus", "SETUP_PENDING");

    ResponseEntity<Map> replayResponse =
        rest.exchange(
            "/api/v1/auth/activation/complete",
            HttpMethod.POST,
            new HttpEntity<>(
                Map.of(
                    "token",
                    copiedToken,
                    "newPassword",
                    "OwnerActivated124!",
                    "confirmPassword",
                    "OwnerActivated124!"),
                jsonHeaders()),
            Map.class);
    assertThat(replayResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

    ResponseEntity<Map> ownerLogin =
        rest.exchange(
            "/api/v1/auth/login",
            HttpMethod.POST,
            new HttpEntity<>(
                Map.of("email", ownerEmail, "password", "OwnerActivated123!", "companyCode", code),
                jsonHeaders()),
            Map.class);
    assertThat(ownerLogin.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  @Test
  void ownerFirstLoginSetupCorridorIsOrderedResumableIdempotentAndAuthorized() {
    org.mockito.Mockito.reset(mailSender);
    String superAdminToken = loginToken(SUPER_ADMIN_EMAIL, ROOT_COMPANY_CODE);
    HttpHeaders superAdminHeaders = headers(superAdminToken, ROOT_COMPANY_CODE);
    String code = "M6SET" + Long.toString(System.nanoTime(), 36).toUpperCase(Locale.ROOT);
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
    Number tenantId = (Number) created.get("tenantId");
    org.mockito.ArgumentCaptor<SimpleMailMessage> messageCaptor =
        org.mockito.ArgumentCaptor.forClass(SimpleMailMessage.class);
    org.mockito.Mockito.verify(mailSender).send(messageCaptor.capture());
    String activationToken =
        messageCaptor
            .getValue()
            .getText()
            .substring(messageCaptor.getValue().getText().indexOf("token=") + 6)
            .lines()
            .findFirst()
            .orElseThrow();

    ResponseEntity<Map> completeResponse =
        rest.exchange(
            "/api/v1/auth/activation/complete",
            HttpMethod.POST,
            new HttpEntity<>(
                Map.of(
                    "token",
                    activationToken,
                    "newPassword",
                    "OwnerSetup123!",
                    "confirmPassword",
                    "OwnerSetup123!"),
                jsonHeaders()),
            Map.class);
    assertThat(completeResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    @SuppressWarnings("unchecked")
    Map<String, Object> completeData = (Map<String, Object>) completeResponse.getBody().get("data");
    assertThat(completeData)
        .containsEntry("tenantStatus", "SETUP_PENDING")
        .containsKey("nextSetupSteps");
    assertThat(completeData.toString().toLowerCase(Locale.ROOT))
        .doesNotContain("branch", "warehouse", "temporarypassword");

    String ownerToken = loginToken(ownerEmail, code, "OwnerSetup123!");
    HttpHeaders ownerHeaders = headers(ownerToken, code);
    ResponseEntity<Map> ownerMe =
        rest.exchange("/api/v1/auth/me", HttpMethod.GET, new HttpEntity<>(ownerHeaders), Map.class);
    assertThat(ownerMe.getStatusCode()).isEqualTo(HttpStatus.OK);
    @SuppressWarnings("unchecked")
    Map<String, Object> ownerIdentity = (Map<String, Object>) ownerMe.getBody().get("data");
    @SuppressWarnings("unchecked")
    List<String> ownerRoles = (List<String>) ownerIdentity.get("roles");
    assertThat(ownerRoles).contains("ROLE_ADMIN").doesNotContain("ROLE_SUPER_ADMIN");

    ResponseEntity<Map> setupStatus =
        rest.exchange(
            "/api/v1/setup/status", HttpMethod.GET, new HttpEntity<>(ownerHeaders), Map.class);
    assertThat(setupStatus.getStatusCode()).isEqualTo(HttpStatus.OK);
    @SuppressWarnings("unchecked")
    Map<String, Object> initialStatus = (Map<String, Object>) setupStatus.getBody().get("data");
    assertThat(initialStatus)
        .containsEntry("tenantStatus", "SETUP_PENDING")
        .containsEntry("setupRequired", true);
    assertThat(initialStatus.toString().toLowerCase(Locale.ROOT))
        .contains("company-details", "gst", "accounting", "invite-team", "finish")
        .doesNotContain("branch", "warehouse");
    @SuppressWarnings("unchecked")
    List<String> roleOptions = (List<String>) initialStatus.get("roleOptions");
    assertThat(roleOptions)
        .containsExactly("ROLE_ACCOUNTING", "ROLE_FACTORY", "ROLE_SALES", "ROLE_DEALER")
        .doesNotContain("ROLE_SUPER_ADMIN", "ROLE_ADMIN");

    ResponseEntity<Map> seedStatus =
        rest.exchange(
            "/api/v1/superadmin/tenants/" + tenantId.longValue() + "/seed-status",
            HttpMethod.GET,
            new HttpEntity<>(superAdminHeaders),
            Map.class);
    assertThat(seedStatus.getStatusCode()).isEqualTo(HttpStatus.OK);
    @SuppressWarnings("unchecked")
    Map<String, Object> seedData = (Map<String, Object>) seedStatus.getBody().get("data");
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> roleTemplates =
        (List<Map<String, Object>>) seedData.get("roleTemplates");
    List<String> roleTemplateKeys =
        roleTemplates.stream().map(template -> template.get("key").toString()).toList();
    assertThat(roleTemplateKeys)
        .containsExactlyElementsOf(roleOptions)
        .doesNotContain(
            "ROLE_SUPER_ADMIN", "ROLE_ADMIN", "TENANT_OWNER", "TENANT_ADMIN", "TENANT_STAFF");

    ResponseEntity<Map> prematureAccounting =
        rest.exchange(
            "/api/v1/setup/accounting",
            HttpMethod.PUT,
            new HttpEntity<>(Map.of("confirmDefaults", true), ownerHeaders),
            Map.class);
    assertThat(prematureAccounting.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

    ResponseEntity<Map> setupRequiredProbe =
        rest.exchange(
            "/api/v1/companies", HttpMethod.GET, new HttpEntity<>(ownerHeaders), Map.class);
    assertThat(setupRequiredProbe.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(setupRequiredProbe.getBody().toString()).contains("TENANT_SETUP_REQUIRED");

    ResponseEntity<Map> companyDetails =
        rest.exchange(
            "/api/v1/setup/company-details",
            HttpMethod.PUT,
            new HttpEntity<>(
                Map.of(
                    "name",
                    "M6 Setup Client",
                    "timezone",
                    "Asia/Kolkata",
                    "stateCode",
                    "MH",
                    "tenantId",
                    "must-not-mutate",
                    "code",
                    "must-not-mutate"),
                ownerHeaders),
            Map.class);
    assertThat(companyDetails.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(companyDetails.getBody().toString())
        .contains("Unsupported field")
        .containsAnyOf("tenantId", "code");

    ResponseEntity<Map> companyDetailsWithLocation =
        rest.exchange(
            "/api/v1/setup/company-details",
            HttpMethod.PUT,
            new HttpEntity<>(
                Map.of(
                    "name",
                    "M6 Setup Client",
                    "timezone",
                    "Asia/Kolkata",
                    "stateCode",
                    "MH",
                    "branch",
                    "B1"),
                ownerHeaders),
            Map.class);
    assertThat(companyDetailsWithLocation.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(companyDetailsWithLocation.getBody().toString())
        .contains("branch", "Unsupported field");

    companyDetails =
        rest.exchange(
            "/api/v1/setup/company-details",
            HttpMethod.PUT,
            new HttpEntity<>(
                Map.of("name", "M6 Setup Client", "timezone", "Asia/Kolkata", "stateCode", "MH"),
                ownerHeaders),
            Map.class);
    assertThat(companyDetails.getStatusCode()).isEqualTo(HttpStatus.OK);

    ResponseEntity<Map> gstWithWarehouse =
        rest.exchange(
            "/api/v1/setup/gst",
            HttpMethod.PUT,
            new HttpEntity<>(
                Map.of("enabled", true, "defaultGstRate", 18, "warehouse", "W1"), ownerHeaders),
            Map.class);
    assertThat(gstWithWarehouse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(gstWithWarehouse.getBody().toString()).contains("warehouse", "Unsupported field");

    ResponseEntity<Map> gst =
        rest.exchange(
            "/api/v1/setup/gst",
            HttpMethod.PUT,
            new HttpEntity<>(
                Map.of("enabled", true, "defaultGstRate", 18, "stateCode", "MH"), ownerHeaders),
            Map.class);
    assertThat(gst.getStatusCode()).isEqualTo(HttpStatus.OK);

    ResponseEntity<Map> accounting =
        rest.exchange(
            "/api/v1/setup/accounting",
            HttpMethod.PUT,
            new HttpEntity<>(
                Map.of("confirmDefaults", true, "branch", "B1", "warehouse", "W1"), ownerHeaders),
            Map.class);
    assertThat(accounting.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(accounting.getBody().toString())
        .contains("Unsupported field")
        .containsAnyOf("branch", "warehouse");

    accounting =
        rest.exchange(
            "/api/v1/setup/accounting",
            HttpMethod.PUT,
            new HttpEntity<>(Map.of("confirmDefaults", true), ownerHeaders),
            Map.class);
    assertThat(accounting.getStatusCode()).isEqualTo(HttpStatus.OK);

    Long usersBeforeInvalidRoleInvites = countRows("select count(*) from app_users", null);
    for (String invalidRole :
        List.of(
            "ROLE_SUPER_ADMIN",
            "ROLE_ADMIN",
            "TENANT_OWNER",
            "TENANT_ADMIN",
            "TENANT_STAFF",
            "ROLE_TENANT_ADMIN")) {
      ResponseEntity<Map> invalidRoleInvite =
          rest.exchange(
              "/api/v1/setup/invite-team",
              HttpMethod.POST,
              new HttpEntity<>(
                  Map.of(
                      "invitations",
                      List.of(
                          Map.of(
                              "email",
                              "blocked-"
                                  + invalidRole.toLowerCase(Locale.ROOT).replace('_', '-')
                                  + "-"
                                  + code.toLowerCase(Locale.ROOT)
                                  + "@example.com",
                              "displayName",
                              "Bad Role",
                              "role",
                              invalidRole))),
                  ownerHeaders),
              Map.class);
      assertThat(invalidRoleInvite.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      assertThat(invalidRoleInvite.getBody().toString())
          .contains("Invite role must be one of")
          .contains("ROLE_ACCOUNTING", "ROLE_FACTORY", "ROLE_SALES", "ROLE_DEALER");
    }
    assertThat(countRows("select count(*) from app_users", null))
        .isEqualTo(usersBeforeInvalidRoleInvites);

    Long usersBeforeNullInvite = countRows("select count(*) from app_users", null);
    ResponseEntity<Map> nullInvite =
        rest.exchange(
            "/api/v1/setup/invite-team",
            HttpMethod.POST,
            new HttpEntity<>(Map.of("invitations", Collections.singletonList(null)), ownerHeaders),
            Map.class);
    assertThat(nullInvite.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(nullInvite.getBody().toString()).contains("invitations");
    assertThat(countRows("select count(*) from app_users", null)).isEqualTo(usersBeforeNullInvite);

    String invitedEmail = "invited-" + code.toLowerCase(Locale.ROOT) + "@example.com";
    ResponseEntity<Map> positiveInvite =
        rest.exchange(
            "/api/v1/setup/invite-team",
            HttpMethod.POST,
            new HttpEntity<>(
                Map.of(
                    "invitations",
                    List.of(
                        Map.of(
                            "email",
                            invitedEmail,
                            "displayName",
                            "Invited Sales",
                            "role",
                            "sales"))),
                ownerHeaders),
            Map.class);
    assertThat(positiveInvite.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(
            countRows("select count(*) from app_users where lower(email) = lower(?)", invitedEmail))
        .isOne();

    ResponseEntity<Map> replayInvite =
        rest.exchange(
            "/api/v1/setup/invite-team",
            HttpMethod.POST,
            new HttpEntity<>(
                Map.of(
                    "invitations",
                    List.of(
                        Map.of(
                            "email",
                            invitedEmail,
                            "displayName",
                            "Invited Sales",
                            "role",
                            "ROLE_SALES"))),
                ownerHeaders),
            Map.class);
    assertThat(replayInvite.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(
            countRows("select count(*) from app_users where lower(email) = lower(?)", invitedEmail))
        .isOne();

    String staffEmail = "staff-" + code.toLowerCase(Locale.ROOT) + "@example.com";
    dataSeeder.ensureUser(staffEmail, LOGIN_CREDENTIAL, "Staff", code, List.of("ROLE_SALES"));
    String staffToken = loginToken(staffEmail, code);
    ResponseEntity<Map> staffFinish =
        rest.exchange(
            "/api/v1/setup/finish",
            HttpMethod.POST,
            new HttpEntity<>(headers(staffToken, code)),
            Map.class);
    assertThat(staffFinish.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

    for (String staleField : List.of("branch", "warehouse", "surpriseField")) {
      ResponseEntity<Map> staleFinish =
          rest.exchange(
              "/api/v1/setup/finish",
              HttpMethod.POST,
              new HttpEntity<>(Map.of(staleField, "stale-value"), ownerHeaders),
              Map.class);
      assertThat(staleFinish.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      assertThat(staleFinish.getBody().toString()).contains(staleField, "Unsupported field");
      assertThat(
              countRows(
                  "select count(*) from companies where lower(code) = lower(?)"
                      + " and onboarding_completed_at is not null",
                  code))
          .isZero();
    }

    ResponseEntity<Map> finish =
        rest.exchange(
            "/api/v1/setup/finish", HttpMethod.POST, new HttpEntity<>(ownerHeaders), Map.class);
    assertThat(finish.getStatusCode()).isEqualTo(HttpStatus.OK);
    @SuppressWarnings("unchecked")
    Map<String, Object> finishData = (Map<String, Object>) finish.getBody().get("data");
    assertThat(finishData).containsEntry("setupRequired", false);
    assertThat(finishData.get("tenantStatus")).isIn("TRIAL_ACTIVE", "ACTIVE");

    ResponseEntity<Map> replayFinish =
        rest.exchange(
            "/api/v1/setup/finish", HttpMethod.POST, new HttpEntity<>(ownerHeaders), Map.class);
    assertThat(replayFinish.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(
            countRows(
                "select count(*) from companies where lower(code) = lower(?)"
                    + " and onboarding_completed_at is not null",
                code))
        .isOne();

    ResponseEntity<Map> postFinishCompanies =
        rest.exchange(
            "/api/v1/companies", HttpMethod.GET, new HttpEntity<>(ownerHeaders), Map.class);
    assertThat(postFinishCompanies.getStatusCode()).isEqualTo(HttpStatus.OK);

    ResponseEntity<Map> superAdminStatus =
        rest.exchange(
            "/api/v1/setup/status", HttpMethod.GET, new HttpEntity<>(superAdminHeaders), Map.class);
    assertThat(superAdminStatus.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
  }

  @Test
  void activationExpireBlocksCurrentTokenButAllowsRecoveryByResend() {
    org.mockito.Mockito.reset(mailSender);
    String superAdminToken = loginToken(SUPER_ADMIN_EMAIL, ROOT_COMPANY_CODE);
    HttpHeaders superAdminHeaders = headers(superAdminToken, ROOT_COMPANY_CODE);
    String code = "M5EXP" + Long.toString(System.nanoTime(), 36).toUpperCase(Locale.ROOT);
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
    Number tenantId = (Number) created.get("tenantId");
    org.mockito.ArgumentCaptor<SimpleMailMessage> firstMessage =
        org.mockito.ArgumentCaptor.forClass(SimpleMailMessage.class);
    org.mockito.Mockito.verify(mailSender).send(firstMessage.capture());
    String expiredToken =
        firstMessage
            .getValue()
            .getText()
            .substring(firstMessage.getValue().getText().indexOf("token=") + 6)
            .lines()
            .findFirst()
            .orElseThrow();

    ResponseEntity<Map> expireResponse =
        rest.exchange(
            "/api/v1/superadmin/tenants/" + tenantId.longValue() + "/activation/expire",
            HttpMethod.POST,
            new HttpEntity<>(superAdminHeaders),
            Map.class);
    assertThat(expireResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    @SuppressWarnings("unchecked")
    Map<String, Object> expired = (Map<String, Object>) expireResponse.getBody().get("data");
    assertThat(expired).containsEntry("activationStatus", "EXPIRED");

    ResponseEntity<Map> expiredVerify =
        rest.exchange(
            "/api/v1/auth/activation/verify?token=" + expiredToken,
            HttpMethod.GET,
            new HttpEntity<>(new HttpHeaders()),
            Map.class);
    assertThat(expiredVerify.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

    ResponseEntity<Map> resendResponse =
        rest.exchange(
            "/api/v1/superadmin/tenants/" + tenantId.longValue() + "/activation/resend",
            HttpMethod.POST,
            new HttpEntity<>(superAdminHeaders),
            Map.class);
    assertThat(resendResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    org.mockito.Mockito.verify(mailSender, org.mockito.Mockito.times(2))
        .send(org.mockito.ArgumentMatchers.any(SimpleMailMessage.class));
  }

  @Test
  void activationSendMailFailureCommitsAuditedTokenAndAllowsRecoveryByResend() {
    org.mockito.Mockito.reset(mailSender);
    org.mockito.Mockito.doThrow(new MailSendException("smtp unavailable"))
        .when(mailSender)
        .send(org.mockito.ArgumentMatchers.any(SimpleMailMessage.class));
    String superAdminToken = loginToken(SUPER_ADMIN_EMAIL, ROOT_COMPANY_CODE);
    HttpHeaders superAdminHeaders = headers(superAdminToken, ROOT_COMPANY_CODE);
    String code = "M5MAIL" + Long.toString(System.nanoTime(), 36).toUpperCase(Locale.ROOT);
    String ownerEmail = "owner-" + code.toLowerCase(Locale.ROOT) + "@example.com";

    ResponseEntity<Map> createResponse =
        rest.exchange(
            "/api/v1/superadmin/tenants",
            HttpMethod.POST,
            new HttpEntity<>(addClientPayload(code, ownerEmail, "DRAFT"), superAdminHeaders),
            Map.class);
    assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    @SuppressWarnings("unchecked")
    Map<String, Object> created = (Map<String, Object>) createResponse.getBody().get("data");
    Number tenantId = (Number) created.get("tenantId");
    long tokenCountBefore =
        countRows("select count(*) from tenant_activation_tokens where company_id = ?", tenantId);

    ResponseEntity<Map> sendResponse =
        rest.exchange(
            "/api/v1/superadmin/tenants/" + tenantId.longValue() + "/activation/send",
            HttpMethod.POST,
            new HttpEntity<>(superAdminHeaders),
            Map.class);

    assertThat(sendResponse.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    Company tenant = companyRepository.findByCodeIgnoreCase(code).orElseThrow();
    assertThat(tenant.getActivationStatus()).isEqualTo("SENT");
    assertThat(
            countRows(
                "select count(*) from tenant_activation_tokens where company_id = ?", tenantId))
        .isEqualTo(tokenCountBefore + 1);

    org.mockito.Mockito.reset(mailSender);
    ResponseEntity<Map> resendResponse =
        rest.exchange(
            "/api/v1/superadmin/tenants/" + tenantId.longValue() + "/activation/resend",
            HttpMethod.POST,
            new HttpEntity<>(superAdminHeaders),
            Map.class);
    assertThat(resendResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    org.mockito.Mockito.verify(mailSender)
        .send(org.mockito.ArgumentMatchers.any(SimpleMailMessage.class));
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
  void addClientDuplicateCompanyCodeIsNormalizedAtApiBoundaryWithoutSideEffects() {
    String superAdminToken = loginToken(SUPER_ADMIN_EMAIL, ROOT_COMPANY_CODE);
    HttpHeaders superAdminHeaders = headers(superAdminToken, ROOT_COMPANY_CODE);
    String code = "M4NORM" + System.nanoTime();
    String originalOwnerEmail = "owner-" + code.toLowerCase(Locale.ROOT) + "@example.com";
    String duplicateOwnerEmail = "duplicate-" + code.toLowerCase(Locale.ROOT) + "@example.com";

    ResponseEntity<Map> createdResponse =
        rest.exchange(
            "/api/v1/superadmin/tenants",
            HttpMethod.POST,
            new HttpEntity<>(
                addClientPayload(code, originalOwnerEmail, "DRAFT"), superAdminHeaders),
            Map.class);
    assertThat(createdResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

    ResponseEntity<Map> duplicateResponse =
        rest.exchange(
            "/api/v1/superadmin/tenants",
            HttpMethod.POST,
            new HttpEntity<>(
                addClientPayload(
                    " " + code.toLowerCase(Locale.ROOT) + " ", duplicateOwnerEmail, "DRAFT"),
                superAdminHeaders),
            Map.class);

    assertThat(duplicateResponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(countRows("select count(*) from companies where lower(code) = lower(?)", code))
        .isOne();
    assertThat(
            countRows(
                "select count(*) from app_users where lower(email) = lower(?)", originalOwnerEmail))
        .isOne();
    assertThat(
            countRows(
                "select count(*) from app_users where lower(email) = lower(?)",
                duplicateOwnerEmail))
        .isZero();
  }

  @Test
  void addClientDuplicateOwnerEmailIsNormalizedAtApiBoundaryWithoutTenantOrTokenSideEffects() {
    String superAdminToken = loginToken(SUPER_ADMIN_EMAIL, ROOT_COMPANY_CODE);
    HttpHeaders superAdminHeaders = headers(superAdminToken, ROOT_COMPANY_CODE);
    String originalCode = "M4MAIL" + System.nanoTime();
    String duplicateCode = originalCode + "DUP";
    String ownerEmail = "owner-" + originalCode.toLowerCase(Locale.ROOT) + "@example.com";
    long activationTokensBefore = countRows("select count(*) from tenant_activation_tokens", null);

    ResponseEntity<Map> createdResponse =
        rest.exchange(
            "/api/v1/superadmin/tenants",
            HttpMethod.POST,
            new HttpEntity<>(
                addClientPayload(originalCode, ownerEmail, "DRAFT"), superAdminHeaders),
            Map.class);
    assertThat(createdResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

    ResponseEntity<Map> duplicateResponse =
        rest.exchange(
            "/api/v1/superadmin/tenants",
            HttpMethod.POST,
            new HttpEntity<>(
                addClientPayload(
                    duplicateCode,
                    " " + ownerEmail.toUpperCase(Locale.ROOT) + " ",
                    "SEND_ACTIVATION"),
                superAdminHeaders),
            Map.class);

    assertThat(duplicateResponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(
            countRows("select count(*) from companies where lower(code) = lower(?)", originalCode))
        .isOne();
    assertThat(
            countRows("select count(*) from companies where lower(code) = lower(?)", duplicateCode))
        .isZero();
    assertThat(
            countRows("select count(*) from app_users where lower(email) = lower(?)", ownerEmail))
        .isOne();
    assertThat(countRows("select count(*) from tenant_activation_tokens", null))
        .isEqualTo(activationTokensBefore);
  }

  @Test
  void addClientDuplicateMaxLengthCompanyCodeTrimsBeforeConflictHandling() {
    String superAdminToken = loginToken(SUPER_ADMIN_EMAIL, ROOT_COMPANY_CODE);
    HttpHeaders superAdminHeaders = headers(superAdminToken, ROOT_COMPANY_CODE);
    String code = maxLengthCompanyCode();
    String originalOwnerEmail = "owner-" + code.toLowerCase(Locale.ROOT) + "@example.com";
    String duplicateOwnerEmail = "duplicate-" + code.toLowerCase(Locale.ROOT) + "@example.com";

    ResponseEntity<Map> createdResponse =
        rest.exchange(
            "/api/v1/superadmin/tenants",
            HttpMethod.POST,
            new HttpEntity<>(
                addClientPayload(code, originalOwnerEmail, "DRAFT"), superAdminHeaders),
            Map.class);
    assertThat(createdResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

    ResponseEntity<Map> duplicateResponse =
        rest.exchange(
            "/api/v1/superadmin/tenants",
            HttpMethod.POST,
            new HttpEntity<>(
                addClientPayload(
                    " " + code.toLowerCase(Locale.ROOT) + " ", duplicateOwnerEmail, "DRAFT"),
                superAdminHeaders),
            Map.class);

    assertThat(duplicateResponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(countRows("select count(*) from companies where lower(code) = lower(?)", code))
        .isOne();
    assertThat(
            countRows(
                "select count(*) from app_users where lower(email) = lower(?)", originalOwnerEmail))
        .isOne();
    assertThat(
            countRows(
                "select count(*) from app_users where lower(email) = lower(?)",
                duplicateOwnerEmail))
        .isZero();
  }

  @Test
  void addClientDuplicateMaxLengthOwnerEmailTrimsBeforeConflictHandling() {
    String superAdminToken = loginToken(SUPER_ADMIN_EMAIL, ROOT_COMPANY_CODE);
    HttpHeaders superAdminHeaders = headers(superAdminToken, ROOT_COMPANY_CODE);
    String originalCode = "M4MAXM" + Long.toString(System.nanoTime(), 36).toUpperCase(Locale.ROOT);
    String duplicateCode = originalCode + "D";
    String ownerEmail = maxLengthOwnerEmail();

    ResponseEntity<Map> createdResponse =
        rest.exchange(
            "/api/v1/superadmin/tenants",
            HttpMethod.POST,
            new HttpEntity<>(
                addClientPayload(originalCode, ownerEmail, "DRAFT"), superAdminHeaders),
            Map.class);
    assertThat(createdResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    long activationTokensAfterOriginal =
        countRows("select count(*) from tenant_activation_tokens", null);

    ResponseEntity<Map> duplicateResponse =
        rest.exchange(
            "/api/v1/superadmin/tenants",
            HttpMethod.POST,
            new HttpEntity<>(
                addClientPayload(
                    duplicateCode,
                    " " + ownerEmail.toUpperCase(Locale.ROOT) + " ",
                    "SEND_ACTIVATION"),
                superAdminHeaders),
            Map.class);

    assertThat(duplicateResponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(
            countRows("select count(*) from companies where lower(code) = lower(?)", originalCode))
        .isOne();
    assertThat(
            countRows("select count(*) from companies where lower(code) = lower(?)", duplicateCode))
        .isZero();
    assertThat(
            countRows("select count(*) from app_users where lower(email) = lower(?)", ownerEmail))
        .isOne();
    assertThat(countRows("select count(*) from tenant_activation_tokens", null))
        .isEqualTo(activationTokensAfterOriginal);
  }

  @Test
  void addClientRejectsOversizedNormalizedCompanyCodeWithoutSideEffects() {
    String superAdminToken = loginToken(SUPER_ADMIN_EMAIL, ROOT_COMPANY_CODE);
    HttpHeaders superAdminHeaders = headers(superAdminToken, ROOT_COMPANY_CODE);
    String oversizedCode = maxLengthCompanyCode() + "X";
    String ownerEmail = "oversized-code-" + Long.toString(System.nanoTime(), 36) + "@example.com";

    ResponseEntity<Map> response =
        rest.exchange(
            "/api/v1/superadmin/tenants",
            HttpMethod.POST,
            new HttpEntity<>(
                addClientPayload(" " + oversizedCode + " ", ownerEmail, "DRAFT"),
                superAdminHeaders),
            Map.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(
            countRows("select count(*) from companies where lower(code) = lower(?)", oversizedCode))
        .isZero();
    assertThat(
            countRows("select count(*) from app_users where lower(email) = lower(?)", ownerEmail))
        .isZero();
  }

  @Test
  void addClientRejectsOversizedNormalizedOwnerEmailWithoutTenantOrTokenSideEffects() {
    String superAdminToken = loginToken(SUPER_ADMIN_EMAIL, ROOT_COMPANY_CODE);
    HttpHeaders superAdminHeaders = headers(superAdminToken, ROOT_COMPANY_CODE);
    String code = "M4OEM" + Long.toString(System.nanoTime(), 36).toUpperCase(Locale.ROOT);
    String oversizedOwnerEmail = maxLengthOwnerEmail() + "x";
    long activationTokensBefore = countRows("select count(*) from tenant_activation_tokens", null);

    ResponseEntity<Map> response =
        rest.exchange(
            "/api/v1/superadmin/tenants",
            HttpMethod.POST,
            new HttpEntity<>(
                addClientPayload(code, " " + oversizedOwnerEmail + " ", "SEND_ACTIVATION"),
                superAdminHeaders),
            Map.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(countRows("select count(*) from companies where lower(code) = lower(?)", code))
        .isZero();
    assertThat(
            countRows(
                "select count(*) from app_users where lower(email) = lower(?)",
                oversizedOwnerEmail))
        .isZero();
    assertThat(countRows("select count(*) from tenant_activation_tokens", null))
        .isEqualTo(activationTokensBefore);
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
                  + "&page=0&size=20&sort=companyCode,asc&includeArchived=true",
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

  private HttpHeaders jsonHeaders() {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    return headers;
  }

  private String loginToken(String email, String companyCode) {
    return loginToken(email, companyCode, LOGIN_CREDENTIAL);
  }

  private String loginToken(String email, String companyCode, String password) {
    Map<String, Object> request =
        Map.of(
            "email", email,
            "password", password,
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

  private Map<String, Object> infraCostPayload(
      String component, long amountMinorUnits, String reason) {
    return Map.of(
        "component",
        component,
        "periodStartAt",
        "2026-04-01T00:00:00Z",
        "periodEndAt",
        "2026-05-01T00:00:00Z",
        "amountMinorUnits",
        amountMinorUnits,
        "currency",
        "INR",
        "source",
        "Manual platform estimate",
        "reason",
        reason,
        "notes",
        "Safe aggregate infra cost");
  }

  private void assertInfraCostAuditReason(Number auditEventId, String expectedReason) {
    assertThat(auditEventId).isNotNull();
    Integer matches =
        jdbcTemplate.queryForObject(
            "select count(*) from audit_log_metadata where audit_log_id = ? and metadata_key ="
                + " 'reason' and metadata_value = ?",
            Integer.class,
            auditEventId.longValue(),
            expectedReason);
    assertThat(matches).isEqualTo(1);
  }

  private String maxLengthCompanyCode() {
    String unique = "M4B" + Long.toString(System.nanoTime(), 36).toUpperCase(Locale.ROOT);
    return (unique + "X".repeat(32)).substring(0, 32);
  }

  private String maxLengthOwnerEmail() {
    String uniqueLocal = "m4b" + Long.toString(System.nanoTime(), 36).toLowerCase(Locale.ROOT);
    String domain = "example.com";
    String local = uniqueLocal + "x".repeat(64 - uniqueLocal.length());
    String base = local + "@" + domain;
    return base + "y".repeat(255 - base.length());
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

  private Long countRows(String sql, Object value) {
    if (value == null) {
      return jdbcTemplate.queryForObject(sql, Long.class);
    }
    return jdbcTemplate.queryForObject(sql, Long.class, value);
  }

  private Long countRows(String sql, Object firstValue, Object secondValue) {
    return jdbcTemplate.queryForObject(sql, Long.class, firstValue, secondValue);
  }

  private Long countRows(
      String sql, Object firstValue, Object secondValue, Object thirdValue, Object fourthValue) {
    return jdbcTemplate.queryForObject(
        sql, Long.class, firstValue, secondValue, thirdValue, fourthValue);
  }

  private String activationDigest(String token) {
    return AuthTokenDigests.tenantActivationTokenDigest(token);
  }
}
