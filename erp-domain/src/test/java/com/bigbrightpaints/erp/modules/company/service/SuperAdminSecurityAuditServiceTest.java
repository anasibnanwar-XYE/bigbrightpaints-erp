package com.bigbrightpaints.erp.modules.company.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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
import org.mockito.ArgumentCaptor;
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
              assertThat(event.remediation().reason()).isEqualTo("OPERATOR_NOTE_PRESENT");
              assertThat(event.remediation().reason()).doesNotContain("reviewed");
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
    when(auditLogRepository.lockByIdWithMetadata(101L)).thenReturn(Optional.of(securityAlert));
    SuperAdminSecurityRemediation remediation = remediation(5L, 101L, "ACKNOWLEDGED", null);
    when(remediationRepository.lockByAuditEventId(101L)).thenReturn(Optional.of(remediation));
    AuditLog auditEvidence = new AuditLog();
    auditEvidence.setId(777L);
    when(auditService.logAuthSuccessRequired(any(), any(), any(), any())).thenReturn(auditEvidence);
    when(remediationRepository.saveAndFlush(any(SuperAdminSecurityRemediation.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    authenticateSuperAdmin();

    SuperAdminSecurityAuditDtos.RemediationResponse response =
        service.resolve(
            101L,
            new SuperAdminSecurityAuditDtos.RemediationRequest(
                "Reviewed by platform operator with private context"));

    assertThat(response.status()).isEqualTo("RESOLVED");
    assertThat(response.reason()).isEqualTo("OPERATOR_NOTE_PRESENT");
    assertThat(response.auditEventId()).isEqualTo(777L);
    ArgumentCaptor<Map<String, String>> metadataCaptor = metadataCaptor();
    verify(auditService).logAuthSuccessRequired(any(), any(), any(), metadataCaptor.capture());
    assertThat(metadataCaptor.getValue())
        .containsEntry("reasonCategory", "OPERATOR_NOTE")
        .containsEntry("reasonPresent", "true")
        .containsKeys("reasonDigest")
        .doesNotContainKeys("reasonText");
    assertThat(metadataCaptor.getValue().toString())
        .doesNotContain("Reviewed by platform operator", "private context");
  }

  @Test
  void exactRetryReturnsExistingAuditBackedRemediationWithoutDuplicateAudit() {
    AuditLogRepository auditLogRepository = mock(AuditLogRepository.class);
    SuperAdminSecurityRemediationRepository remediationRepository =
        mock(SuperAdminSecurityRemediationRepository.class);
    AuditService auditService = mock(AuditService.class);
    SuperAdminSecurityAuditService service =
        new SuperAdminSecurityAuditService(
            mock(AuditAccessService.class),
            auditLogRepository,
            remediationRepository,
            auditService);
    AuditLog securityAlert = auditLog(101L, AuditEvent.SECURITY_ALERT);
    when(auditLogRepository.lockByIdWithMetadata(101L)).thenReturn(Optional.of(securityAlert));
    SuperAdminSecurityRemediation remediation = remediation(5L, 101L, "RESOLVED", 777L);
    remediation.setReason("reviewed by operator");
    when(remediationRepository.lockByAuditEventId(101L)).thenReturn(Optional.of(remediation));
    authenticateSuperAdmin();

    SuperAdminSecurityAuditDtos.RemediationResponse response =
        service.resolve(
            101L, new SuperAdminSecurityAuditDtos.RemediationRequest("  Reviewed   By Operator  "));

    assertThat(response.status()).isEqualTo("RESOLVED");
    assertThat(response.auditEventId()).isEqualTo(777L);
    verify(auditService, never()).logAuthSuccessRequired(any(), any(), any(), any());
    verify(remediationRepository, never()).saveAndFlush(any(SuperAdminSecurityRemediation.class));
  }

  @Test
  void sameStatusReasonCorrectionAppendsOneAuditEventAndPreservesPreviousEvidenceReference() {
    AuditLogRepository auditLogRepository = mock(AuditLogRepository.class);
    SuperAdminSecurityRemediationRepository remediationRepository =
        mock(SuperAdminSecurityRemediationRepository.class);
    AuditService auditService = mock(AuditService.class);
    SuperAdminSecurityAuditService service =
        new SuperAdminSecurityAuditService(
            mock(AuditAccessService.class),
            auditLogRepository,
            remediationRepository,
            auditService);
    AuditLog securityAlert = auditLog(101L, AuditEvent.SECURITY_ALERT);
    when(auditLogRepository.lockByIdWithMetadata(101L)).thenReturn(Optional.of(securityAlert));
    SuperAdminSecurityRemediation remediation = remediation(5L, 101L, "RESOLVED", 777L);
    remediation.setReason("first operator note");
    when(remediationRepository.lockByAuditEventId(101L)).thenReturn(Optional.of(remediation));
    AuditLog auditEvidence = new AuditLog();
    auditEvidence.setId(778L);
    when(auditService.logAuthSuccessRequired(any(), any(), any(), any())).thenReturn(auditEvidence);
    when(remediationRepository.saveAndFlush(any(SuperAdminSecurityRemediation.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    authenticateSuperAdmin();

    SuperAdminSecurityAuditDtos.RemediationResponse response =
        service.resolve(101L, new SuperAdminSecurityAuditDtos.RemediationRequest("corrected note"));

    assertThat(response.status()).isEqualTo("RESOLVED");
    assertThat(response.auditEventId()).isEqualTo(778L);
    ArgumentCaptor<Map<String, String>> metadataCaptor = metadataCaptor();
    verify(auditService).logAuthSuccessRequired(any(), any(), any(), metadataCaptor.capture());
    assertThat(metadataCaptor.getValue())
        .containsEntry("previousAuditEventId", "777")
        .containsEntry("previousStatus", "RESOLVED")
        .containsEntry("newStatus", "RESOLVED")
        .containsKeys("reasonDigest")
        .doesNotContainKeys("reasonText");
  }

  @Test
  void resolveAuditFailureDoesNotPersistRemediationTransition() {
    AuditLogRepository auditLogRepository = mock(AuditLogRepository.class);
    SuperAdminSecurityRemediationRepository remediationRepository =
        mock(SuperAdminSecurityRemediationRepository.class);
    AuditService auditService = mock(AuditService.class);
    SuperAdminSecurityAuditService service =
        new SuperAdminSecurityAuditService(
            mock(AuditAccessService.class),
            auditLogRepository,
            remediationRepository,
            auditService);
    AuditLog securityAlert = auditLog(101L, AuditEvent.SECURITY_ALERT);
    when(auditLogRepository.lockByIdWithMetadata(101L)).thenReturn(Optional.of(securityAlert));
    SuperAdminSecurityRemediation remediation = remediation(5L, 101L, "ACKNOWLEDGED", null);
    when(remediationRepository.lockByAuditEventId(101L)).thenReturn(Optional.of(remediation));
    when(auditService.logAuthSuccessRequired(any(), any(), any(), any()))
        .thenThrow(new IllegalStateException("required audit signing unavailable"));
    authenticateSuperAdmin();

    assertThatThrownBy(
            () ->
                service.resolve(
                    101L, new SuperAdminSecurityAuditDtos.RemediationRequest("closed by operator")))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("required audit signing unavailable");

    assertThat(remediation.getStatus()).isEqualTo("ACKNOWLEDGED");
    assertThat(remediation.getLastAuditEventId()).isNull();
    verify(remediationRepository, never()).saveAndFlush(any(SuperAdminSecurityRemediation.class));
  }

  @Test
  void resolveNullRequiredAuditResultDoesNotPersistRemediationTransition() {
    AuditLogRepository auditLogRepository = mock(AuditLogRepository.class);
    SuperAdminSecurityRemediationRepository remediationRepository =
        mock(SuperAdminSecurityRemediationRepository.class);
    AuditService auditService = mock(AuditService.class);
    SuperAdminSecurityAuditService service =
        new SuperAdminSecurityAuditService(
            mock(AuditAccessService.class),
            auditLogRepository,
            remediationRepository,
            auditService);
    AuditLog securityAlert = auditLog(101L, AuditEvent.SECURITY_ALERT);
    when(auditLogRepository.lockByIdWithMetadata(101L)).thenReturn(Optional.of(securityAlert));
    SuperAdminSecurityRemediation remediation = remediation(5L, 101L, "ACKNOWLEDGED", null);
    when(remediationRepository.lockByAuditEventId(101L)).thenReturn(Optional.of(remediation));
    when(auditService.logAuthSuccessRequired(any(), any(), any(), any())).thenReturn(null);
    authenticateSuperAdmin();

    assertThatThrownBy(
            () ->
                service.resolve(
                    101L, new SuperAdminSecurityAuditDtos.RemediationRequest("closed by operator")))
        .isInstanceOf(ApplicationException.class)
        .hasMessageContaining("Security remediation audit event was not persisted");

    assertThat(remediation.getStatus()).isEqualTo("ACKNOWLEDGED");
    assertThat(remediation.getLastAuditEventId()).isNull();
    verify(remediationRepository, never()).saveAndFlush(any(SuperAdminSecurityRemediation.class));
  }

  @Test
  void resolveRecoveryAfterAuditFailureCreatesExactlyOneAuditBackedTransition() {
    AuditLogRepository auditLogRepository = mock(AuditLogRepository.class);
    SuperAdminSecurityRemediationRepository remediationRepository =
        mock(SuperAdminSecurityRemediationRepository.class);
    AuditService auditService = mock(AuditService.class);
    SuperAdminSecurityAuditService service =
        new SuperAdminSecurityAuditService(
            mock(AuditAccessService.class),
            auditLogRepository,
            remediationRepository,
            auditService);
    AuditLog securityAlert = auditLog(101L, AuditEvent.SECURITY_ALERT);
    when(auditLogRepository.lockByIdWithMetadata(101L)).thenReturn(Optional.of(securityAlert));
    SuperAdminSecurityRemediation remediation = remediation(5L, 101L, "ACKNOWLEDGED", null);
    when(remediationRepository.lockByAuditEventId(101L)).thenReturn(Optional.of(remediation));
    AuditLog auditEvidence = new AuditLog();
    auditEvidence.setId(778L);
    when(auditService.logAuthSuccessRequired(any(), any(), any(), any()))
        .thenThrow(new IllegalStateException("required audit persistence unavailable"))
        .thenReturn(auditEvidence);
    when(remediationRepository.saveAndFlush(any(SuperAdminSecurityRemediation.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    authenticateSuperAdmin();

    assertThatThrownBy(
            () ->
                service.resolve(
                    101L, new SuperAdminSecurityAuditDtos.RemediationRequest("reviewed")))
        .isInstanceOf(IllegalStateException.class);
    remediation.setStatus("ACKNOWLEDGED");
    remediation.setLastAuditEventId(null);

    SuperAdminSecurityAuditDtos.RemediationResponse response =
        service.resolve(101L, new SuperAdminSecurityAuditDtos.RemediationRequest("reviewed"));

    assertThat(response.status()).isEqualTo("RESOLVED");
    assertThat(response.auditEventId()).isEqualTo(778L);
    verify(remediationRepository).saveAndFlush(remediation);
  }

  @Test
  void remediationReasonRejectsSecretAndPrivateTenantBusinessCanaries() {
    AuditLogRepository auditLogRepository = mock(AuditLogRepository.class);
    SuperAdminSecurityAuditService service =
        new SuperAdminSecurityAuditService(
            mock(AuditAccessService.class),
            auditLogRepository,
            mock(SuperAdminSecurityRemediationRepository.class),
            mock(AuditService.class));
    when(auditLogRepository.lockByIdWithMetadata(101L))
        .thenReturn(Optional.of(auditLog(101L, AuditEvent.SECURITY_ALERT)));
    authenticateSuperAdmin();

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

  private void authenticateSuperAdmin() {
    SecurityContextHolder.getContext()
        .setAuthentication(new TestingAuthenticationToken("root-superadmin@example.test", "n/a"));
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

  @SuppressWarnings("unchecked")
  private ArgumentCaptor<Map<String, String>> metadataCaptor() {
    return ArgumentCaptor.forClass((Class<Map<String, String>>) (Class<?>) Map.class);
  }
}
