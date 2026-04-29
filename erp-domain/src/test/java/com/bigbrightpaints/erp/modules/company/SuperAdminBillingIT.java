package com.bigbrightpaints.erp.modules.company;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
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
    long auditRowsAfterReplay = countBillingLedgerAuditEvents(tenantId);

    ResponseEntity<Map> endpointConflict = postLedger(tenantId, "payments", invoiceRequest);
    assertThat(endpointConflict.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(countRows("super_admin_billing_ledger_entries", tenantId)).isEqualTo(1L);
    assertThat(countBillingLedgerAuditEvents(tenantId)).isEqualTo(auditRowsAfterReplay);

    Map<String, Object> amountConflictRequest = new LinkedHashMap<>(invoiceRequest);
    amountConflictRequest.put("amountMinorUnits", 121_000L);
    ResponseEntity<Map> amountConflict = postLedger(tenantId, "invoices", amountConflictRequest);
    assertThat(amountConflict.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(countRows("super_admin_billing_ledger_entries", tenantId)).isEqualTo(1L);
    assertThat(countBillingLedgerAuditEvents(tenantId)).isEqualTo(auditRowsAfterReplay);

    Map<String, Object> currencyConflictRequest = new LinkedHashMap<>(invoiceRequest);
    currencyConflictRequest.put("currency", "USD");
    ResponseEntity<Map> currencyConflict =
        postLedger(tenantId, "invoices", currencyConflictRequest);
    assertThat(currencyConflict.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(countRows("super_admin_billing_ledger_entries", tenantId)).isEqualTo(1L);
    assertThat(countBillingLedgerAuditEvents(tenantId)).isEqualTo(auditRowsAfterReplay);

    Map<String, Object> reasonConflictRequest = new LinkedHashMap<>(invoiceRequest);
    reasonConflictRequest.put("reason", "Changed May invoice reason");
    ResponseEntity<Map> reasonConflict = postLedger(tenantId, "invoices", reasonConflictRequest);
    assertThat(reasonConflict.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(countRows("super_admin_billing_ledger_entries", tenantId)).isEqualTo(1L);
    assertThat(countBillingLedgerAuditEvents(tenantId)).isEqualTo(auditRowsAfterReplay);

    Map<String, Object> referenceConflictRequest = new LinkedHashMap<>(invoiceRequest);
    referenceConflictRequest.put("externalReference", "EXT-invoice-changed");
    ResponseEntity<Map> referenceConflict =
        postLedger(tenantId, "invoices", referenceConflictRequest);
    assertThat(referenceConflict.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(countRows("super_admin_billing_ledger_entries", tenantId)).isEqualTo(1L);
    assertThat(countBillingLedgerAuditEvents(tenantId)).isEqualTo(auditRowsAfterReplay);

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
    long rowsAfterAdjustment = countRows("super_admin_billing_ledger_entries", tenantId);
    long auditRowsAfterAdjustment = countBillingLedgerAuditEvents(tenantId);

    Map<String, Object> directionConflictRequest =
        new LinkedHashMap<>(
            adjustmentPayload(tenantId, "credit-adjustment", 60_000L, "INR", "DEBIT"));
    ResponseEntity<Map> directionConflict =
        postLedger(tenantId, "adjustments", directionConflictRequest);
    assertThat(directionConflict.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(countRows("super_admin_billing_ledger_entries", tenantId))
        .isEqualTo(rowsAfterAdjustment);
    assertThat(countBillingLedgerAuditEvents(tenantId)).isEqualTo(auditRowsAfterAdjustment);

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
    Instant now = Instant.now();
    String currentStart = now.minusSeconds(86_400).toString();
    String currentEnd = now.plusSeconds(2_592_000).toString();
    String futureStart = now.plusSeconds(2_592_000).toString();
    String futureEnd = now.plusSeconds(5_184_000).toString();
    String pastStart = now.minusSeconds(5_184_000).toString();
    String pastEnd = now.minusSeconds(86_400).toString();
    String dueAt = now.plusSeconds(86_400).toString();

    Long monthlyTenant = createTenant("M10MRR", "STARTER", "MANUAL");
    createSubscription(
        monthlyTenant,
        subscriptionPayload(
            "STARTER", "MONTHLY", 10_000L, "ACTIVE", "USD", dueAt, currentStart, currentEnd));
    Long annualTenant = createTenant("M10ARR", "ENTERPRISE", "MANUAL");
    createSubscription(
        annualTenant,
        subscriptionPayload(
            "ENTERPRISE", "ANNUAL", 120_001L, "ACTIVE", "USD", dueAt, currentStart, currentEnd));
    Long manualTenant = createTenant("M10MAN", "CUSTOM", "MANUAL");
    createSubscription(
        manualTenant,
        subscriptionPayload(
            "CUSTOM", "MONTHLY", 3_000L, "MANUAL", "USD", dueAt, currentStart, currentEnd));
    Long futureTenant = createTenant("M10FUT", "GROWTH", "MANUAL");
    createSubscription(
        futureTenant,
        subscriptionPayload(
            "GROWTH", "MONTHLY", 99_999L, "ACTIVE", "USD", dueAt, futureStart, futureEnd));
    Long endedTenant = createTenant("M10END", "GROWTH", "MANUAL");
    createSubscription(
        endedTenant,
        subscriptionPayload(
            "GROWTH", "MONTHLY", 88_888L, "ACTIVE", "USD", dueAt, pastStart, pastEnd));
    Long canceledTenant = createTenant("M10CAN", "GROWTH", "MANUAL");
    createSubscription(
        canceledTenant,
        subscriptionPayload(
            "GROWTH", "MONTHLY", 77_777L, "CANCELED", "USD", dueAt, currentStart, currentEnd));

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
        .containsEntry("mrrMinorUnits", 23_000)
        .containsEntry("arrMinorUnits", 276_000)
        .containsEntry("activeSubscriptionCount", 3)
        .containsEntry("excludedSubscriptionCount", 3)
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
        .isGreaterThanOrEqualTo(23_000L);
    assertThat(((Number) dashboardData.get("arrMinorUnits")).longValue())
        .isGreaterThanOrEqualTo(276_000L);
  }

  @Test
  void commercialStateSuspensionResumeCancelArchiveMatrixIsAuditedAndListAware() {
    Long tenantId = createTenant("M10SUS", "GROWTH", "DUE");
    createSubscription(
        tenantId,
        subscriptionPayload("GROWTH", "MONTHLY", 75_000L, "ACTIVE", "INR", "2026-05-15T00:00:00Z"));
    postLedger(
        tenantId,
        "invoices",
        ledgerPayload(tenantId, "suspension-invoice", 75_000L, "INR", "Suspension invoice"));

    Map<String, Object> grace =
        postCommercialAction(
            tenantId,
            "suspension/grace",
            Map.of("reason", "Payment due reminder", "graceUntilAt", "2026-05-20T00:00:00Z"));
    assertCommercialState(grace, "GRACE", "ACTIVE", "ACTIVE", "GRACE");
    assertThat(grace).containsEntry("safeReadsAllowed", true).containsEntry("writesAllowed", true);
    assertAuditReason((Number) grace.get("auditEventId"), "commercial-state-grace-started");

    Map<String, Object> readOnly =
        postCommercialAction(
            tenantId,
            "suspension/read-only",
            Map.of("reason", "Grace expired read only", "effectiveAt", "2026-05-21T00:00:00Z"));
    assertCommercialState(readOnly, "SUSPENDED_READ_ONLY", "SUSPENDED", "HOLD", "OVERDUE");
    assertThat(readOnly)
        .containsEntry("loginAllowed", true)
        .containsEntry("safeReadsAllowed", true)
        .containsEntry("writesAllowed", false)
        .containsEntry("backgroundWorkAllowed", false);
    assertAuditReason(
        (Number) readOnly.get("auditEventId"), "commercial-state-suspended-read-only");

    Map<String, Object> blocked =
        postCommercialAction(
            tenantId,
            "suspension/blocked",
            Map.of("reason", "Payment still overdue", "effectiveAt", "2026-05-21T00:00:00Z"));
    assertCommercialState(blocked, "SUSPENDED_BLOCKED", "SUSPENDED", "BLOCKED", "OVERDUE");
    assertThat(blocked)
        .containsEntry("loginAllowed", false)
        .containsEntry("safeReadsAllowed", false)
        .containsEntry("writesAllowed", false)
        .containsEntry("backgroundWorkAllowed", false);

    postLedger(
        tenantId,
        "payments",
        ledgerPayload(tenantId, "suspension-payment", 75_000L, "INR", "Suspension payment"));
    Map<String, Object> resumed =
        postCommercialAction(tenantId, "resume", Map.of("reason", "Payment promise accepted"));
    assertCommercialState(resumed, "ACTIVE", "ACTIVE", "ACTIVE", "PAID");
    assertThat(resumed)
        .containsEntry("writesAllowed", true)
        .containsEntry("backgroundWorkAllowed", true);

    Map<String, Object> canceled =
        postCommercialAction(
            tenantId, "cancel", Map.of("reason", "Customer requested cancellation"));
    assertCommercialState(canceled, "CANCELED", "DEACTIVATED", "BLOCKED", "CANCELED");
    assertAuditReason((Number) canceled.get("auditEventId"), "commercial-state-canceled");

    Map<String, Object> archived =
        postCommercialAction(tenantId, "archive", Map.of("reason", "History-only archive"));
    assertCommercialState(archived, "ARCHIVED", "DEACTIVATED", "BLOCKED", "ARCHIVED");
    assertThat(archived).containsEntry("defaultListIncluded", false);

    ResponseEntity<Map> defaultList =
        rest.exchange(
            "/api/v1/superadmin/tenants?q=" + archived.get("tenantCode"),
            HttpMethod.GET,
            new HttpEntity<>(superAdminHeaders),
            Map.class);
    assertThat(defaultList.getStatusCode()).isEqualTo(HttpStatus.OK);
    @SuppressWarnings("unchecked")
    Map<String, Object> defaultPage = (Map<String, Object>) defaultList.getBody().get("data");
    assertThat(defaultPage.get("totalElements")).isEqualTo(0);

    ResponseEntity<Map> includeArchivedList =
        rest.exchange(
            "/api/v1/superadmin/tenants?q=" + archived.get("tenantCode") + "&includeArchived=true",
            HttpMethod.GET,
            new HttpEntity<>(superAdminHeaders),
            Map.class);
    assertThat(includeArchivedList.getStatusCode()).isEqualTo(HttpStatus.OK);
    @SuppressWarnings("unchecked")
    Map<String, Object> archivedPage =
        (Map<String, Object>) includeArchivedList.getBody().get("data");
    assertThat(archivedPage.get("totalElements")).isEqualTo(1);
  }

  @Test
  void futureCancelAndArchiveStayScheduledUntilEffectiveInstantThenApplyDeterministically()
      throws Exception {
    Long cancelTenant = createTenant("M10FCA", "GROWTH", "DUE");
    createSubscription(cancelTenant, subscriptionPayload("GROWTH", "MONTHLY", 75_000L, "ACTIVE"));
    long mrrBefore = usdMrrMinorUnits();
    Instant futureCancelAt = Instant.now().plusSeconds(60);

    Map<String, Object> scheduledCancel =
        postCommercialAction(
            cancelTenant,
            "cancel",
            Map.of(
                "reason",
                "Schedule future cancellation",
                "effectiveAt",
                futureCancelAt.toString()));

    assertCommercialState(scheduledCancel, "ACTIVE", "ACTIVE", "ACTIVE", "PAID");
    assertThat(scheduledCancel)
        .containsEntry("effectiveAt", futureCancelAt.toString())
        .containsEntry("canceledAt", futureCancelAt.toString())
        .containsEntry("loginAllowed", true)
        .containsEntry("writesAllowed", true);
    assertThat(usdMrrMinorUnits()).isEqualTo(mrrBefore);
    assertSubscriptionStatus(cancelTenant, "ACTIVE", "PAID");

    ResponseEntity<Map> conflictingArchive =
        postCommercialActionRaw(
            cancelTenant, "archive", Map.of("reason", "Cannot archive while cancel is pending"));
    assertThat(conflictingArchive.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertSubscriptionStatus(cancelTenant, "ACTIVE", "PAID");

    Long dueCancelTenant = createTenant("M10DCA", "GROWTH", "DUE");
    createSubscription(
        dueCancelTenant, subscriptionPayload("GROWTH", "MONTHLY", 75_000L, "ACTIVE"));
    Instant dueCancelAt = Instant.now().plusSeconds(2);
    Map<String, Object> scheduledDueCancel =
        postCommercialAction(
            dueCancelTenant,
            "cancel",
            Map.of("reason", "Due cancellation", "effectiveAt", dueCancelAt.toString()));
    assertCommercialState(scheduledDueCancel, "ACTIVE", "ACTIVE", "ACTIVE", "PAID");
    Thread.sleep(2_250);
    Map<String, Object> appliedCancel = getCommercialState(dueCancelTenant);
    assertCommercialState(appliedCancel, "CANCELED", "DEACTIVATED", "BLOCKED", "CANCELED");

    Long archiveTenant = createTenant("M10FAR", "GROWTH", "DUE");
    createSubscription(archiveTenant, subscriptionPayload("GROWTH", "MONTHLY", 75_000L, "ACTIVE"));
    Instant archiveAt = Instant.now().plusSeconds(2);
    Map<String, Object> scheduledArchive =
        postCommercialAction(
            archiveTenant,
            "archive",
            Map.of("reason", "Schedule archive", "effectiveAt", archiveAt.toString()));
    assertCommercialState(scheduledArchive, "ACTIVE", "ACTIVE", "ACTIVE", "PAID");
    assertThat(defaultListCount((String) scheduledArchive.get("tenantCode"), false)).isEqualTo(1);
    Thread.sleep(2_250);
    Map<String, Object> appliedArchive = getCommercialState(archiveTenant);
    assertCommercialState(appliedArchive, "ARCHIVED", "DEACTIVATED", "BLOCKED", "ARCHIVED");
    assertThat(defaultListCount((String) appliedArchive.get("tenantCode"), false)).isZero();
    assertThat(defaultListCount((String) appliedArchive.get("tenantCode"), true)).isEqualTo(1);
  }

  @Test
  void lifecycleActionRepeatsAreNoOpAndConflictsDoNotMutateStateOrAudit() {
    Long tenantId = createTenant("M10IDM", "GROWTH", "DUE");
    createSubscription(
        tenantId,
        subscriptionPayload("GROWTH", "MONTHLY", 75_000L, "ACTIVE", "INR", "2026-05-15T00:00:00Z"));
    postLedger(
        tenantId,
        "invoices",
        ledgerPayload(tenantId, "idempotency-invoice", 75_000L, "INR", "Idempotency invoice"));

    Map<String, Object> graceRequest =
        Map.of("reason", "Grace idempotency", "graceUntilAt", "2026-05-20T00:00:00Z");
    Map<String, Object> firstGrace =
        postCommercialAction(tenantId, "suspension/grace", graceRequest);
    long auditAfterFirstGrace = countCommercialLifecycleAuditEvents(tenantId);
    Map<String, Object> replayGrace =
        postCommercialAction(tenantId, "suspension/grace", graceRequest);
    assertThat(replayGrace.get("auditEventId")).isEqualTo(firstGrace.get("auditEventId"));
    assertThat(countCommercialLifecycleAuditEvents(tenantId)).isEqualTo(auditAfterFirstGrace);

    ResponseEntity<Map> conflictingGrace =
        postCommercialActionRaw(
            tenantId,
            "suspension/grace",
            Map.of("reason", "Different grace", "graceUntilAt", "2026-05-21T00:00:00Z"));
    assertThat(conflictingGrace.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(countCommercialLifecycleAuditEvents(tenantId)).isEqualTo(auditAfterFirstGrace);
    assertCommercialState(getCommercialState(tenantId), "GRACE", "ACTIVE", "ACTIVE", "GRACE");

    Map<String, Object> readOnlyRequest =
        Map.of("reason", "Read-only idempotency", "effectiveAt", "2026-05-21T00:00:00Z");
    Map<String, Object> firstReadOnly =
        postCommercialAction(tenantId, "suspension/read-only", readOnlyRequest);
    long auditAfterReadOnly = countCommercialLifecycleAuditEvents(tenantId);
    Map<String, Object> replayReadOnly =
        postCommercialAction(tenantId, "suspension/read-only", readOnlyRequest);
    assertThat(replayReadOnly.get("auditEventId")).isEqualTo(firstReadOnly.get("auditEventId"));
    assertThat(countCommercialLifecycleAuditEvents(tenantId)).isEqualTo(auditAfterReadOnly);

    Map<String, Object> blockedRequest =
        Map.of("reason", "Blocked idempotency", "effectiveAt", "2026-05-22T00:00:00Z");
    Map<String, Object> firstBlocked =
        postCommercialAction(tenantId, "suspension/blocked", blockedRequest);
    long auditAfterBlocked = countCommercialLifecycleAuditEvents(tenantId);
    Map<String, Object> replayBlocked =
        postCommercialAction(tenantId, "suspension/blocked", blockedRequest);
    assertThat(replayBlocked.get("auditEventId")).isEqualTo(firstBlocked.get("auditEventId"));
    assertThat(countCommercialLifecycleAuditEvents(tenantId)).isEqualTo(auditAfterBlocked);

    postLedger(
        tenantId,
        "payments",
        ledgerPayload(tenantId, "idempotency-payment", 75_000L, "INR", "Idempotency payment"));
    Map<String, Object> resumeRequest = Map.of("reason", "Resume idempotency");
    Map<String, Object> firstResume = postCommercialAction(tenantId, "resume", resumeRequest);
    long auditAfterResume = countCommercialLifecycleAuditEvents(tenantId);
    Map<String, Object> replayResume = postCommercialAction(tenantId, "resume", resumeRequest);
    assertThat(replayResume.get("auditEventId")).isEqualTo(firstResume.get("auditEventId"));
    assertThat(countCommercialLifecycleAuditEvents(tenantId)).isEqualTo(auditAfterResume);

    Map<String, Object> archiveRequest = Map.of("reason", "Archive idempotency");
    Map<String, Object> firstArchive = postCommercialAction(tenantId, "archive", archiveRequest);
    long auditAfterArchive = countCommercialLifecycleAuditEvents(tenantId);
    Map<String, Object> replayArchive = postCommercialAction(tenantId, "archive", archiveRequest);
    assertThat(replayArchive.get("auditEventId")).isEqualTo(firstArchive.get("auditEventId"));
    assertThat(countCommercialLifecycleAuditEvents(tenantId)).isEqualTo(auditAfterArchive);

    Long futureCancelTenant = createTenant("M10IDC", "GROWTH", "DUE");
    createSubscription(
        futureCancelTenant, subscriptionPayload("GROWTH", "MONTHLY", 75_000L, "ACTIVE", "INR"));
    Map<String, Object> cancelRequest =
        Map.of(
            "reason",
            "Future cancel idempotency",
            "effectiveAt",
            Instant.now().plusSeconds(60).toString());
    Map<String, Object> firstCancel =
        postCommercialAction(futureCancelTenant, "cancel", cancelRequest);
    long auditAfterCancel = countCommercialLifecycleAuditEvents(futureCancelTenant);
    Map<String, Object> replayCancel =
        postCommercialAction(futureCancelTenant, "cancel", cancelRequest);
    assertThat(replayCancel.get("auditEventId")).isEqualTo(firstCancel.get("auditEventId"));
    assertThat(countCommercialLifecycleAuditEvents(futureCancelTenant)).isEqualTo(auditAfterCancel);

    ResponseEntity<Map> conflictingCancel =
        postCommercialActionRaw(
            futureCancelTenant,
            "cancel",
            Map.of(
                "reason",
                "Different future cancel",
                "effectiveAt",
                Instant.now().plusSeconds(120).toString()));
    assertThat(conflictingCancel.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(countCommercialLifecycleAuditEvents(futureCancelTenant)).isEqualTo(auditAfterCancel);
    assertCommercialState(
        getCommercialState(futureCancelTenant), "ACTIVE", "ACTIVE", "ACTIVE", "PAID");
  }

  @Test
  void concurrentLifecycleActionsAreAtomicForIdenticalReplaysAndConflicts() throws Exception {
    Long identicalTenant = createTenant("M10ACI", "GROWTH", "DUE");
    createSubscription(
        identicalTenant,
        subscriptionPayload("GROWTH", "MONTHLY", 75_000L, "ACTIVE", "INR", "2026-05-15T00:00:00Z"));
    postLedger(
        identicalTenant,
        "invoices",
        ledgerPayload(
            identicalTenant, "atomic-identical-invoice", 75_000L, "INR", "Atomic invoice"));

    assertConcurrentIdenticalMutation(
        identicalTenant,
        "suspension/grace",
        Map.of("reason", "Atomic grace", "graceUntilAt", "2026-05-20T00:00:00Z"),
        "GRACE");
    assertConcurrentIdenticalMutation(
        identicalTenant,
        "suspension/read-only",
        Map.of("reason", "Atomic read only", "effectiveAt", "2026-05-21T00:00:00Z"),
        "SUSPENDED_READ_ONLY");
    assertConcurrentIdenticalMutation(
        identicalTenant,
        "suspension/blocked",
        Map.of("reason", "Atomic blocked", "effectiveAt", "2026-05-22T00:00:00Z"),
        "SUSPENDED_BLOCKED");

    postLedger(
        identicalTenant,
        "payments",
        ledgerPayload(
            identicalTenant, "atomic-identical-payment", 75_000L, "INR", "Atomic payment"));
    assertConcurrentIdenticalMutation(
        identicalTenant, "resume", Map.of("reason", "Atomic resume"), "ACTIVE");
    assertConcurrentIdenticalMutation(
        identicalTenant, "cancel", Map.of("reason", "Atomic cancel"), "CANCELED");
    assertConcurrentIdenticalMutation(
        identicalTenant, "archive", Map.of("reason", "Atomic archive"), "ARCHIVED");

    assertConcurrentConflictingMutation(
        "suspension/grace",
        Map.of("reason", "Conflict grace winner", "graceUntilAt", "2026-05-20T00:00:00Z"),
        Map.of("reason", "Conflict grace loser", "graceUntilAt", "2026-05-21T00:00:00Z"),
        "GRACE");
    assertConcurrentConflictingMutation(
        "suspension/read-only",
        Map.of("reason", "Conflict read only winner", "effectiveAt", "2026-05-21T00:00:00Z"),
        Map.of("reason", "Conflict read only loser", "effectiveAt", "2026-05-22T00:00:00Z"),
        "SUSPENDED_READ_ONLY");
    assertConcurrentConflictingMutation(
        "suspension/blocked",
        Map.of("reason", "Conflict blocked winner", "effectiveAt", "2026-05-22T00:00:00Z"),
        Map.of("reason", "Conflict blocked loser", "effectiveAt", "2026-05-23T00:00:00Z"),
        "SUSPENDED_BLOCKED");
    Long resumeTenant = preparedSuspendedTenant("M10ACR");
    assertConcurrentConflictingMutation(
        resumeTenant,
        "resume",
        Map.of("reason", "Conflict resume winner"),
        Map.of("reason", "Conflict resume loser"),
        "ACTIVE");
    assertConcurrentConflictingMutation(
        "cancel",
        Map.of("reason", "Conflict cancel winner"),
        Map.of("reason", "Conflict cancel loser"),
        "CANCELED");
    assertConcurrentConflictingMutation(
        "archive",
        Map.of("reason", "Conflict archive winner"),
        Map.of("reason", "Conflict archive loser"),
        "ARCHIVED");
  }

  @Test
  void concurrentFutureCancelAndArchiveSchedulingUsesTheSameAtomicReplayPath() throws Exception {
    Instant futureCancelAt = Instant.now().plusSeconds(120);
    Long futureCancelTenant = createTenant("M10AFC", "GROWTH", "DUE");
    createSubscription(
        futureCancelTenant, subscriptionPayload("GROWTH", "MONTHLY", 75_000L, "ACTIVE", "INR"));

    assertConcurrentIdenticalMutation(
        futureCancelTenant,
        "cancel",
        Map.of("reason", "Atomic future cancel", "effectiveAt", futureCancelAt.toString()),
        "ACTIVE");
    assertThat(countCommercialLifecycleAuditEvents(futureCancelTenant)).isEqualTo(1L);

    Instant conflictingCancelAt = Instant.now().plusSeconds(120);
    Long conflictingCancelTenant = createTenant("M10CFC", "GROWTH", "DUE");
    createSubscription(
        conflictingCancelTenant,
        subscriptionPayload("GROWTH", "MONTHLY", 75_000L, "ACTIVE", "INR"));
    assertConcurrentConflictForTenant(
        conflictingCancelTenant,
        "cancel",
        Map.of("reason", "Future cancel winner", "effectiveAt", conflictingCancelAt.toString()),
        Map.of(
            "reason",
            "Future cancel loser",
            "effectiveAt",
            conflictingCancelAt.plusSeconds(60).toString()),
        "ACTIVE");

    Instant futureArchiveAt = Instant.now().plusSeconds(120);
    Long futureArchiveTenant = createTenant("M10AFA", "GROWTH", "DUE");
    createSubscription(
        futureArchiveTenant, subscriptionPayload("GROWTH", "MONTHLY", 75_000L, "ACTIVE", "INR"));
    assertConcurrentIdenticalMutation(
        futureArchiveTenant,
        "archive",
        Map.of("reason", "Atomic future archive", "effectiveAt", futureArchiveAt.toString()),
        "ACTIVE");
    assertThat(countCommercialLifecycleAuditEvents(futureArchiveTenant)).isEqualTo(1L);

    Instant conflictingArchiveAt = Instant.now().plusSeconds(120);
    Long conflictingArchiveTenant = createTenant("M10CFA", "GROWTH", "DUE");
    createSubscription(
        conflictingArchiveTenant,
        subscriptionPayload("GROWTH", "MONTHLY", 75_000L, "ACTIVE", "INR"));
    assertConcurrentConflictForTenant(
        conflictingArchiveTenant,
        "archive",
        Map.of("reason", "Future archive winner", "effectiveAt", conflictingArchiveAt.toString()),
        Map.of(
            "reason",
            "Future archive loser",
            "effectiveAt",
            conflictingArchiveAt.plusSeconds(60).toString()),
        "ACTIVE");
  }

  @Test
  void billingStatusBoundaryDrivesGraceThenOverdueWithoutTimezoneDrift() {
    Long tenantId = createTenant("M10TIM", "STARTER", "DUE");
    createSubscription(
        tenantId,
        subscriptionPayload(
            "STARTER", "MONTHLY", 10_000L, "ACTIVE", "INR", "2026-05-15T00:00:00Z"));
    postLedger(
        tenantId,
        "invoices",
        ledgerPayload(tenantId, "boundary", 10_000L, "INR", "Boundary invoice"));

    Map<String, Object> oneInstantBefore =
        postCommercialAction(
            tenantId,
            "suspension/grace",
            Map.of(
                "reason",
                "Boundary check",
                "effectiveAt",
                "2026-05-19T23:59:59Z",
                "graceUntilAt",
                "2026-05-20T00:00:00Z"));
    assertThat(oneInstantBefore).containsEntry("billingStatus", "GRACE");

    Map<String, Object> atBoundary =
        postCommercialAction(
            tenantId,
            "suspension/blocked",
            Map.of("reason", "Boundary reached", "effectiveAt", "2026-05-20T00:00:00Z"));
    assertThat(atBoundary)
        .containsEntry("billingStatus", "OVERDUE")
        .containsEntry("commercialState", "SUSPENDED_BLOCKED");
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

  private Long preparedSuspendedTenant(String prefix) {
    Long tenantId = createTenant(prefix, "GROWTH", "DUE");
    createSubscription(
        tenantId,
        subscriptionPayload("GROWTH", "MONTHLY", 75_000L, "ACTIVE", "INR", "2026-05-15T00:00:00Z"));
    postLedger(
        tenantId,
        "invoices",
        ledgerPayload(
            tenantId, prefix.toLowerCase(Locale.ROOT) + "-invoice", 75_000L, "INR", "Invoice"));
    postCommercialAction(
        tenantId,
        "suspension/blocked",
        Map.of("reason", prefix + " suspended", "effectiveAt", "2026-05-22T00:00:00Z"));
    postLedger(
        tenantId,
        "payments",
        ledgerPayload(
            tenantId, prefix.toLowerCase(Locale.ROOT) + "-payment", 75_000L, "INR", "Payment"));
    return tenantId;
  }

  private void assertConcurrentIdenticalMutation(
      Long tenantId, String action, Map<String, Object> request, String expectedCommercialState)
      throws Exception {
    long auditBefore = countCommercialLifecycleAuditEvents(tenantId);
    List<ResponseEntity<Map>> responses =
        concurrentPostCommercialActions(tenantId, action, request, request);
    assertThat(responses).hasSize(2);
    assertThat(responses)
        .allSatisfy(response -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK));
    @SuppressWarnings("unchecked")
    Map<String, Object> first = (Map<String, Object>) responses.get(0).getBody().get("data");
    @SuppressWarnings("unchecked")
    Map<String, Object> second = (Map<String, Object>) responses.get(1).getBody().get("data");
    assertThat(second.get("auditEventId")).isEqualTo(first.get("auditEventId"));
    assertThat(countCommercialLifecycleAuditEvents(tenantId)).isEqualTo(auditBefore + 1);
    assertThat(getCommercialState(tenantId))
        .containsEntry("commercialState", expectedCommercialState);
  }

  private void assertConcurrentConflictingMutation(
      String action,
      Map<String, Object> winnerRequest,
      Map<String, Object> loserRequest,
      String expectedCommercialState)
      throws Exception {
    Long tenantId = createTenant("M10ACF", "GROWTH", "DUE");
    createSubscription(
        tenantId, subscriptionPayload("GROWTH", "MONTHLY", 75_000L, "ACTIVE", "INR"));
    assertConcurrentConflictForTenant(
        tenantId, action, winnerRequest, loserRequest, expectedCommercialState);
  }

  private void assertConcurrentConflictingMutation(
      Long tenantId,
      String action,
      Map<String, Object> winnerRequest,
      Map<String, Object> loserRequest,
      String expectedCommercialState)
      throws Exception {
    assertConcurrentConflictForTenant(
        tenantId, action, winnerRequest, loserRequest, expectedCommercialState);
  }

  private void assertConcurrentConflictForTenant(
      Long tenantId,
      String action,
      Map<String, Object> winnerRequest,
      Map<String, Object> loserRequest,
      String expectedCommercialState)
      throws Exception {
    long auditBefore = countCommercialLifecycleAuditEvents(tenantId);
    List<ResponseEntity<Map>> responses =
        concurrentPostCommercialActions(tenantId, action, winnerRequest, loserRequest);
    assertThat(responses).hasSize(2);
    assertThat(responses.stream().map(ResponseEntity::getStatusCode))
        .containsExactlyInAnyOrder(HttpStatus.OK, HttpStatus.CONFLICT);
    assertThat(countCommercialLifecycleAuditEvents(tenantId)).isEqualTo(auditBefore + 1);
    assertThat(getCommercialState(tenantId))
        .containsEntry("commercialState", expectedCommercialState);
  }

  private List<ResponseEntity<Map>> concurrentPostCommercialActions(
      Long tenantId, String action, Map<String, Object> first, Map<String, Object> second)
      throws Exception {
    CountDownLatch ready = new CountDownLatch(2);
    CountDownLatch start = new CountDownLatch(1);
    ExecutorService executor = Executors.newFixedThreadPool(2);
    try {
      List<Callable<ResponseEntity<Map>>> calls =
          List.of(
              concurrentCommercialCall(tenantId, action, first, ready, start),
              concurrentCommercialCall(tenantId, action, second, ready, start));
      List<Future<ResponseEntity<Map>>> futures = new ArrayList<>();
      for (Callable<ResponseEntity<Map>> call : calls) {
        futures.add(executor.submit(call));
      }
      assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
      start.countDown();
      List<ResponseEntity<Map>> responses = new ArrayList<>();
      for (Future<ResponseEntity<Map>> future : futures) {
        responses.add(future.get(15, TimeUnit.SECONDS));
      }
      return responses;
    } finally {
      executor.shutdownNow();
    }
  }

  private Callable<ResponseEntity<Map>> concurrentCommercialCall(
      Long tenantId,
      String action,
      Map<String, Object> request,
      CountDownLatch ready,
      CountDownLatch start) {
    return () -> {
      ready.countDown();
      assertThat(start.await(5, TimeUnit.SECONDS)).isTrue();
      return postCommercialActionRaw(tenantId, action, request);
    };
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
    Instant now = Instant.now();
    return subscriptionPayload(
        planId,
        cadence,
        amountMinorUnits,
        status,
        currency,
        dueAt,
        now.minusSeconds(86_400).toString(),
        now.plusSeconds(2_592_000).toString());
  }

  private Map<String, Object> subscriptionPayload(
      String planId,
      String cadence,
      long amountMinorUnits,
      String status,
      String currency,
      String dueAt,
      String periodStartAt,
      String periodEndAt) {
    return Map.ofEntries(
        Map.entry("planId", planId),
        Map.entry("status", status),
        Map.entry("cadence", cadence),
        Map.entry("amountMinorUnits", amountMinorUnits),
        Map.entry("currency", currency),
        Map.entry("collectionMode", "MANUAL"),
        Map.entry("periodStartAt", periodStartAt),
        Map.entry("periodEndAt", periodEndAt),
        Map.entry("renewalAt", periodEndAt),
        Map.entry("dueAt", dueAt),
        Map.entry("trialStartAt", periodStartAt),
        Map.entry("trialEndAt", periodStartAt),
        Map.entry("graceUntilAt", periodEndAt),
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

  private Map<String, Object> postCommercialAction(
      Long tenantId, String action, Map<String, Object> request) {
    ResponseEntity<Map> response = postCommercialActionRaw(tenantId, action, request);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    @SuppressWarnings("unchecked")
    Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
    return data;
  }

  private ResponseEntity<Map> postCommercialActionRaw(
      Long tenantId, String action, Map<String, Object> request) {
    return rest.exchange(
        "/api/v1/superadmin/tenants/" + tenantId + "/" + action,
        HttpMethod.POST,
        new HttpEntity<>(request, superAdminHeaders),
        Map.class);
  }

  private Map<String, Object> getCommercialState(Long tenantId) {
    ResponseEntity<Map> response =
        rest.exchange(
            "/api/v1/superadmin/tenants/" + tenantId + "/commercial-state",
            HttpMethod.GET,
            new HttpEntity<>(superAdminHeaders),
            Map.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    @SuppressWarnings("unchecked")
    Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
    return data;
  }

  private void assertSubscriptionStatus(
      Long tenantId, String expectedStatus, String expectedBillingStatus) {
    ResponseEntity<Map> response =
        rest.exchange(
            "/api/v1/superadmin/tenants/" + tenantId + "/billing/subscription",
            HttpMethod.GET,
            new HttpEntity<>(superAdminHeaders),
            Map.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    @SuppressWarnings("unchecked")
    Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
    assertThat(data)
        .containsEntry("status", expectedStatus)
        .containsEntry("billingStatus", expectedBillingStatus);
  }

  private long usdMrrMinorUnits() {
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
    return usd == null ? 0L : ((Number) usd.get("mrrMinorUnits")).longValue();
  }

  private int defaultListCount(String tenantCode, boolean includeArchived) {
    String url =
        "/api/v1/superadmin/tenants?q="
            + tenantCode
            + (includeArchived ? "&includeArchived=true" : "");
    ResponseEntity<Map> response =
        rest.exchange(url, HttpMethod.GET, new HttpEntity<>(superAdminHeaders), Map.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    @SuppressWarnings("unchecked")
    Map<String, Object> page = (Map<String, Object>) response.getBody().get("data");
    return ((Number) page.get("totalElements")).intValue();
  }

  private void assertCommercialState(
      Map<String, Object> data,
      String commercialState,
      String lifecycleState,
      String runtimeState,
      String billingStatus) {
    assertThat(data)
        .containsEntry("commercialState", commercialState)
        .containsEntry("lifecycleState", lifecycleState)
        .containsEntry("runtimeState", runtimeState)
        .containsEntry("billingStatus", billingStatus);
    assertThat(data).containsKeys("tenantId", "tenantCode", "reason", "auditEventId");
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

  private Long countBillingLedgerAuditEvents(Long companyId) {
    return jdbcTemplate.queryForObject(
        "select count(distinct reason.audit_log_id) from audit_log_metadata reason "
            + "join audit_log_metadata target on target.audit_log_id = reason.audit_log_id "
            + "where reason.metadata_key = 'reason' "
            + "and reason.metadata_value like 'billing-ledger-%' "
            + "and target.metadata_key = 'targetCompanyId' "
            + "and target.metadata_value = ?",
        Long.class, String.valueOf(companyId));
  }

  private Long countCommercialLifecycleAuditEvents(Long companyId) {
    return jdbcTemplate.queryForObject(
        "select count(distinct reason.audit_log_id) from audit_log_metadata reason "
            + "join audit_log_metadata target on target.audit_log_id = reason.audit_log_id "
            + "where reason.metadata_key = 'reason' "
            + "and reason.metadata_value like 'commercial-state-%' "
            + "and target.metadata_key = 'targetCompanyId' "
            + "and target.metadata_value = ?",
        Long.class, String.valueOf(companyId));
  }
}
