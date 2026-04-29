package com.bigbrightpaints.erp.modules.company;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

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

class SuperAdminBillingIT extends AbstractIntegrationTest {

  private static final String ROOT_COMPANY_CODE = "BILLROOT";
  private static final String SUPER_ADMIN_EMAIL = "billing-superadmin@bbp.com";
  private static final String PASSWORD = "admin123";

  @Autowired private TestRestTemplate rest;
  @Autowired private JdbcTemplate jdbcTemplate;

  private HttpHeaders superAdminHeaders;

  @BeforeEach
  void seedSuperAdmin() {
    dataSeeder.ensureUser(
        SUPER_ADMIN_EMAIL,
        PASSWORD,
        "Billing Super Admin",
        ROOT_COMPANY_CODE,
        List.of("ROLE_SUPER_ADMIN", "ROLE_ADMIN"));
    superAdminHeaders = headers(loginToken(SUPER_ADMIN_EMAIL, ROOT_COMPANY_CODE));
  }

  @Test
  void subscriptionCanBeCreatedReadAndRejectsDuplicateActiveSubscription() {
    Long tenantId = createTenant("M10SUB", "STARTER", "MANUAL");
    Map<String, Object> payload =
        subscriptionPayload("STARTER", "MONTHLY", 25_000L, "ACTIVE", "INR", "2026-05-15T00:00:00Z");

    ResponseEntity<Map> createResponse =
        rest.exchange(
            "/api/v1/superadmin/tenants/" + tenantId + "/billing/subscription",
            HttpMethod.POST,
            new HttpEntity<>(payload, superAdminHeaders),
            Map.class);

    assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    @SuppressWarnings("unchecked")
    Map<String, Object> subscription = (Map<String, Object>) createResponse.getBody().get("data");
    assertThat(subscription)
        .containsEntry("tenantId", tenantId.intValue())
        .containsEntry("planId", "STARTER")
        .containsEntry("status", "ACTIVE")
        .containsEntry("cadence", "MONTHLY")
        .containsEntry("amountMinorUnits", 25_000)
        .containsEntry("currency", "INR")
        .containsEntry("collectionMode", "MANUAL");
    assertThat(subscription)
        .containsKeys(
            "subscriptionId",
            "periodStartAt",
            "periodEndAt",
            "renewalAt",
            "dueAt",
            "trialStartAt",
            "trialEndAt",
            "graceUntilAt",
            "canceledAt",
            "archivedAt",
            "externalReference",
            "billingStatus",
            "auditEventId");
    assertAuditReason((Number) subscription.get("auditEventId"), "billing-subscription-created");

    ResponseEntity<Map> readResponse =
        rest.exchange(
            "/api/v1/superadmin/tenants/" + tenantId + "/billing/subscription",
            HttpMethod.GET,
            new HttpEntity<>(superAdminHeaders),
            Map.class);
    assertThat(readResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(readResponse.getBody().get("data")).isEqualTo(subscription);

    ResponseEntity<Map> duplicateResponse =
        rest.exchange(
            "/api/v1/superadmin/tenants/" + tenantId + "/billing/subscription",
            HttpMethod.POST,
            new HttpEntity<>(payload, superAdminHeaders),
            Map.class);
    assertThat(duplicateResponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(countRows("super_admin_billing_subscriptions", tenantId)).isEqualTo(1L);
  }

  @Test
  void manualLedgerIsImmutableIdempotentMathematicalAndPrivacySafe() {
    Long tenantId = createTenant("M10LED", "GROWTH", "MANUAL");
    createSubscription(tenantId, subscriptionPayload("GROWTH", "MONTHLY", 50_000L, "ACTIVE"));

    Map<String, Object> invoiceRequest =
        ledgerPayload(tenantId, "invoice", 120_000L, "INR", "Manual May invoice");
    ResponseEntity<Map> invoiceResponse = postLedger(tenantId, "invoices", invoiceRequest);
    assertThat(invoiceResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    @SuppressWarnings("unchecked")
    Map<String, Object> invoice = (Map<String, Object>) invoiceResponse.getBody().get("data");
    assertThat(invoice)
        .containsEntry("entryType", "INVOICE")
        .containsEntry("direction", "DEBIT")
        .containsEntry("balanceAfterMinorUnits", 120_000)
        .containsEntry("billingStatusAfter", "DUE");

    ResponseEntity<Map> replayInvoice = postLedger(tenantId, "invoices", invoiceRequest);
    assertThat(replayInvoice.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(replayInvoice.getBody().get("data")).isEqualTo(invoice);
    assertThat(countRows("super_admin_billing_ledger_entries", tenantId)).isEqualTo(1L);

    ResponseEntity<Map> zeroInvoice =
        postLedger(
            tenantId,
            "invoices",
            ledgerPayload(tenantId, "zero", 0L, "INR", "Zero invoice blocked"));
    assertThat(zeroInvoice.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

    ResponseEntity<Map> currencyMismatch =
        postLedger(
            tenantId,
            "payments",
            ledgerPayload(tenantId, "currency", 1_000L, "USD", "Wrong currency"));
    assertThat(currencyMismatch.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

    ResponseEntity<Map> paymentResponse =
        postLedger(
            tenantId,
            "payments",
            ledgerPayload(tenantId, "payment", 60_000L, "INR", "UPI payment received"));
    assertThat(paymentResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    @SuppressWarnings("unchecked")
    Map<String, Object> payment = (Map<String, Object>) paymentResponse.getBody().get("data");
    assertThat(payment)
        .containsEntry("entryType", "PAYMENT")
        .containsEntry("direction", "CREDIT")
        .containsEntry("balanceBeforeMinorUnits", 120_000)
        .containsEntry("balanceAfterMinorUnits", 60_000);

    ResponseEntity<Map> adjustmentResponse =
        postLedger(
            tenantId,
            "adjustments",
            adjustmentPayload(tenantId, "credit-adjustment", 60_000L, "INR", "CREDIT"));
    assertThat(adjustmentResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    @SuppressWarnings("unchecked")
    Map<String, Object> adjustment = (Map<String, Object>) adjustmentResponse.getBody().get("data");
    assertThat(adjustment)
        .containsEntry("entryType", "ADJUSTMENT")
        .containsEntry("direction", "CREDIT")
        .containsEntry("balanceAfterMinorUnits", 0)
        .containsEntry("billingStatusAfter", "PAID");
    assertAuditReason((Number) adjustment.get("auditEventId"), "billing-ledger-adjustment-created");

    ResponseEntity<Map> ledgerResponse =
        rest.exchange(
            "/api/v1/superadmin/tenants/" + tenantId + "/billing/ledger",
            HttpMethod.GET,
            new HttpEntity<>(superAdminHeaders),
            Map.class);
    assertThat(ledgerResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    @SuppressWarnings("unchecked")
    Map<String, Object> ledger = (Map<String, Object>) ledgerResponse.getBody().get("data");
    assertThat(ledger)
        .containsEntry("tenantId", tenantId.intValue())
        .containsEntry("balanceDueMinorUnits", 0)
        .containsEntry("currency", "INR")
        .containsEntry("billingStatus", "PAID");
    assertThat(ledger.toString().toLowerCase(Locale.ROOT))
        .doesNotContain("customerinvoice", "gstreturn", "journal", "inventory", "salary");

    ResponseEntity<Map> profileResponse =
        rest.exchange(
            "/api/v1/superadmin/tenants/" + tenantId,
            HttpMethod.GET,
            new HttpEntity<>(superAdminHeaders),
            Map.class);
    assertThat(profileResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    @SuppressWarnings("unchecked")
    Map<String, Object> profile = (Map<String, Object>) profileResponse.getBody().get("data");
    @SuppressWarnings("unchecked")
    Map<String, Object> billing = (Map<String, Object>) profile.get("billing");
    assertThat(billing)
        .containsEntry("billingStatus", "PAID")
        .containsEntry("balanceDueMinorUnits", 0)
        .containsEntry("currency", "INR");
    assertThat(profile.toString().toLowerCase(Locale.ROOT))
        .doesNotContain("ledgerentry", "gst return", "private invoice");
  }

  @Test
  void metricsUseMinorUnitsCadenceRoundingCurrencyGroupingAndLifecycleExclusions() {
    Long monthlyTenant = createTenant("M10MRR", "STARTER", "MANUAL");
    createSubscription(
        monthlyTenant, subscriptionPayload("STARTER", "MONTHLY", 10_000L, "ACTIVE", "USD"));
    Long annualTenant = createTenant("M10ARR", "ENTERPRISE", "MANUAL");
    createSubscription(
        annualTenant, subscriptionPayload("ENTERPRISE", "ANNUAL", 120_001L, "ACTIVE", "USD"));
    Long canceledTenant = createTenant("M10CAN", "GROWTH", "MANUAL");
    createSubscription(
        canceledTenant, subscriptionPayload("GROWTH", "MONTHLY", 99_999L, "CANCELED", "USD"));

    ResponseEntity<Map> response =
        rest.exchange(
            "/api/v1/superadmin/billing/metrics",
            HttpMethod.GET,
            new HttpEntity<>(superAdminHeaders),
            Map.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    @SuppressWarnings("unchecked")
    Map<String, Object> metrics = (Map<String, Object>) response.getBody().get("data");
    @SuppressWarnings("unchecked")
    Map<String, Object> usd = (Map<String, Object>) metrics.get("USD");
    assertThat(usd)
        .containsEntry("currency", "USD")
        .containsEntry("mrrMinorUnits", 20_000)
        .containsEntry("arrMinorUnits", 240_000)
        .containsEntry("activeSubscriptionCount", 2)
        .containsEntry("excludedSubscriptionCount", 1)
        .containsEntry("annualToMonthlyRoundingPolicy", "HALF_UP_TO_MINOR_UNIT");

    ResponseEntity<Map> dashboard =
        rest.exchange(
            "/api/v1/superadmin/dashboard",
            HttpMethod.GET,
            new HttpEntity<>(superAdminHeaders),
            Map.class);
    assertThat(dashboard.getStatusCode()).isEqualTo(HttpStatus.OK);
    @SuppressWarnings("unchecked")
    Map<String, Object> dashboardData = (Map<String, Object>) dashboard.getBody().get("data");
    assertThat(((Number) dashboardData.get("mrrMinorUnits")).longValue())
        .isGreaterThanOrEqualTo(20_000L);
    assertThat(((Number) dashboardData.get("arrMinorUnits")).longValue())
        .isGreaterThanOrEqualTo(240_000L);
  }

  private Long createTenant(String prefix, String planId, String billingStatus) {
    String code =
        (prefix + Long.toString(System.nanoTime(), 36) + "ZZZZZZZZ")
            .toUpperCase(Locale.ROOT)
            .substring(0, 20);
    String ownerEmail = "owner-" + code.toLowerCase(Locale.ROOT) + "@example.com";
    ResponseEntity<Map> response =
        rest.exchange(
            "/api/v1/superadmin/tenants",
            HttpMethod.POST,
            new HttpEntity<>(
                addClientPayload(code, ownerEmail, planId, billingStatus), superAdminHeaders),
            Map.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    @SuppressWarnings("unchecked")
    Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
    return ((Number) data.get("tenantId")).longValue();
  }

  private Map<String, Object> addClientPayload(
      String code, String ownerEmail, String planId, String billingStatus) {
    return Map.of(
        "company",
        Map.of(
            "name",
            "M10 Billing Client",
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
            planId,
            "billingStatus",
            billingStatus,
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
        Map.of("notes", "safe note", "tags", List.of("M10")),
        "createMode",
        "DRAFT");
  }

  private Map<String, Object> subscriptionPayload(
      String planId, String cadence, long amountMinorUnits, String status) {
    return subscriptionPayload(
        planId, cadence, amountMinorUnits, status, "INR", "2026-05-15T00:00:00Z");
  }

  private Map<String, Object> subscriptionPayload(
      String planId, String cadence, long amountMinorUnits, String status, String currency) {
    return subscriptionPayload(
        planId, cadence, amountMinorUnits, status, currency, "2026-05-15T00:00:00Z");
  }

  private Map<String, Object> subscriptionPayload(
      String planId,
      String cadence,
      long amountMinorUnits,
      String status,
      String currency,
      String dueAt) {
    return Map.ofEntries(
        Map.entry("planId", planId),
        Map.entry("status", status),
        Map.entry("cadence", cadence),
        Map.entry("amountMinorUnits", amountMinorUnits),
        Map.entry("currency", currency),
        Map.entry("collectionMode", "MANUAL"),
        Map.entry("periodStartAt", "2026-05-01T00:00:00Z"),
        Map.entry("periodEndAt", "2026-05-31T23:59:59Z"),
        Map.entry("renewalAt", "2026-06-01T00:00:00Z"),
        Map.entry("dueAt", dueAt),
        Map.entry("trialStartAt", "2026-05-01T00:00:00Z"),
        Map.entry("trialEndAt", "2026-05-15T00:00:00Z"),
        Map.entry("graceUntilAt", "2026-05-22T00:00:00Z"),
        Map.entry("externalReference", "REF-" + UUID.randomUUID()),
        Map.entry("reason", "M10 subscription test"));
  }

  private Map<String, Object> ledgerPayload(
      Long tenantId, String marker, long amountMinorUnits, String currency, String reason) {
    return Map.of(
        "amountMinorUnits",
        amountMinorUnits,
        "currency",
        currency,
        "reason",
        reason,
        "idempotencyKey",
        "idem-" + tenantId + "-" + marker,
        "externalReference",
        "EXT-" + marker);
  }

  private Map<String, Object> adjustmentPayload(
      Long tenantId, String marker, long amountMinorUnits, String currency, String direction) {
    return Map.of(
        "amountMinorUnits",
        amountMinorUnits,
        "currency",
        currency,
        "direction",
        direction,
        "reason",
        "Manual billing adjustment",
        "idempotencyKey",
        "idem-" + tenantId + "-" + marker,
        "externalReference",
        "EXT-" + marker);
  }

  private void createSubscription(Long tenantId, Map<String, Object> payload) {
    ResponseEntity<Map> response =
        rest.exchange(
            "/api/v1/superadmin/tenants/" + tenantId + "/billing/subscription",
            HttpMethod.POST,
            new HttpEntity<>(payload, superAdminHeaders),
            Map.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
  }

  private ResponseEntity<Map> postLedger(
      Long tenantId, String action, Map<String, Object> request) {
    return rest.exchange(
        "/api/v1/superadmin/tenants/" + tenantId + "/billing/" + action,
        HttpMethod.POST,
        new HttpEntity<>(request, superAdminHeaders),
        Map.class);
  }

  private HttpHeaders headers(String token) {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("X-Company-Code", ROOT_COMPANY_CODE);
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

  private void assertAuditReason(Number auditEventId, String expectedReason) {
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

  private Long countRows(String tableName, Long companyId) {
    return jdbcTemplate.queryForObject(
        "select count(*) from " + tableName + " where company_id = ?", Long.class, companyId);
  }
}
