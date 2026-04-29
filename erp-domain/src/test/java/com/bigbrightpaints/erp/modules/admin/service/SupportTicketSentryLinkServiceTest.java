package com.bigbrightpaints.erp.modules.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.bigbrightpaints.erp.core.audit.AuditEvent;
import com.bigbrightpaints.erp.core.audit.AuditLog;
import com.bigbrightpaints.erp.core.audit.AuditService;
import com.bigbrightpaints.erp.core.exception.ApplicationException;
import com.bigbrightpaints.erp.core.exception.ErrorCode;
import com.bigbrightpaints.erp.core.util.CompanyClock;
import com.bigbrightpaints.erp.core.util.CompanyTime;
import com.bigbrightpaints.erp.modules.admin.domain.SupportTicket;
import com.bigbrightpaints.erp.modules.admin.domain.SupportTicketCategory;
import com.bigbrightpaints.erp.modules.admin.domain.SupportTicketRepository;
import com.bigbrightpaints.erp.modules.admin.dto.SuperAdminSupportTicketDtos;
import com.bigbrightpaints.erp.modules.company.domain.Company;

class SupportTicketSentryLinkServiceTest {

  private SupportTicketRepository supportTicketRepository;
  private SentryIssueClient sentryIssueClient;
  private SupportTicketSentryLinkService service;

  @BeforeEach
  void setUp() {
    installCompanyTime(Instant.parse("2026-04-29T12:00:00Z"));
    supportTicketRepository = Mockito.mock(SupportTicketRepository.class);
    SupportTicketAccessSupport supportTicketAccessSupport =
        Mockito.mock(SupportTicketAccessSupport.class);
    sentryIssueClient = Mockito.mock(SentryIssueClient.class);
    AuditService auditService = Mockito.mock(AuditService.class);
    BugReportMetadataSanitizer sanitizer = new BugReportMetadataSanitizer(new ObjectMapper());
    service =
        new SupportTicketSentryLinkService(
            supportTicketRepository,
            supportTicketAccessSupport,
            sentryIssueClient,
            sanitizer,
            auditService);
    when(supportTicketAccessSupport.requireTicketId(any()))
        .thenAnswer(invocation -> invocation.getArgument(0));
    AuditLog auditLog = new AuditLog();
    auditLog.setId(9001L);
    when(auditService.logAuthSuccessRequired(eq(AuditEvent.DATA_UPDATE), any(), any(), anyMap()))
        .thenReturn(auditLog);
    SecurityContextHolder.getContext()
        .setAuthentication(new TestingAuthenticationToken("superadmin@example.test", "n/a"));
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void linkRejectsUrlsBeforePersistingSentryState() {
    SupportTicket ticket = ticket(101L);
    when(supportTicketRepository.findById(101L)).thenReturn(Optional.of(ticket));

    assertThatThrownBy(
            () ->
                service.link(
                    101L,
                    new SuperAdminSupportTicketDtos.SentryLinkRequest(
                        "https://169.254.169.254/latest/meta-data")))
        .isInstanceOf(ApplicationException.class)
        .hasMessageContaining("not a URL");

    assertThat(ticket.getSentryIssueId()).isNull();
  }

  @Test
  void syncFailureStoresSanitizedErrorWithoutMarkingSuccess() {
    SupportTicket ticket = ticket(102L);
    ticket.setSentryIssueId("ERP-102");
    ticket.setSentryIssueStatus("LINKED");
    when(supportTicketRepository.findById(102L)).thenReturn(Optional.of(ticket));
    when(supportTicketRepository.saveAndFlush(ticket)).thenReturn(ticket);
    when(sentryIssueClient.fetchIssue("ERP-102"))
        .thenThrow(
            new ApplicationException(
                ErrorCode.INTEGRATION_CONNECTION_FAILED,
                "Sentry auth failed Bearer should-not-print"));

    SuperAdminSupportTicketDtos.SentryLinkResponse response = service.sync(102L);

    assertThat(response.status()).isEqualTo("LINKED");
    assertThat(response.syncedAt()).isNull();
    assertThat(response.lastSyncAt()).isNotNull();
    assertThat(response.lastError()).contains("[redacted]").doesNotContain("should-not-print");
    verify(supportTicketRepository).saveAndFlush(ticket);
  }

  @Test
  void syncSuccessExposesOnlySafeSentryFieldsAndMetadata() {
    SupportTicket ticket = ticket(103L);
    ticket.setSentryIssueId("ERP-103");
    ticket.setBugEnvironment("prod");
    ticket.setBugRelease("erp-domain@2026.04");
    ticket.setBugTraceId("trace-103");
    ticket.setBugMetadataJson(
        "{\"route\":\"/api/v1/superadmin/support/tickets/{ticketId}\",\"status\":\"5xx\"}");
    when(supportTicketRepository.findById(103L)).thenReturn(Optional.of(ticket));
    when(supportTicketRepository.saveAndFlush(ticket)).thenReturn(ticket);
    when(sentryIssueClient.fetchIssue("ERP-103"))
        .thenReturn(
            new SentryIssueClient.SentryIssueResult(
                "ERP-103",
                "https://sentry.io/organizations/bbp/issues/ERP-103/",
                "RESOLVED",
                Instant.parse("2026-04-29T12:01:00Z")));

    SuperAdminSupportTicketDtos.SentryLinkResponse response = service.sync(103L);

    assertThat(response.issueId()).isEqualTo("ERP-103");
    assertThat(response.issueUrl()).contains("sentry.io").doesNotContain("auth");
    assertThat(response.status()).isEqualTo("RESOLVED");
    assertThat(response.lastError()).isNull();
    assertThat(response.safeMetadata())
        .containsKeys("environment", "release", "traceId", "route", "tenantHash", "actorHash")
        .doesNotContainKeys("tenant", "userId", "email");
    assertThat(response.auditEventId()).isEqualTo(9001L);
  }

  @Test
  void responseRejectsUnsafeStoredSentryMetadataValues() {
    SupportTicket ticket = ticket(104L);
    ticket.setSentryIssueId("ERP-104");
    ticket.setBugEnvironment("privacy-probe@example.invalid");
    ticket.setBugRelease("ACME-01");
    ticket.setBugTraceId("trace-104");
    ticket.setBugMetadataJson(
        "{\"route\":\"/api/v1/superadmin/support/tickets/{ticketId}\","
            + "\"status\":\"5xx\","
            + "\"component\":\"tenant-private-canary\"}");

    assertThatThrownBy(() -> service.response(ticket, 9002L))
        .isInstanceOf(ApplicationException.class)
        .hasMessageContaining("Stored bug metadata violates current privacy contract");
  }

  private SupportTicket ticket(Long id) {
    Company company = new Company();
    ReflectionTestUtils.setField(company, "id", 71L);
    company.setCode("TENANT-RAW");
    company.setName("Tenant Raw Legal Name");
    SupportTicket ticket = new SupportTicket();
    ReflectionTestUtils.setField(ticket, "id", id);
    ticket.setCompany(company);
    ticket.setUserId(501L);
    ticket.setCategory(SupportTicketCategory.BUG);
    ticket.setSubject("Bounded bug report");
    ticket.setDescription("Safe description");
    return ticket;
  }

  private void installCompanyTime(Instant now) {
    CompanyClock companyClock = Mockito.mock(CompanyClock.class);
    LocalDate today = LocalDate.ofInstant(now, ZoneOffset.UTC);
    when(companyClock.now(any())).thenReturn(now);
    when(companyClock.now(null)).thenReturn(now);
    when(companyClock.today(any())).thenReturn(today);
    when(companyClock.today(null)).thenReturn(today);
    new CompanyTime(companyClock);
  }
}
