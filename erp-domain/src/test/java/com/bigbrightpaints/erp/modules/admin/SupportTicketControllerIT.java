package com.bigbrightpaints.erp.modules.admin;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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

import com.bigbrightpaints.erp.core.audit.AuditLogRepository;
import com.bigbrightpaints.erp.modules.admin.domain.SupportTicket;
import com.bigbrightpaints.erp.modules.admin.domain.SupportTicketCategory;
import com.bigbrightpaints.erp.modules.admin.domain.SupportTicketPriority;
import com.bigbrightpaints.erp.modules.admin.domain.SupportTicketRepository;
import com.bigbrightpaints.erp.modules.admin.domain.SupportTicketSlaStatus;
import com.bigbrightpaints.erp.modules.admin.domain.SupportTicketStatus;
import com.bigbrightpaints.erp.modules.admin.domain.SupportTicketTimelineRepository;
import com.bigbrightpaints.erp.modules.auth.domain.UserAccount;
import com.bigbrightpaints.erp.modules.auth.domain.UserAccountRepository;
import com.bigbrightpaints.erp.modules.company.domain.Company;
import com.bigbrightpaints.erp.modules.company.domain.CompanyRepository;
import com.bigbrightpaints.erp.test.AbstractIntegrationTest;

class SupportTicketControllerIT extends AbstractIntegrationTest {

  private static final String TENANT_A = "SUPA";
  private static final String TENANT_B = "SUPB";
  private static final String ROOT_TENANT = "ROOTSUP";

  private static final String PASSWORD = "Admin@123";
  private static final String DEALER_A_EMAIL = "support.dealer.a@bbp.com";
  private static final String DEALER_B_EMAIL = "support.dealer.b@bbp.com";
  private static final String ADMIN_A_EMAIL = "support.admin.a@bbp.com";
  private static final String ADMIN_B_EMAIL = "support.admin.b@bbp.com";
  private static final String SUPER_ADMIN_EMAIL = "support.superadmin@bbp.com";

  @Autowired private TestRestTemplate rest;

  @Autowired private CompanyRepository companyRepository;

  @Autowired private UserAccountRepository userAccountRepository;

  @Autowired private SupportTicketRepository supportTicketRepository;

  @Autowired private SupportTicketTimelineRepository supportTicketTimelineRepository;

  @Autowired private AuditLogRepository auditLogRepository;

  @BeforeEach
  void seedUsers() {
    dataSeeder.ensureUser(
        DEALER_A_EMAIL, PASSWORD, "Support Dealer A", TENANT_A, List.of("ROLE_DEALER"));
    dataSeeder.ensureUser(
        DEALER_B_EMAIL, PASSWORD, "Support Dealer B", TENANT_A, List.of("ROLE_DEALER"));
    dataSeeder.ensureUser(
        ADMIN_A_EMAIL, PASSWORD, "Support Admin A", TENANT_A, List.of("ROLE_ADMIN"));
    dataSeeder.ensureUser(
        ADMIN_B_EMAIL, PASSWORD, "Support Admin B", TENANT_B, List.of("ROLE_ADMIN"));
    dataSeeder.ensureUser(
        SUPER_ADMIN_EMAIL,
        PASSWORD,
        "Support Super Admin",
        ROOT_TENANT,
        List.of("ROLE_SUPER_ADMIN"));
    companyRepository
        .findByCodeIgnoreCase(TENANT_A)
        .ifPresent(
            company -> {
              company.setCommercialSupportTier("STANDARD");
              companyRepository.saveAndFlush(company);
            });
  }

  @Test
  void adminCreate_persistsAndReturnsApiEnvelope() {
    String token = login(ADMIN_A_EMAIL, TENANT_A);
    HttpHeaders headers = authHeaders(token, TENANT_A);

    String subject = "Portal support create flow " + System.nanoTime();
    ResponseEntity<Map> response =
        rest.exchange(
            "/api/v1/admin/support/tickets",
            HttpMethod.POST,
            new HttpEntity<>(
                Map.of(
                    "category", "SUPPORT",
                    "subject", subject,
                    "description", "Unable to complete export after approval"),
                headers),
            Map.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().get("success")).isEqualTo(Boolean.TRUE);

    @SuppressWarnings("unchecked")
    Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
    assertThat(data).isNotNull();
    assertThat(data.get("subject")).isEqualTo(subject);
    assertThat(data.get("category")).isEqualTo("SUPPORT");
    assertThat(data.get("companyCode")).isEqualTo(TENANT_A);
    assertThat(data.get("id")).isNotNull();

    Long createdId = Long.parseLong(data.get("id").toString());
    assertThat(supportTicketRepository.findById(createdId)).isPresent();
  }

  @Test
  void dealerCreate_persistsAndReturnsApiEnvelope() {
    String token = login(DEALER_A_EMAIL, TENANT_A);
    HttpHeaders headers = authHeaders(token, TENANT_A);

    String subject = "Dealer support create flow " + System.nanoTime();
    ResponseEntity<Map> response =
        rest.exchange(
            "/api/v1/dealer-portal/support/tickets",
            HttpMethod.POST,
            new HttpEntity<>(
                Map.of(
                    "category", "SUPPORT",
                    "subject", subject,
                    "description", "Dealer cannot reconcile invoice payment"),
                headers),
            Map.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().get("success")).isEqualTo(Boolean.TRUE);

    @SuppressWarnings("unchecked")
    Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
    assertThat(data).isNotNull();
    assertThat(data.get("subject")).isEqualTo(subject);
    assertThat(data.get("category")).isEqualTo("SUPPORT");
    assertThat(data.get("companyCode")).isEqualTo(TENANT_A);
    assertThat(data.get("id")).isNotNull();

    Long createdId = Long.parseLong(data.get("id").toString());
    assertThat(supportTicketRepository.findById(createdId)).isPresent();
  }

  @Test
  void listEndpoints_applyHostSpecificVisibilityAndRetireSharedHost() {
    String marker = "scope-" + System.nanoTime();
    String adminSubject = marker + "-admin";
    String dealerOwnSubject = marker + "-dealer-own";
    String dealerPeerSubject = marker + "-dealer-peer";
    String foreignSubject = marker + "-foreign";

    seedTicket(TENANT_A, ADMIN_A_EMAIL, adminSubject);
    seedTicket(TENANT_A, DEALER_A_EMAIL, dealerOwnSubject);
    seedTicket(TENANT_A, DEALER_B_EMAIL, dealerPeerSubject);
    seedTicket(TENANT_B, ADMIN_B_EMAIL, foreignSubject);

    ResponseEntity<Map> adminSupportResponse =
        rest.exchange(
            "/api/v1/admin/support/tickets",
            HttpMethod.GET,
            new HttpEntity<>(authHeaders(login(ADMIN_A_EMAIL, TENANT_A), TENANT_A)),
            Map.class);
    assertThat(adminSupportResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    Set<String> adminSubjects = subjectsFromListResponse(adminSupportResponse);
    assertThat(adminSubjects).contains(adminSubject, dealerOwnSubject, dealerPeerSubject);
    assertThat(adminSubjects).doesNotContain(foreignSubject);

    ResponseEntity<Map> dealerResponse =
        rest.exchange(
            "/api/v1/dealer-portal/support/tickets",
            HttpMethod.GET,
            new HttpEntity<>(authHeaders(login(DEALER_A_EMAIL, TENANT_A), TENANT_A)),
            Map.class);
    assertThat(dealerResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    Set<String> dealerSubjects = subjectsFromListResponse(dealerResponse);
    assertThat(dealerSubjects).contains(dealerOwnSubject);
    assertThat(dealerSubjects).doesNotContain(adminSubject, dealerPeerSubject, foreignSubject);

    ResponseEntity<Map> adminSupportDealerDenied =
        rest.exchange(
            "/api/v1/admin/support/tickets",
            HttpMethod.GET,
            new HttpEntity<>(authHeaders(login(DEALER_A_EMAIL, TENANT_A), TENANT_A)),
            Map.class);
    assertThat(adminSupportDealerDenied.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

    ResponseEntity<Map> dealerPortalAdminDenied =
        rest.exchange(
            "/api/v1/dealer-portal/support/tickets",
            HttpMethod.GET,
            new HttpEntity<>(authHeaders(login(ADMIN_A_EMAIL, TENANT_A), TENANT_A)),
            Map.class);
    assertThat(dealerPortalAdminDenied.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

    ResponseEntity<Map> portalAdminDenied =
        rest.exchange(
            "/api/v1/portal/support/tickets",
            HttpMethod.GET,
            new HttpEntity<>(authHeaders(login(ADMIN_A_EMAIL, TENANT_A), TENANT_A)),
            Map.class);
    assertThat(portalAdminDenied.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

    ResponseEntity<Map> retiredSharedAdmin =
        rest.exchange(
            "/api/v1/support/tickets",
            HttpMethod.GET,
            new HttpEntity<>(authHeaders(login(ADMIN_A_EMAIL, TENANT_A), TENANT_A)),
            Map.class);
    assertThat(retiredSharedAdmin.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

    ResponseEntity<Map> retiredSharedDealer =
        rest.exchange(
            "/api/v1/support/tickets",
            HttpMethod.GET,
            new HttpEntity<>(authHeaders(login(DEALER_A_EMAIL, TENANT_A), TENANT_A)),
            Map.class);
    assertThat(retiredSharedDealer.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void detailEndpoints_enforceHostAndTenantBoundaries() {
    Long dealerOwnTicket =
        seedTicket(TENANT_A, DEALER_A_EMAIL, "detail-dealer-own-" + System.nanoTime());
    Long dealerPeerTicket =
        seedTicket(TENANT_A, DEALER_B_EMAIL, "detail-dealer-peer-" + System.nanoTime());
    Long adminTicket = seedTicket(TENANT_A, ADMIN_A_EMAIL, "detail-admin-" + System.nanoTime());
    Long foreignTicket = seedTicket(TENANT_B, ADMIN_B_EMAIL, "detail-foreign-" + System.nanoTime());

    ResponseEntity<Map> dealerOwnResponse =
        rest.exchange(
            "/api/v1/dealer-portal/support/tickets/" + dealerOwnTicket,
            HttpMethod.GET,
            new HttpEntity<>(authHeaders(login(DEALER_A_EMAIL, TENANT_A), TENANT_A)),
            Map.class);
    assertThat(dealerOwnResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

    ResponseEntity<Map> dealerPeerDenied =
        rest.exchange(
            "/api/v1/dealer-portal/support/tickets/" + dealerPeerTicket,
            HttpMethod.GET,
            new HttpEntity<>(authHeaders(login(DEALER_A_EMAIL, TENANT_A), TENANT_A)),
            Map.class);
    assertThat(dealerPeerDenied.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

    ResponseEntity<Map> dealerCrossHostProbe =
        rest.exchange(
            "/api/v1/dealer-portal/support/tickets/" + adminTicket,
            HttpMethod.GET,
            new HttpEntity<>(authHeaders(login(DEALER_A_EMAIL, TENANT_A), TENANT_A)),
            Map.class);
    assertThat(dealerCrossHostProbe.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

    ResponseEntity<Map> dealerForeignDenied =
        rest.exchange(
            "/api/v1/dealer-portal/support/tickets/" + foreignTicket,
            HttpMethod.GET,
            new HttpEntity<>(authHeaders(login(DEALER_A_EMAIL, TENANT_A), TENANT_A)),
            Map.class);
    assertThat(dealerForeignDenied.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

    ResponseEntity<Map> adminSupportResponse =
        rest.exchange(
            "/api/v1/admin/support/tickets/" + dealerOwnTicket,
            HttpMethod.GET,
            new HttpEntity<>(authHeaders(login(ADMIN_A_EMAIL, TENANT_A), TENANT_A)),
            Map.class);
    assertThat(adminSupportResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

    ResponseEntity<Map> adminSupportForeignDenied =
        rest.exchange(
            "/api/v1/admin/support/tickets/" + foreignTicket,
            HttpMethod.GET,
            new HttpEntity<>(authHeaders(login(ADMIN_A_EMAIL, TENANT_A), TENANT_A)),
            Map.class);
    assertThat(adminSupportForeignDenied.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

    ResponseEntity<Map> adminSupportDealerDenied =
        rest.exchange(
            "/api/v1/admin/support/tickets/" + dealerOwnTicket,
            HttpMethod.GET,
            new HttpEntity<>(authHeaders(login(DEALER_A_EMAIL, TENANT_A), TENANT_A)),
            Map.class);
    assertThat(adminSupportDealerDenied.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

    ResponseEntity<Map> dealerPortalAdminDenied =
        rest.exchange(
            "/api/v1/dealer-portal/support/tickets/" + dealerOwnTicket,
            HttpMethod.GET,
            new HttpEntity<>(authHeaders(login(ADMIN_A_EMAIL, TENANT_A), TENANT_A)),
            Map.class);
    assertThat(dealerPortalAdminDenied.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

    ResponseEntity<Map> retiredSharedAdmin =
        rest.exchange(
            "/api/v1/support/tickets/" + adminTicket,
            HttpMethod.GET,
            new HttpEntity<>(authHeaders(login(ADMIN_A_EMAIL, TENANT_A), TENANT_A)),
            Map.class);
    assertThat(retiredSharedAdmin.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void createEndpoints_denyCrossHostRolesAndSuperAdminTenantWorkflowAccess() {
    ResponseEntity<Map> dealerOnAdminSupportResponse =
        rest.exchange(
            "/api/v1/admin/support/tickets",
            HttpMethod.POST,
            new HttpEntity<>(
                Map.of(
                    "category", "SUPPORT",
                    "subject", "dealer-on-admin-support-" + System.nanoTime(),
                    "description", "Dealer must not post admin support tickets"),
                authHeaders(login(DEALER_A_EMAIL, TENANT_A), TENANT_A)),
            Map.class);
    assertThat(dealerOnAdminSupportResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

    ResponseEntity<Map> dealerOnPortalResponse =
        rest.exchange(
            "/api/v1/portal/support/tickets",
            HttpMethod.POST,
            new HttpEntity<>(
                Map.of(
                    "category", "SUPPORT",
                    "subject", "dealer-on-portal-" + System.nanoTime(),
                    "description", "Dealer must not post portal support tickets"),
                authHeaders(login(DEALER_A_EMAIL, TENANT_A), TENANT_A)),
            Map.class);
    assertThat(dealerOnPortalResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

    ResponseEntity<Map> adminOnDealerPortalResponse =
        rest.exchange(
            "/api/v1/dealer-portal/support/tickets",
            HttpMethod.POST,
            new HttpEntity<>(
                Map.of(
                    "category", "SUPPORT",
                    "subject", "admin-on-dealer-portal-" + System.nanoTime(),
                    "description", "Admin must not post dealer portal support tickets"),
                authHeaders(login(ADMIN_A_EMAIL, TENANT_A), TENANT_A)),
            Map.class);
    assertThat(adminOnDealerPortalResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

    ResponseEntity<Map> retiredSharedAdminResponse =
        rest.exchange(
            "/api/v1/support/tickets",
            HttpMethod.POST,
            new HttpEntity<>(
                Map.of(
                    "category", "SUPPORT",
                    "subject", "retired-admin-" + System.nanoTime(),
                    "description", "Shared support host must be gone"),
                authHeaders(login(ADMIN_A_EMAIL, TENANT_A), TENANT_A)),
            Map.class);
    assertThat(retiredSharedAdminResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

    ResponseEntity<Map> retiredSharedSuperAdminResponse =
        rest.exchange(
            "/api/v1/support/tickets",
            HttpMethod.POST,
            new HttpEntity<>(
                Map.of(
                    "category", "SUPPORT",
                    "subject", "retired-super-admin-" + System.nanoTime(),
                    "description", "Shared support host must stay unmapped for super admins too"),
                authHeaders(login(SUPER_ADMIN_EMAIL, ROOT_TENANT), ROOT_TENANT)),
            Map.class);
    assertThat(retiredSharedSuperAdminResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

    ResponseEntity<Map> response =
        rest.exchange(
            "/api/v1/admin/support/tickets",
            HttpMethod.POST,
            new HttpEntity<>(
                Map.of(
                    "category", "SUPPORT",
                    "subject", "platform-only-" + System.nanoTime(),
                    "description", "Super admin must not create tenant support tickets"),
                authHeaders(login(SUPER_ADMIN_EMAIL, ROOT_TENANT), ROOT_TENANT)),
            Map.class);

    assertForbiddenPlatformOnly(response);
  }

  @Test
  void superAdminQueueMessagesAndInternalNotesAreTenantSafeAuditedAndPaginated() {
    String marker = "m11-chat-" + System.nanoTime();
    Long ticketId = seedTicket(TENANT_A, ADMIN_A_EMAIL, marker + "-ticket");
    String tenantToken = login(ADMIN_A_EMAIL, TENANT_A);
    String superAdminToken = login(SUPER_ADMIN_EMAIL, ROOT_TENANT);

    ResponseEntity<Map> tenantMessage =
        rest.exchange(
            "/api/v1/admin/support/tickets/" + ticketId + "/messages",
            HttpMethod.POST,
            new HttpEntity<>(
                Map.of("content", "Customer reply <b>" + marker + "</b>"),
                authHeaders(tenantToken, TENANT_A)),
            Map.class);
    assertThat(tenantMessage.getStatusCode()).isEqualTo(HttpStatus.OK);
    Map<String, Object> tenantMessageData = data(tenantMessage);
    assertThat(tenantMessageData.get("authorRole")).isEqualTo("TENANT");
    assertThat(tenantMessageData.get("visibility")).isEqualTo("CUSTOMER");
    assertThat(String.valueOf(tenantMessageData.get("content"))).doesNotContain("<b>");

    ResponseEntity<Map> platformReply =
        rest.exchange(
            "/api/v1/superadmin/support/tickets/" + ticketId + "/messages",
            HttpMethod.POST,
            new HttpEntity<>(
                Map.of("content", "Platform reply " + marker),
                authHeaders(superAdminToken, ROOT_TENANT)),
            Map.class);
    assertThat(platformReply.getStatusCode()).isEqualTo(HttpStatus.OK);
    Map<String, Object> platformReplyData = data(platformReply);
    assertThat(platformReplyData.get("authorRole")).isEqualTo("SUPER_ADMIN");
    assertThat(platformReplyData.get("visibility")).isEqualTo("CUSTOMER");
    assertThat(platformReplyData.get("authorEmail")).isEqualTo(SUPER_ADMIN_EMAIL);
    assertThat(platformReplyData.get("auditEventId")).isNotNull();

    ResponseEntity<Map> internalNote =
        rest.exchange(
            "/api/v1/superadmin/support/tickets/" + ticketId + "/internal-notes",
            HttpMethod.POST,
            new HttpEntity<>(
                Map.of("content", "Internal triage note " + marker),
                authHeaders(superAdminToken, ROOT_TENANT)),
            Map.class);
    assertThat(internalNote.getStatusCode()).isEqualTo(HttpStatus.OK);
    Map<String, Object> internalNoteData = data(internalNote);
    assertThat(internalNoteData.get("visibility")).isEqualTo("INTERNAL");
    assertThat(internalNoteData.get("auditEventId")).isNotNull();
    assertThat(
            auditLogRepository.findById(
                Long.parseLong(String.valueOf(internalNoteData.get("auditEventId")))))
        .isPresent();

    ResponseEntity<Map> queue =
        rest.exchange(
            "/api/v1/superadmin/support/tickets?q="
                + marker
                + "&status=OPEN&page=0&size=5&sort=createdAt,desc",
            HttpMethod.GET,
            new HttpEntity<>(authHeaders(superAdminToken, ROOT_TENANT)),
            Map.class);
    assertThat(queue.getStatusCode()).isEqualTo(HttpStatus.OK);
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> queueContent = (List<Map<String, Object>>) data(queue).get("content");
    assertThat(queueContent).hasSize(1);
    Map<String, Object> queueItem = queueContent.getFirst();
    assertThat(queueItem.get("ticketId")).isEqualTo(ticketId.intValue());
    assertThat(queueItem.get("companyCode")).isEqualTo(TENANT_A);
    assertThat(queueItem.get("priority")).isEqualTo("NORMAL");
    assertThat(queueItem.get("requesterEmail")).isEqualTo(ADMIN_A_EMAIL);
    assertThat(queueItem.get("requesterRole")).isEqualTo("TENANT_ADMIN");
    assertThat(queueItem.get("sla")).isInstanceOf(Map.class);

    ResponseEntity<Map> platformDetail =
        rest.exchange(
            "/api/v1/superadmin/support/tickets/" + ticketId,
            HttpMethod.GET,
            new HttpEntity<>(authHeaders(superAdminToken, ROOT_TENANT)),
            Map.class);
    assertThat(platformDetail.getStatusCode()).isEqualTo(HttpStatus.OK);
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> platformMessages =
        (List<Map<String, Object>>) data(platformDetail).get("messages");
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> platformInternalNotes =
        (List<Map<String, Object>>) data(platformDetail).get("internalNotes");
    assertThat(platformMessages)
        .extracting(message -> message.get("authorRole"))
        .containsExactly("TENANT", "SUPER_ADMIN");
    assertThat(platformMessages.get(1).get("authorEmail")).isEqualTo(SUPER_ADMIN_EMAIL);
    assertThat(platformMessages.get(1).get("authorUserId")).isNotNull();
    assertThat(platformMessages.get(1).get("auditEventId")).isNotNull();
    assertThat(platformInternalNotes)
        .extracting(message -> String.valueOf(message.get("content")))
        .containsExactly("Internal triage note " + marker);

    ResponseEntity<Map> tenantDetail =
        rest.exchange(
            "/api/v1/admin/support/tickets/" + ticketId,
            HttpMethod.GET,
            new HttpEntity<>(authHeaders(tenantToken, TENANT_A)),
            Map.class);
    assertThat(tenantDetail.getStatusCode()).isEqualTo(HttpStatus.OK);
    Map<String, Object> tenantDetailData = data(tenantDetail);
    assertThat(tenantDetailData).doesNotContainKey("internalNotes");
    assertThat(String.valueOf(tenantDetailData)).doesNotContain("Internal triage note " + marker);
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> tenantDetailMessages =
        (List<Map<String, Object>>) tenantDetailData.get("messages");
    assertThat(tenantDetailMessages).hasSize(2);
    Map<String, Object> tenantVisiblePlatformReply = tenantDetailMessages.get(1);
    assertThat(tenantVisiblePlatformReply.get("authorRole")).isEqualTo("SUPER_ADMIN");
    assertThat(tenantVisiblePlatformReply.get("authorEmail")).isNull();
    assertThat(tenantVisiblePlatformReply.get("authorUserId")).isNull();
    assertThat(tenantVisiblePlatformReply.get("auditEventId")).isNull();

    ResponseEntity<Map> firstMessagePage =
        rest.exchange(
            "/api/v1/admin/support/tickets/" + ticketId + "/messages?page=0&size=1",
            HttpMethod.GET,
            new HttpEntity<>(authHeaders(tenantToken, TENANT_A)),
            Map.class);
    assertThat(firstMessagePage.getStatusCode()).isEqualTo(HttpStatus.OK);
    Map<String, Object> firstMessagePageData = data(firstMessagePage);
    assertThat(firstMessagePageData.get("totalElements")).isEqualTo(2);
    assertThat(firstMessagePageData.get("page")).isEqualTo(0);
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> firstPageContent =
        (List<Map<String, Object>>) firstMessagePageData.get("content");
    assertThat(firstPageContent).hasSize(1);
    assertThat(firstPageContent.getFirst().get("id")).isEqualTo(tenantMessageData.get("id"));

    ResponseEntity<Map> allMessagesForTenant =
        rest.exchange(
            "/api/v1/admin/support/tickets/" + ticketId + "/messages?page=0&size=10",
            HttpMethod.GET,
            new HttpEntity<>(authHeaders(tenantToken, TENANT_A)),
            Map.class);
    assertThat(allMessagesForTenant.getStatusCode()).isEqualTo(HttpStatus.OK);
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> tenantMessagePage =
        (List<Map<String, Object>>) data(allMessagesForTenant).get("content");
    Map<String, Object> redactedPlatformReply = tenantMessagePage.get(1);
    assertThat(redactedPlatformReply.get("authorRole")).isEqualTo("SUPER_ADMIN");
    assertThat(redactedPlatformReply.get("authorEmail")).isNull();
    assertThat(redactedPlatformReply.get("authorUserId")).isNull();
    assertThat(redactedPlatformReply.get("auditEventId")).isNull();

    ResponseEntity<Map> editProbe =
        rest.exchange(
            "/api/v1/admin/support/tickets/"
                + ticketId
                + "/messages/"
                + tenantMessageData.get("id"),
            HttpMethod.PUT,
            new HttpEntity<>(Map.of("content", "edited"), authHeaders(tenantToken, TENANT_A)),
            Map.class);
    assertThat(editProbe.getStatusCode()).isIn(HttpStatus.METHOD_NOT_ALLOWED, HttpStatus.NOT_FOUND);
  }

  @Test
  void listAndDetailResponsesEmbedOnlyBoundedMessagePreviewForLongThreads() {
    String marker = "m11-preview-" + System.nanoTime();
    Long adminTicketId = seedTicket(TENANT_A, ADMIN_A_EMAIL, marker + "-admin-ticket");
    String tenantToken = login(ADMIN_A_EMAIL, TENANT_A);
    String superAdminToken = login(SUPER_ADMIN_EMAIL, ROOT_TENANT);

    for (int i = 1; i <= 7; i++) {
      ResponseEntity<Map> message =
          rest.exchange(
              "/api/v1/admin/support/tickets/" + adminTicketId + "/messages",
              HttpMethod.POST,
              new HttpEntity<>(
                  Map.of("content", marker + "-admin-message-" + i),
                  authHeaders(tenantToken, TENANT_A)),
              Map.class);
      assertThat(message.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    ResponseEntity<Map> adminList =
        rest.exchange(
            "/api/v1/admin/support/tickets",
            HttpMethod.GET,
            new HttpEntity<>(authHeaders(tenantToken, TENANT_A)),
            Map.class);
    assertThat(adminList.getStatusCode()).isEqualTo(HttpStatus.OK);
    Map<String, Object> listTicket = ticketFromListResponse(adminList, adminTicketId);
    assertMessagesAreBoundedPreview(listTicket, 5);

    ResponseEntity<Map> adminDetail =
        rest.exchange(
            "/api/v1/admin/support/tickets/" + adminTicketId,
            HttpMethod.GET,
            new HttpEntity<>(authHeaders(tenantToken, TENANT_A)),
            Map.class);
    assertThat(adminDetail.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertMessagesAreBoundedPreview(data(adminDetail), 5);

    ResponseEntity<Map> superAdminDetail =
        rest.exchange(
            "/api/v1/superadmin/support/tickets/" + adminTicketId,
            HttpMethod.GET,
            new HttpEntity<>(authHeaders(superAdminToken, ROOT_TENANT)),
            Map.class);
    assertThat(superAdminDetail.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertMessagesAreBoundedPreview(data(superAdminDetail), 5);

    ResponseEntity<Map> pagedMessages =
        rest.exchange(
            "/api/v1/admin/support/tickets/" + adminTicketId + "/messages?page=1&size=3",
            HttpMethod.GET,
            new HttpEntity<>(authHeaders(tenantToken, TENANT_A)),
            Map.class);
    assertThat(pagedMessages.getStatusCode()).isEqualTo(HttpStatus.OK);
    Map<String, Object> pagedData = data(pagedMessages);
    assertThat(pagedData.get("totalElements")).isEqualTo(7);
    assertThat(pagedData.get("page")).isEqualTo(1);
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> pagedContent = (List<Map<String, Object>>) pagedData.get("content");
    assertThat(pagedContent).hasSize(3);
    assertThat(String.valueOf(pagedContent.getFirst().get("content")))
        .isEqualTo(marker + "-admin-message-4");

    Long dealerTicketId = seedTicket(TENANT_A, DEALER_A_EMAIL, marker + "-dealer-ticket");
    String dealerToken = login(DEALER_A_EMAIL, TENANT_A);
    for (int i = 1; i <= 6; i++) {
      ResponseEntity<Map> message =
          rest.exchange(
              "/api/v1/dealer-portal/support/tickets/" + dealerTicketId + "/messages",
              HttpMethod.POST,
              new HttpEntity<>(
                  Map.of("content", marker + "-dealer-message-" + i),
                  authHeaders(dealerToken, TENANT_A)),
              Map.class);
      assertThat(message.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    ResponseEntity<Map> dealerDetail =
        rest.exchange(
            "/api/v1/dealer-portal/support/tickets/" + dealerTicketId,
            HttpMethod.GET,
            new HttpEntity<>(authHeaders(dealerToken, TENANT_A)),
            Map.class);
    assertThat(dealerDetail.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertMessagesAreBoundedPreview(data(dealerDetail), 5);
  }

  @Test
  void superAdminIncludeInternalMessagesArePaginatedAcrossCustomerMessagesAndInternalNotes() {
    String marker = "m11-include-internal-" + System.nanoTime();
    Long ticketId = seedTicket(TENANT_A, ADMIN_A_EMAIL, marker + "-ticket");
    String tenantToken = login(ADMIN_A_EMAIL, TENANT_A);
    String superAdminToken = login(SUPER_ADMIN_EMAIL, ROOT_TENANT);

    postMessage(
        "/api/v1/admin/support/tickets/" + ticketId + "/messages",
        marker + "-customer-1",
        tenantToken,
        TENANT_A);
    postMessage(
        "/api/v1/superadmin/support/tickets/" + ticketId + "/internal-notes",
        marker + "-internal-1",
        superAdminToken,
        ROOT_TENANT);
    postMessage(
        "/api/v1/superadmin/support/tickets/" + ticketId + "/messages",
        marker + "-customer-2",
        superAdminToken,
        ROOT_TENANT);
    postMessage(
        "/api/v1/superadmin/support/tickets/" + ticketId + "/internal-notes",
        marker + "-internal-2",
        superAdminToken,
        ROOT_TENANT);
    postMessage(
        "/api/v1/admin/support/tickets/" + ticketId + "/messages",
        marker + "-customer-3",
        tenantToken,
        TENANT_A);

    ResponseEntity<Map> includeInternalPage0 =
        rest.exchange(
            "/api/v1/superadmin/support/tickets/"
                + ticketId
                + "/messages?includeInternal=true&page=0&size=3",
            HttpMethod.GET,
            new HttpEntity<>(authHeaders(superAdminToken, ROOT_TENANT)),
            Map.class);
    assertThat(includeInternalPage0.getStatusCode()).isEqualTo(HttpStatus.OK);
    Map<String, Object> page0Data = data(includeInternalPage0);
    assertThat(page0Data.get("totalElements")).isEqualTo(5);
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> page0Content = (List<Map<String, Object>>) page0Data.get("content");
    assertThat(page0Content)
        .extracting(message -> message.get("visibility"))
        .containsExactly("CUSTOMER", "INTERNAL", "CUSTOMER");
    assertThat(page0Content)
        .extracting(message -> String.valueOf(message.get("content")))
        .containsExactly(marker + "-customer-1", marker + "-internal-1", marker + "-customer-2");

    ResponseEntity<Map> includeInternalPage1 =
        rest.exchange(
            "/api/v1/superadmin/support/tickets/"
                + ticketId
                + "/messages?includeInternal=true&page=1&size=3",
            HttpMethod.GET,
            new HttpEntity<>(authHeaders(superAdminToken, ROOT_TENANT)),
            Map.class);
    assertThat(includeInternalPage1.getStatusCode()).isEqualTo(HttpStatus.OK);
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> page1Content =
        (List<Map<String, Object>>) data(includeInternalPage1).get("content");
    assertThat(page1Content)
        .extracting(message -> message.get("visibility"))
        .containsExactly("INTERNAL", "CUSTOMER");

    ResponseEntity<Map> customerOnlyPage =
        rest.exchange(
            "/api/v1/superadmin/support/tickets/"
                + ticketId
                + "/messages?includeInternal=false&page=0&size=10",
            HttpMethod.GET,
            new HttpEntity<>(authHeaders(superAdminToken, ROOT_TENANT)),
            Map.class);
    assertThat(customerOnlyPage.getStatusCode()).isEqualTo(HttpStatus.OK);
    Map<String, Object> customerOnlyData = data(customerOnlyPage);
    assertThat(customerOnlyData.get("totalElements")).isEqualTo(3);
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> customerOnlyContent =
        (List<Map<String, Object>>) customerOnlyData.get("content");
    assertThat(customerOnlyContent)
        .extracting(message -> message.get("visibility"))
        .containsExactly("CUSTOMER", "CUSTOMER", "CUSTOMER");
  }

  @Test
  void superAdminPrioritySortUsesBusinessRankAndDeterministicIdTieBreakers() {
    String marker = "m11-priority-" + System.nanoTime();
    Long low = seedTicket(TENANT_A, ADMIN_A_EMAIL, marker + "-low", SupportTicketPriority.LOW);
    Long high1 =
        seedTicket(TENANT_A, ADMIN_A_EMAIL, marker + "-high-1", SupportTicketPriority.HIGH);
    Long normal =
        seedTicket(TENANT_A, ADMIN_A_EMAIL, marker + "-normal", SupportTicketPriority.NORMAL);
    Long urgent =
        seedTicket(TENANT_A, ADMIN_A_EMAIL, marker + "-urgent", SupportTicketPriority.URGENT);
    Long high2 =
        seedTicket(TENANT_A, ADMIN_A_EMAIL, marker + "-high-2", SupportTicketPriority.HIGH);

    ResponseEntity<Map> queue =
        rest.exchange(
            "/api/v1/superadmin/support/tickets?q=" + marker + "&page=0&size=10&sort=priority",
            HttpMethod.GET,
            new HttpEntity<>(authHeaders(login(SUPER_ADMIN_EMAIL, ROOT_TENANT), ROOT_TENANT)),
            Map.class);
    assertThat(queue.getStatusCode()).isEqualTo(HttpStatus.OK);
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> queueContent = (List<Map<String, Object>>) data(queue).get("content");

    assertThat(queueContent).hasSize(5);
    assertThat(queueContent)
        .extracting(item -> item.get("priority"))
        .containsExactly("URGENT", "HIGH", "HIGH", "NORMAL", "LOW");
    assertThat(queueContent)
        .extracting(item -> Long.parseLong(String.valueOf(item.get("ticketId"))))
        .containsExactly(urgent, high1, high2, normal, low);
  }

  @Test
  void supportAccessMatrixEnforcesTenantBoundariesAndPlatformOnlyControls() {
    Long tenantTicket = seedTicket(TENANT_A, ADMIN_A_EMAIL, "matrix-admin-" + System.nanoTime());
    Long dealerTicket = seedTicket(TENANT_A, DEALER_A_EMAIL, "matrix-dealer-" + System.nanoTime());
    Long foreignTicket = seedTicket(TENANT_B, ADMIN_B_EMAIL, "matrix-foreign-" + System.nanoTime());
    String dealerToken = login(DEALER_A_EMAIL, TENANT_A);
    String superAdminToken = login(SUPER_ADMIN_EMAIL, ROOT_TENANT);

    ResponseEntity<Map> foreignTenantMessageProbe =
        rest.exchange(
            "/api/v1/admin/support/tickets/" + foreignTicket + "/messages",
            HttpMethod.POST,
            new HttpEntity<>(
                Map.of("content", "cross tenant probe"),
                authHeaders(login(ADMIN_A_EMAIL, TENANT_A), TENANT_A)),
            Map.class);
    assertThat(foreignTenantMessageProbe.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

    ResponseEntity<Map> dealerPeerMessageProbe =
        rest.exchange(
            "/api/v1/dealer-portal/support/tickets/" + tenantTicket + "/messages",
            HttpMethod.POST,
            new HttpEntity<>(
                Map.of("content", "dealer should not message admin ticket"),
                authHeaders(dealerToken, TENANT_A)),
            Map.class);
    assertThat(dealerPeerMessageProbe.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

    ResponseEntity<Map> dealerOwnMessage =
        rest.exchange(
            "/api/v1/dealer-portal/support/tickets/" + dealerTicket + "/messages",
            HttpMethod.POST,
            new HttpEntity<>(
                Map.of("content", "dealer own ticket reply"), authHeaders(dealerToken, TENANT_A)),
            Map.class);
    assertThat(dealerOwnMessage.getStatusCode()).isEqualTo(HttpStatus.OK);

    ResponseEntity<Map> superAdminDealerReply =
        rest.exchange(
            "/api/v1/superadmin/support/tickets/" + dealerTicket + "/messages",
            HttpMethod.POST,
            new HttpEntity<>(
                Map.of("content", "platform dealer reply"),
                authHeaders(superAdminToken, ROOT_TENANT)),
            Map.class);
    assertThat(superAdminDealerReply.getStatusCode()).isEqualTo(HttpStatus.OK);

    ResponseEntity<Map> dealerMessages =
        rest.exchange(
            "/api/v1/dealer-portal/support/tickets/" + dealerTicket + "/messages?page=0&size=10",
            HttpMethod.GET,
            new HttpEntity<>(authHeaders(dealerToken, TENANT_A)),
            Map.class);
    assertThat(dealerMessages.getStatusCode()).isEqualTo(HttpStatus.OK);
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> dealerMessageContent =
        (List<Map<String, Object>>) data(dealerMessages).get("content");
    Map<String, Object> dealerVisiblePlatformReply = dealerMessageContent.get(1);
    assertThat(dealerVisiblePlatformReply.get("authorRole")).isEqualTo("SUPER_ADMIN");
    assertThat(dealerVisiblePlatformReply.get("authorEmail")).isNull();
    assertThat(dealerVisiblePlatformReply.get("authorUserId")).isNull();
    assertThat(dealerVisiblePlatformReply.get("auditEventId")).isNull();

    ResponseEntity<Map> tenantInternalNoteProbe =
        rest.exchange(
            "/api/v1/superadmin/support/tickets/" + tenantTicket + "/internal-notes",
            HttpMethod.POST,
            new HttpEntity<>(
                Map.of("content", "tenant cannot add platform note"),
                authHeaders(login(ADMIN_A_EMAIL, TENANT_A), TENANT_A)),
            Map.class);
    assertThat(tenantInternalNoteProbe.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

    ResponseEntity<Map> anonymousQueueProbe =
        rest.exchange(
            "/api/v1/superadmin/support/tickets",
            HttpMethod.GET,
            new HttpEntity<>(jsonOnlyHeaders()),
            Map.class);
    assertThat(anonymousQueueProbe.getStatusCode())
        .isIn(HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN);

    ResponseEntity<Map> tenantPriorityProbe =
        rest.exchange(
            "/api/v1/admin/support/tickets/" + tenantTicket + "/priority",
            HttpMethod.PUT,
            new HttpEntity<>(
                Map.of("priority", "URGENT"),
                authHeaders(login(ADMIN_A_EMAIL, TENANT_A), TENANT_A)),
            Map.class);
    assertThat(tenantPriorityProbe.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void slaRefreshRejectsFutureAsOfWithoutTicketAuditTimelineOrCounterSideEffects() {
    Company company = companyRepository.findByCodeIgnoreCase(TENANT_A).orElseThrow();
    company.setCommercialSupportTier("PRIORITY");
    companyRepository.saveAndFlush(company);

    String marker = "m11-sla-future-asof-" + System.nanoTime();
    String tenantToken = login(ADMIN_A_EMAIL, TENANT_A);
    String superAdminToken = login(SUPER_ADMIN_EMAIL, ROOT_TENANT);
    Long ticketId = createTenantTicket(tenantToken, "SUPPORT", "HIGH", marker + "-ticket");

    Map<String, Object> initialData = data(superAdminDetail(ticketId, superAdminToken));
    Map<String, Object> initialSla = nestedMap(initialData, "sla");
    SupportTicket ticket = supportTicketRepository.findById(ticketId).orElseThrow();
    long breachedCounterBefore =
        supportTicketRepository.countBySlaStatus(SupportTicketSlaStatus.BREACHED);
    long auditCountBefore = auditLogRepository.count();
    long timelineCountBefore = supportTicketTimelineRepository.count();
    long breachTimelineBefore =
        supportTicketTimelineRepository.countByTicketAndEventTypeIn(
            ticket, List.of("SLA_BREACHED"));

    ResponseEntity<Map> futureRefresh =
        rest.exchange(
            "/api/v1/superadmin/support/tickets/sla/refresh",
            HttpMethod.POST,
            new HttpEntity<>(
                Map.of(
                    "asOf",
                    Instant.parse(String.valueOf(initialSla.get("resolutionDueAt")))
                        .plusSeconds(60)
                        .toString()),
                authHeaders(superAdminToken, ROOT_TENANT)),
            Map.class);

    assertThat(futureRefresh.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(supportTicketRepository.countBySlaStatus(SupportTicketSlaStatus.BREACHED))
        .isEqualTo(breachedCounterBefore);
    assertThat(auditLogRepository.count()).isEqualTo(auditCountBefore);
    assertThat(supportTicketTimelineRepository.count()).isEqualTo(timelineCountBefore);
    SupportTicket unchanged = supportTicketRepository.findById(ticketId).orElseThrow();
    assertThat(unchanged.getSlaStatus()).isEqualTo(SupportTicketSlaStatus.PENDING);
    assertThat(unchanged.getBreachedAt()).isNull();
    assertThat(unchanged.getFirstResponseDueAt())
        .isEqualTo(Instant.parse(String.valueOf(initialSla.get("firstResponseDueAt"))));
    assertThat(unchanged.getResolutionDueAt())
        .isEqualTo(Instant.parse(String.valueOf(initialSla.get("resolutionDueAt"))));
    assertThat(
            supportTicketTimelineRepository.countByTicketAndEventTypeIn(
                unchanged, List.of("SLA_BREACHED")))
        .isEqualTo(breachTimelineBefore);
    assertThat(nestedMap(data(superAdminDetail(ticketId, superAdminToken)), "sla").get("status"))
        .isEqualTo("PENDING");
  }

  @Test
  void slaPolicyFirstResponseAndBreachLifecycleAreDeterministicAndIdempotent() {
    Company company = companyRepository.findByCodeIgnoreCase(TENANT_A).orElseThrow();
    company.setCommercialSupportTier("PRIORITY");
    companyRepository.saveAndFlush(company);

    String marker = "m11-sla-" + System.nanoTime();
    String tenantToken = login(ADMIN_A_EMAIL, TENANT_A);
    String superAdminToken = login(SUPER_ADMIN_EMAIL, ROOT_TENANT);
    ResponseEntity<Map> createResponse =
        rest.exchange(
            "/api/v1/admin/support/tickets",
            HttpMethod.POST,
            new HttpEntity<>(
                Map.of(
                    "category",
                    "SUPPORT",
                    "priority",
                    "HIGH",
                    "subject",
                    marker + "-ticket",
                    "description",
                    "SLA lifecycle proof"),
                authHeaders(tenantToken, TENANT_A)),
            Map.class);
    assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    Long ticketId = Long.parseLong(String.valueOf(data(createResponse).get("id")));

    ResponseEntity<Map> initialDetail = superAdminDetail(ticketId, superAdminToken);
    Map<String, Object> initialSla = nestedMap(data(initialDetail), "sla");
    assertThat(initialSla.get("policyId")).isEqualTo("PRIORITY-HIGH");
    assertThat(initialSla.get("status")).isEqualTo("PENDING");
    Instant createdAt = Instant.parse(String.valueOf(data(initialDetail).get("createdAt")));
    assertThat(Instant.parse(String.valueOf(initialSla.get("firstResponseDueAt"))))
        .isBetween(createdAt.plusSeconds(7199), createdAt.plusSeconds(7201));
    assertThat(Instant.parse(String.valueOf(initialSla.get("resolutionDueAt"))))
        .isBetween(
            createdAt.plusSeconds((40L * 3600L) - 1), createdAt.plusSeconds((40L * 3600L) + 1));

    ResponseEntity<Map> internalNote =
        rest.exchange(
            "/api/v1/superadmin/support/tickets/" + ticketId + "/internal-notes",
            HttpMethod.POST,
            new HttpEntity<>(
                Map.of("content", "Internal note must not satisfy first response " + marker),
                authHeaders(superAdminToken, ROOT_TENANT)),
            Map.class);
    assertThat(internalNote.getStatusCode()).isEqualTo(HttpStatus.OK);
    Map<String, Object> afterInternalSla =
        nestedMap(data(superAdminDetail(ticketId, superAdminToken)), "sla");
    assertThat(afterInternalSla.get("firstRespondedAt")).isNull();
    assertThat(afterInternalSla.get("status")).isEqualTo("PENDING");

    ResponseEntity<Map> platformReply =
        rest.exchange(
            "/api/v1/superadmin/support/tickets/" + ticketId + "/messages",
            HttpMethod.POST,
            new HttpEntity<>(
                Map.of("content", "Platform visible reply " + marker),
                authHeaders(superAdminToken, ROOT_TENANT)),
            Map.class);
    assertThat(platformReply.getStatusCode()).isEqualTo(HttpStatus.OK);
    Map<String, Object> afterReplyData = data(superAdminDetail(ticketId, superAdminToken));
    Map<String, Object> afterReplySla = nestedMap(afterReplyData, "sla");
    assertThat(afterReplySla.get("firstRespondedAt")).isNotNull();
    assertThat(afterReplySla.get("status")).isEqualTo("RESPONDED");

    SupportTicket overdueTicket = supportTicketRepository.findById(ticketId).orElseThrow();
    overdueTicket.setResolutionDueAt(Instant.now().minusSeconds(60));
    supportTicketRepository.saveAndFlush(overdueTicket);

    ResponseEntity<Map> breachRefresh =
        rest.exchange(
            "/api/v1/superadmin/support/tickets/sla/refresh",
            HttpMethod.POST,
            new HttpEntity<>(authHeaders(superAdminToken, ROOT_TENANT)),
            Map.class);
    assertThat(breachRefresh.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(Long.parseLong(String.valueOf(data(breachRefresh).get("breachedTickets"))))
        .isGreaterThanOrEqualTo(1);
    assertThat(data(breachRefresh).get("auditEventIds")).asList().isNotEmpty();

    Map<String, Object> breachedData = data(superAdminDetail(ticketId, superAdminToken));
    Map<String, Object> breachedSla = nestedMap(breachedData, "sla");
    assertThat(breachedSla.get("status")).isEqualTo("BREACHED");
    assertThat(breachedSla.get("breachedAt")).isNotNull();
    assertTimelineEvents(breachedData, "TICKET_CREATED", "FIRST_RESPONSE", "SLA_BREACHED");

    ResponseEntity<Map> breachQueue =
        rest.exchange(
            "/api/v1/superadmin/support/tickets?slaStatus=BREACHED&q=" + marker,
            HttpMethod.GET,
            new HttpEntity<>(authHeaders(superAdminToken, ROOT_TENANT)),
            Map.class);
    assertThat(breachQueue.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(data(breachQueue).get("totalElements")).isEqualTo(1);

    ResponseEntity<Map> secondRefresh =
        rest.exchange(
            "/api/v1/superadmin/support/tickets/sla/refresh",
            HttpMethod.POST,
            new HttpEntity<>(authHeaders(superAdminToken, ROOT_TENANT)),
            Map.class);
    assertThat(secondRefresh.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(data(secondRefresh).get("breachedTickets")).isEqualTo(0);
    assertTimelineEvents(data(superAdminDetail(ticketId, superAdminToken)), "SLA_BREACHED");
    assertThat(
            timelineEvents(data(superAdminDetail(ticketId, superAdminToken))).stream()
                .filter("SLA_BREACHED"::equals)
                .count())
        .isEqualTo(1);
  }

  @Test
  void tenantPlanSupportTierChangeRecalculatesActiveSupportAndBugTicketSlaOnly() {
    Company company = companyRepository.findByCodeIgnoreCase(TENANT_A).orElseThrow();
    company.setCommercialSupportTier("STANDARD");
    companyRepository.saveAndFlush(company);

    String marker = "m11-sla-tier-" + System.nanoTime();
    String tenantToken = login(ADMIN_A_EMAIL, TENANT_A);
    String superAdminToken = login(SUPER_ADMIN_EMAIL, ROOT_TENANT);
    Long supportTicket = createTenantTicket(tenantToken, "SUPPORT", "NORMAL", marker + "-support");
    Long bugTicket = createTenantTicket(tenantToken, "BUG", "HIGH", marker + "-bug");
    Long featureTicket =
        createTenantTicket(tenantToken, "FEATURE_REQUEST", "LOW", marker + "-feature");
    Long resolvedTicket =
        createTenantTicket(tenantToken, "SUPPORT", "URGENT", marker + "-resolved");

    ResponseEntity<Map> bugInProgress =
        rest.exchange(
            "/api/v1/superadmin/support/tickets/" + bugTicket + "/status",
            HttpMethod.POST,
            new HttpEntity<>(
                Map.of("status", "IN_PROGRESS", "reason", "Active bug triage"),
                authHeaders(superAdminToken, ROOT_TENANT)),
            Map.class);
    assertThat(bugInProgress.getStatusCode()).isEqualTo(HttpStatus.OK);

    ResponseEntity<Map> internalNote =
        rest.exchange(
            "/api/v1/superadmin/support/tickets/" + supportTicket + "/internal-notes",
            HttpMethod.POST,
            new HttpEntity<>(
                Map.of("content", "Internal note before support tier upgrade " + marker),
                authHeaders(superAdminToken, ROOT_TENANT)),
            Map.class);
    assertThat(internalNote.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(
            nestedMap(data(superAdminDetail(supportTicket, superAdminToken)), "sla")
                .get("firstRespondedAt"))
        .isNull();

    ResponseEntity<Map> platformReply =
        rest.exchange(
            "/api/v1/superadmin/support/tickets/" + supportTicket + "/messages",
            HttpMethod.POST,
            new HttpEntity<>(
                Map.of("content", "Platform reply before support tier upgrade " + marker),
                authHeaders(superAdminToken, ROOT_TENANT)),
            Map.class);
    assertThat(platformReply.getStatusCode()).isEqualTo(HttpStatus.OK);

    Map<String, Object> supportBefore = data(superAdminDetail(supportTicket, superAdminToken));
    Map<String, Object> bugBefore = data(superAdminDetail(bugTicket, superAdminToken));
    Map<String, Object> featureBefore = data(superAdminDetail(featureTicket, superAdminToken));
    ResponseEntity<Map> resolved =
        rest.exchange(
            "/api/v1/superadmin/support/tickets/" + resolvedTicket + "/status",
            HttpMethod.POST,
            new HttpEntity<>(
                Map.of("status", "RESOLVED", "reason", "Resolved before plan assignment"),
                authHeaders(superAdminToken, ROOT_TENANT)),
            Map.class);
    assertThat(resolved.getStatusCode()).isEqualTo(HttpStatus.OK);
    Map<String, Object> resolvedBefore = data(superAdminDetail(resolvedTicket, superAdminToken));
    Map<String, Object> supportSlaBefore = nestedMap(supportBefore, "sla");
    Map<String, Object> bugSlaBefore = nestedMap(bugBefore, "sla");
    Map<String, Object> featureSlaBefore = nestedMap(featureBefore, "sla");
    Map<String, Object> resolvedSlaBefore = nestedMap(resolvedBefore, "sla");
    Object firstRespondedAtBefore = supportSlaBefore.get("firstRespondedAt");
    assertThat(firstRespondedAtBefore).isNotNull();
    assertThat(supportSlaBefore.get("policyId")).isEqualTo("STANDARD-NORMAL");
    assertThat(bugSlaBefore.get("policyId")).isEqualTo("STANDARD-HIGH");
    assertThat(featureSlaBefore.get("status")).isEqualTo("NOT_APPLICABLE");
    assertThat(resolvedSlaBefore.get("status")).isEqualTo("RESOLVED");

    ResponseEntity<Map> assignment =
        rest.exchange(
            "/api/v1/superadmin/tenants/" + company.getId() + "/plan",
            HttpMethod.PUT,
            new HttpEntity<>(
                dedicatedCustomPlanPayload(), authHeaders(superAdminToken, ROOT_TENANT)),
            Map.class);
    assertThat(assignment.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(nestedMap(data(assignment), "plan").get("supportTier")).isEqualTo("DEDICATED");
    assertThat(data(assignment).get("auditEventId")).isNotNull();

    Map<String, Object> supportAfter = data(superAdminDetail(supportTicket, superAdminToken));
    Map<String, Object> bugAfter = data(superAdminDetail(bugTicket, superAdminToken));
    Map<String, Object> featureAfter = data(superAdminDetail(featureTicket, superAdminToken));
    Map<String, Object> resolvedAfter = data(superAdminDetail(resolvedTicket, superAdminToken));
    Map<String, Object> supportSlaAfter = nestedMap(supportAfter, "sla");
    Map<String, Object> bugSlaAfter = nestedMap(bugAfter, "sla");
    Map<String, Object> featureSlaAfter = nestedMap(featureAfter, "sla");
    Map<String, Object> resolvedSlaAfter = nestedMap(resolvedAfter, "sla");

    assertThat(supportSlaAfter.get("policyId")).isEqualTo("DEDICATED-NORMAL");
    assertThat(supportSlaAfter.get("supportTier")).isEqualTo("DEDICATED");
    assertThat(supportSlaAfter.get("firstResponseDueAt"))
        .isNotEqualTo(supportSlaBefore.get("firstResponseDueAt"));
    assertThat(supportSlaAfter.get("resolutionDueAt"))
        .isNotEqualTo(supportSlaBefore.get("resolutionDueAt"));
    assertThat(supportSlaAfter.get("firstRespondedAt")).isEqualTo(firstRespondedAtBefore);
    assertThat(supportSlaAfter.get("status")).isEqualTo("RESPONDED");
    assertThat(bugSlaAfter.get("policyId")).isEqualTo("DEDICATED-HIGH");
    assertThat(bugSlaAfter.get("supportTier")).isEqualTo("DEDICATED");
    assertThat(bugSlaAfter.get("firstResponseDueAt"))
        .isNotEqualTo(bugSlaBefore.get("firstResponseDueAt"));
    assertThat(bugSlaAfter.get("resolutionDueAt"))
        .isNotEqualTo(bugSlaBefore.get("resolutionDueAt"));
    assertThat(featureSlaAfter).isEqualTo(featureSlaBefore);
    assertThat(resolvedSlaAfter).isEqualTo(resolvedSlaBefore);

    assertTimelineEvents(supportAfter, "SLA_POLICY_RECALCULATED");
    assertTimelineEvents(bugAfter, "SLA_POLICY_RECALCULATED");
    assertThat(firstTimelineAudit(supportAfter, "SLA_POLICY_RECALCULATED")).isNotNull();
    assertThat(firstTimelineAudit(bugAfter, "SLA_POLICY_RECALCULATED")).isNotNull();
    assertThat(firstTimelineNote(supportAfter, "SLA_POLICY_RECALCULATED"))
        .contains("STANDARD-NORMAL", "DEDICATED-NORMAL", "firstResponseDueAt", "resolutionDueAt");
    assertThat(timelineEvents(featureAfter)).doesNotContain("SLA_POLICY_RECALCULATED");
    assertThat(timelineEvents(resolvedAfter)).doesNotContain("SLA_POLICY_RECALCULATED");
  }

  @Test
  void featureRequestsStatusTimelineAndExplicitIncidentConversionAreAudited() {
    String marker = "m11-feature-" + System.nanoTime();
    String tenantToken = login(ADMIN_A_EMAIL, TENANT_A);
    String superAdminToken = login(SUPER_ADMIN_EMAIL, ROOT_TENANT);
    ResponseEntity<Map> createResponse =
        rest.exchange(
            "/api/v1/admin/support/tickets",
            HttpMethod.POST,
            new HttpEntity<>(
                Map.of(
                    "category",
                    "FEATURE_REQUEST",
                    "priority",
                    "LOW",
                    "subject",
                    marker + "-request",
                    "description",
                    "Please add a safer export filter"),
                authHeaders(tenantToken, TENANT_A)),
            Map.class);
    assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    Long ticketId = Long.parseLong(String.valueOf(data(createResponse).get("id")));

    Map<String, Object> featureData = data(superAdminDetail(ticketId, superAdminToken));
    assertThat(featureData.get("category")).isEqualTo("FEATURE_REQUEST");
    assertThat(nestedMap(featureData, "sla").get("status")).isEqualTo("NOT_APPLICABLE");
    assertThat(nestedMap(featureData, "sla").get("resolutionDueAt")).isNull();

    ResponseEntity<Map> featureQueue =
        rest.exchange(
            "/api/v1/superadmin/support/tickets?category=FEATURE_REQUEST&q=" + marker,
            HttpMethod.GET,
            new HttpEntity<>(authHeaders(superAdminToken, ROOT_TENANT)),
            Map.class);
    assertThat(featureQueue.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(data(featureQueue).get("totalElements")).isEqualTo(1);

    ResponseEntity<Map> statusChange =
        rest.exchange(
            "/api/v1/superadmin/support/tickets/" + ticketId + "/status",
            HttpMethod.POST,
            new HttpEntity<>(
                Map.of("status", "IN_PROGRESS", "reason", "Reviewed for product backlog"),
                authHeaders(superAdminToken, ROOT_TENANT)),
            Map.class);
    assertThat(statusChange.getStatusCode()).isEqualTo(HttpStatus.OK);
    Map<String, Object> statusData = data(statusChange);
    assertThat(statusData.get("status")).isEqualTo("IN_PROGRESS");
    assertTimelineEvents(statusData, "STATUS_CHANGED");
    assertThat(firstTimelineAudit(statusData, "STATUS_CHANGED")).isNotNull();

    ResponseEntity<Map> refresh =
        rest.exchange(
            "/api/v1/superadmin/support/tickets/sla/refresh",
            HttpMethod.POST,
            new HttpEntity<>(authHeaders(superAdminToken, ROOT_TENANT)),
            Map.class);
    assertThat(refresh.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(nestedMap(data(superAdminDetail(ticketId, superAdminToken)), "sla").get("status"))
        .isEqualTo("NOT_APPLICABLE");

    ResponseEntity<Map> conversion =
        rest.exchange(
            "/api/v1/superadmin/support/tickets/" + ticketId + "/convert-to-incident",
            HttpMethod.POST,
            new HttpEntity<>(
                Map.of("reason", "Confirmed customer-impacting defect"),
                authHeaders(superAdminToken, ROOT_TENANT)),
            Map.class);
    assertThat(conversion.getStatusCode()).isEqualTo(HttpStatus.OK);
    Map<String, Object> conversionData = data(conversion);
    assertThat(conversionData.get("category")).isEqualTo("BUG");
    assertThat(conversionData.get("convertedToIncidentAt")).isNotNull();
    assertThat(nestedMap(conversionData, "sla").get("status")).isEqualTo("PENDING");
    assertThat(nestedMap(conversionData, "sla").get("policyId")).isEqualTo("STANDARD-LOW");
    assertTimelineEvents(
        conversionData, "TICKET_CREATED", "STATUS_CHANGED", "FEATURE_CONVERTED_TO_INCIDENT");
    assertThat(firstTimelineAudit(conversionData, "FEATURE_CONVERTED_TO_INCIDENT")).isNotNull();

    ResponseEntity<Map> secondConversion =
        rest.exchange(
            "/api/v1/superadmin/support/tickets/" + ticketId + "/convert-to-incident",
            HttpMethod.POST,
            new HttpEntity<>(
                Map.of("reason", "Duplicate conversion probe"),
                authHeaders(superAdminToken, ROOT_TENANT)),
            Map.class);
    assertThat(secondConversion.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

    ResponseEntity<Map> bugQueue =
        rest.exchange(
            "/api/v1/superadmin/support/tickets?category=BUG&q=" + marker,
            HttpMethod.GET,
            new HttpEntity<>(authHeaders(superAdminToken, ROOT_TENANT)),
            Map.class);
    assertThat(bugQueue.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(data(bugQueue).get("totalElements")).isEqualTo(1);
  }

  private Long seedTicket(String companyCode, String userEmail, String subject) {
    return seedTicket(companyCode, userEmail, subject, SupportTicketPriority.NORMAL);
  }

  private Long seedTicket(
      String companyCode, String userEmail, String subject, SupportTicketPriority priority) {
    Company company = companyRepository.findByCodeIgnoreCase(companyCode).orElseThrow();
    UserAccount requester =
        userAccountRepository
            .findByEmailIgnoreCaseAndAuthScopeCodeIgnoreCase(userEmail, companyCode)
            .orElseThrow();

    SupportTicket ticket = new SupportTicket();
    ticket.setCompany(company);
    ticket.setUserId(requester.getId());
    ticket.setCategory(SupportTicketCategory.SUPPORT);
    ticket.setPriority(priority);
    ticket.setSubject(subject);
    ticket.setDescription("Investigate support visibility");
    ticket.setStatus(SupportTicketStatus.OPEN);

    return supportTicketRepository.save(ticket).getId();
  }

  private Long createTenantTicket(
      String tenantToken, String category, String priority, String subject) {
    ResponseEntity<Map> response =
        rest.exchange(
            "/api/v1/admin/support/tickets",
            HttpMethod.POST,
            new HttpEntity<>(
                Map.of(
                    "category",
                    category,
                    "priority",
                    priority,
                    "subject",
                    subject,
                    "description",
                    "SLA recalculation regression coverage"),
                authHeaders(tenantToken, TENANT_A)),
            Map.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    return Long.parseLong(String.valueOf(data(response).get("id")));
  }

  private Map<String, Object> dedicatedCustomPlanPayload() {
    return Map.of(
        "customPlan",
        Map.of(
            "displayName",
            "Dedicated SLA Plan",
            "cadence",
            "CUSTOM",
            "priceMinorUnits",
            0,
            "currency",
            "INR",
            "trialDurationDays",
            0,
            "supportTier",
            "DEDICATED",
            "featureFlags",
            Map.of("PRODUCTION", true, "PORTAL", true, "PURCHASING", true, "REPORTS", true),
            "defaultLimits",
            Map.of(
                "maxActiveUsers",
                25,
                "maxApiRequests",
                250_000,
                "maxStorageBytes",
                10_000_000,
                "maxPdfExports",
                1_000,
                "maxEmails",
                1_000,
                "maxJobs",
                1_000,
                "burstRequestsPerMinute",
                300,
                "maxConcurrentRequests",
                25)),
        "repriceSubscription",
        false,
        "reason",
        "upgrade support tier for SLA recalculation regression");
  }

  private void postMessage(String path, String content, String token, String companyCode) {
    ResponseEntity<Map> response =
        rest.exchange(
            path,
            HttpMethod.POST,
            new HttpEntity<>(Map.of("content", content), authHeaders(token, companyCode)),
            Map.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  private ResponseEntity<Map> superAdminDetail(Long ticketId, String superAdminToken) {
    ResponseEntity<Map> response =
        rest.exchange(
            "/api/v1/superadmin/support/tickets/" + ticketId,
            HttpMethod.GET,
            new HttpEntity<>(authHeaders(superAdminToken, ROOT_TENANT)),
            Map.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    return response;
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> nestedMap(Map<String, Object> data, String key) {
    Object nested = data.get(key);
    assertThat(nested).isInstanceOf(Map.class);
    return (Map<String, Object>) nested;
  }

  private void assertTimelineEvents(Map<String, Object> ticketData, String... expectedEvents) {
    assertThat(timelineEvents(ticketData)).contains(expectedEvents);
  }

  @SuppressWarnings("unchecked")
  private List<String> timelineEvents(Map<String, Object> ticketData) {
    Object timeline = ticketData.get("timeline");
    assertThat(timeline).isInstanceOf(List.class);
    return ((List<Map<String, Object>>) timeline)
        .stream().map(entry -> String.valueOf(entry.get("eventType"))).toList();
  }

  @SuppressWarnings("unchecked")
  private Object firstTimelineAudit(Map<String, Object> ticketData, String eventType) {
    Object timeline = ticketData.get("timeline");
    assertThat(timeline).isInstanceOf(List.class);
    return ((List<Map<String, Object>>) timeline)
        .stream()
            .filter(entry -> eventType.equals(entry.get("eventType")))
            .map(entry -> entry.get("auditEventId"))
            .filter(java.util.Objects::nonNull)
            .findFirst()
            .orElse(null);
  }

  @SuppressWarnings("unchecked")
  private String firstTimelineNote(Map<String, Object> ticketData, String eventType) {
    Object timeline = ticketData.get("timeline");
    assertThat(timeline).isInstanceOf(List.class);
    return ((List<Map<String, Object>>) timeline)
        .stream()
            .filter(entry -> eventType.equals(entry.get("eventType")))
            .map(entry -> String.valueOf(entry.get("note")))
            .findFirst()
            .orElse("");
  }

  private Set<String> subjectsFromListResponse(ResponseEntity<Map> response) {
    assertThat(response.getBody()).isNotNull();
    @SuppressWarnings("unchecked")
    Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
    assertThat(data).isNotNull();

    @SuppressWarnings("unchecked")
    List<Map<String, Object>> tickets = (List<Map<String, Object>>) data.get("tickets");
    return tickets.stream()
        .map(ticket -> String.valueOf(ticket.get("subject")))
        .collect(Collectors.toSet());
  }

  private Map<String, Object> ticketFromListResponse(ResponseEntity<Map> response, Long ticketId) {
    assertThat(response.getBody()).isNotNull();
    @SuppressWarnings("unchecked")
    Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
    assertThat(data).isNotNull();

    @SuppressWarnings("unchecked")
    List<Map<String, Object>> tickets = (List<Map<String, Object>>) data.get("tickets");
    return tickets.stream()
        .filter(ticket -> Long.valueOf(String.valueOf(ticket.get("id"))).equals(ticketId))
        .findFirst()
        .orElseThrow();
  }

  private void assertMessagesAreBoundedPreview(Map<String, Object> ticketData, int expectedSize) {
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> messages = (List<Map<String, Object>>) ticketData.get("messages");
    assertThat(messages).hasSize(expectedSize);
  }

  private HttpHeaders authHeaders(String token, String companyCode) {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("X-Company-Code", companyCode);
    return headers;
  }

  private HttpHeaders jsonOnlyHeaders() {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    return headers;
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> data(ResponseEntity<Map> response) {
    assertThat(response.getBody()).isNotNull();
    Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
    assertThat(data).isNotNull();
    return data;
  }

  private String login(String email, String companyCode) {
    ResponseEntity<Map> response =
        rest.postForEntity(
            "/api/v1/auth/login",
            Map.of(
                "email", email,
                "password", PASSWORD,
                "companyCode", companyCode),
            Map.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    return String.valueOf(response.getBody().get("accessToken"));
  }

  @SuppressWarnings("unchecked")
  private void assertForbiddenPlatformOnly(ResponseEntity<Map> response) {
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().get("success")).isEqualTo(Boolean.FALSE);
    assertThat(response.getBody().get("message")).isEqualTo("Access denied");
    Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
    assertThat(data).isNotNull();
    assertThat(data.get("code")).isEqualTo("AUTH_004");
    assertThat(data.get("reason")).isEqualTo("SUPER_ADMIN_PLATFORM_ONLY");
  }
}
