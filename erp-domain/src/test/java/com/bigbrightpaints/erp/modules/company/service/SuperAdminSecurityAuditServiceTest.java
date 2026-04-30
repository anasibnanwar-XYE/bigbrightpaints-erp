package com.bigbrightpaints.erp.modules.company.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.bigbrightpaints.erp.core.audit.AuditEvent;
import com.bigbrightpaints.erp.core.audit.AuditLog;
import com.bigbrightpaints.erp.core.audit.AuditLogRepository;
import com.bigbrightpaints.erp.core.audit.AuditService;
import com.bigbrightpaints.erp.core.audit.AuditStatus;
import com.bigbrightpaints.erp.core.auditaccess.AuditAccessService;
import com.bigbrightpaints.erp.core.auditaccess.AuditFeedFilter;
import com.bigbrightpaints.erp.core.auditaccess.dto.AuditFeedItemDto;
import com.bigbrightpaints.erp.core.exception.ApplicationException;
import com.bigbrightpaints.erp.modules.company.domain.SuperAdminSecurityRemediation;
import com.bigbrightpaints.erp.modules.company.domain.SuperAdminSecurityRemediationRepository;
import com.bigbrightpaints.erp.modules.company.dto.SuperAdminSecurityAuditDtos;
import com.bigbrightpaints.erp.shared.dto.PageResponse;

class SuperAdminSecurityAuditServiceTest {

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void listSecurityEventsAddsSafeRemediationAndHashesActorIdentifier() {
    AuditAccessService auditAccessService = mock(AuditAccessService.class);
    AuditLogRepository auditLogRepository = mock(AuditLogRepository.class);
    SuperAdminSecurityRemediationRepository remediationRepository =
        mock(SuperAdminSecurityRemediationRepository.class);
    SuperAdminSecurityAuditService service =
        new SuperAdminSecurityAuditService(
            auditAccessService,
            auditLogRepository,
            remediationRepository,
            mock(AuditService.class));
    AuditFeedFilter filter =
        new AuditFeedFilter(null, null, null, null, null, null, null, null, 0, 50);
    when(auditAccessService.queryPlatformSecurityFeed(filter, false))
        .thenReturn(
            PageResponse.of(
                List.of(
                    new AuditFeedItemDto(
                        101L,
                        "AUDIT_LOG",
                        "SECURITY",
                        Instant.parse("2026-04-30T00:00:00Z"),
                        7L,
                        "ACME",
                        "SECURITY",
                        "SECURITY_ALERT",
                        "WARNING",
                        null,
                        "root-superadmin@example.test",
                        null,
                        null,
                        "SECURITY_EVENT",
                        "101",
                        "101",
                        "POST",
                        "/api/v1/auth/login",
                        "trace-1",
                        Map.of("alertType", "BRUTE_FORCE_ATTACK"))),
                1,
                0,
                50));
    SuperAdminSecurityRemediation remediation = remediation(5L, 101L, "ACKNOWLEDGED", 501L);
    when(remediationRepository.findByAuditEventIdIn(List.of(101L)))
        .thenReturn(List.of(remediation));

    PageResponse<SuperAdminSecurityAuditDtos.SecurityEventResponse> response =
        service.listSecurityEvents(filter);

    assertThat(response.content())
        .singleElement()
        .satisfies(
            event -> {
              assertThat(event.eventId()).isEqualTo(101L);
              assertThat(event.severity()).isEqualTo("HIGH");
              assertThat(event.actorIdentifier()).startsWith("hash:");
              assertThat(event.actorIdentifier()).doesNotContain("root-superadmin");
              assertThat(event.remediation().status()).isEqualTo("ACKNOWLEDGED");
              assertThat(event.metadata()).containsEntry("alertType", "BRUTE_FORCE_ATTACK");
            });
  }

  @Test
  void resolveTransitionsRemediationAndAuditsWithoutPrivateReasonText() {
    AuditAccessService auditAccessService = mock(AuditAccessService.class);
    AuditLogRepository auditLogRepository = mock(AuditLogRepository.class);
    SuperAdminSecurityRemediationRepository remediationRepository =
        mock(SuperAdminSecurityRemediationRepository.class);
    AuditService auditService = mock(AuditService.class);
    SuperAdminSecurityAuditService service =
        new SuperAdminSecurityAuditService(
            auditAccessService, auditLogRepository, remediationRepository, auditService);
    AuditLog securityAlert = auditLog(101L, AuditEvent.SECURITY_ALERT);
    when(auditLogRepository.findById(101L)).thenReturn(Optional.of(securityAlert));
    SuperAdminSecurityRemediation remediation = remediation(5L, 101L, "ACKNOWLEDGED", null);
    when(remediationRepository.lockByAuditEventId(101L)).thenReturn(Optional.of(remediation));
    AuditLog auditEvidence = new AuditLog();
    auditEvidence.setId(777L);
    when(auditService.logAuthSuccessRequired(any(), any(), any(), any())).thenReturn(auditEvidence);
    when(remediationRepository.saveAndFlush(any(SuperAdminSecurityRemediation.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    SecurityContextHolder.getContext()
        .setAuthentication(new TestingAuthenticationToken("root-superadmin@example.test", "n/a"));

    SuperAdminSecurityAuditDtos.RemediationResponse response =
        service.resolve(101L, new SuperAdminSecurityAuditDtos.RemediationRequest("reviewed"));

    assertThat(response.status()).isEqualTo("RESOLVED");
    assertThat(response.auditEventId()).isEqualTo(777L);
    verify(auditService).logAuthSuccessRequired(any(), any(), any(), any());
  }

  @Test
  void remediationReasonRejectsSecretAndPrivateTenantBusinessCanaries() {
    SuperAdminSecurityAuditService service =
        new SuperAdminSecurityAuditService(
            mock(AuditAccessService.class),
            mock(AuditLogRepository.class),
            mock(SuperAdminSecurityRemediationRepository.class),
            mock(AuditService.class));

    assertThatThrownBy(
            () ->
                service.resolve(
                    101L,
                    new SuperAdminSecurityAuditDtos.RemediationRequest(
                        "contains password token invoice ledger inventory salary vendor customer")))
        .isInstanceOf(ApplicationException.class);
  }

  private AuditLog auditLog(Long id, AuditEvent event) {
    AuditLog auditLog = new AuditLog();
    auditLog.setId(id);
    auditLog.setEventType(event);
    auditLog.setTimestamp(LocalDateTime.of(2026, 4, 30, 0, 0));
    auditLog.setStatus(AuditStatus.WARNING);
    auditLog.setMetadata(new HashMap<>());
    return auditLog;
  }

  private SuperAdminSecurityRemediation remediation(
      Long id, Long auditEventId, String status, Long lastAuditEventId) {
    SuperAdminSecurityRemediation remediation = new SuperAdminSecurityRemediation();
    remediation.setAuditEventId(auditEventId);
    remediation.setStatus(status);
    remediation.setSeverity("HIGH");
    remediation.setReason("reviewed");
    remediation.setUpdatedBy("operator@example.test");
    remediation.setLastAuditEventId(lastAuditEventId);
    try {
      java.lang.reflect.Field idField = SuperAdminSecurityRemediation.class.getDeclaredField("id");
      idField.setAccessible(true);
      idField.set(remediation, id);
    } catch (ReflectiveOperationException ex) {
      throw new AssertionError(ex);
    }
    return remediation;
  }
}
