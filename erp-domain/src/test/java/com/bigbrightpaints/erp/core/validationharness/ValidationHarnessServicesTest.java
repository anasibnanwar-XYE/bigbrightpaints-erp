package com.bigbrightpaints.erp.core.validationharness;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.bigbrightpaints.erp.core.audit.AuditEvent;
import com.bigbrightpaints.erp.core.audit.AuditLog;
import com.bigbrightpaints.erp.core.audit.AuditService;
import com.bigbrightpaints.erp.core.audit.AuditStatus;

@Tag("critical")
class ValidationHarnessServicesTest {

  @Test
  void runMarkersRejectUnsafeNamespaces() {
    assertThat(ValidationRunNamespace.requireSafeRunMarker("m0-run_2026:04.28"))
        .isEqualTo("m0-run_2026:04.28");

    assertThatThrownBy(() -> ValidationRunNamespace.requireSafeRunMarker("../.env"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("runMarker");
    assertThatThrownBy(() -> ValidationRunNamespace.requireSafeRunMarker("bad\nmarker"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void faultInjectionCoversExpectedValidationFaultMatrixWithSafeStateOnly() {
    ValidationFaultInjectionService service = new ValidationFaultInjectionService();

    assertThat(service.states())
        .extracting(ValidationFaultInjectionService.FaultState::code)
        .containsExactly(
            "audit-failure",
            "audit-signing-failure",
            "datadog-failure",
            "health-degraded",
            "partial-seed-failure",
            "seed-failure",
            "sentry-failure",
            "smtp-failure");

    ValidationFaultInjectionService.FaultState state =
        service.setFault(
            ValidationFaultKind.SMTP_FAILURE,
            true,
            "m0-validation-run-01",
            "mailhog down\nshould sanitize");

    assertThat(state.enabled()).isTrue();
    assertThat(state.validationOnly()).isTrue();
    assertThat(state.runMarker()).isEqualTo("m0-validation-run-01");
    assertThat(state.reason()).isEqualTo("mailhog down should sanitize");
    assertThat(service.isEnabled(ValidationFaultKind.SMTP_FAILURE)).isTrue();
  }

  @Test
  void timeControlFreezesAndClearsInstantBySafeRunMarker() {
    ValidationTimeControlService service = new ValidationTimeControlService();
    Instant instant = Instant.parse("2026-04-28T00:00:00Z");

    ValidationTimeControlService.TimeState frozen = service.freeze("m0-time-boundary", instant);

    assertThat(frozen.enabled()).isTrue();
    assertThat(frozen.validationOnly()).isTrue();
    assertThat(service.fixedInstant()).contains(instant);

    ValidationTimeControlService.TimeState cleared = service.clear("m0-time-boundary");
    assertThat(cleared.enabled()).isFalse();
    assertThat(service.fixedInstant()).isEmpty();
  }

  @Test
  void healthFaultDrivesValidationOnlyHealthDown() {
    ValidationFaultInjectionService service = new ValidationFaultInjectionService();
    ValidationFaultHealthIndicator indicator = new ValidationFaultHealthIndicator(service);

    assertThat(indicator.health().getStatus().getCode()).isEqualTo("UP");

    service.setFault(ValidationFaultKind.HEALTH_DEGRADED, true, "m0-health", "fixture");

    assertThat(indicator.health().getStatus().getCode()).isEqualTo("DOWN");
    assertThat(indicator.health().getDetails())
        .containsEntry("validationOnly", true)
        .containsEntry("fault", "health-degraded")
        .containsEntry("runMarker", "m0-health");
  }

  @Test
  @SuppressWarnings("unchecked")
  void securityAlertTriggerWritesSynchronousValidationOnlyAlertWithReference() {
    AuditService auditService = mock(AuditService.class);
    AuditLog saved = new AuditLog();
    saved.setId(720L);
    saved.setEventType(AuditEvent.SECURITY_ALERT);
    saved.setStatus(AuditStatus.WARNING);
    saved.setTimestamp(LocalDateTime.parse("2026-04-30T02:13:47"));
    saved.setTraceId("trace-m14-validation");
    when(auditService.logSecurityAlertNow(eq("M14_VALIDATION_ALERT"), any(), any()))
        .thenReturn(saved);

    ValidationSecurityAlertTriggerService service =
        new ValidationSecurityAlertTriggerService(auditService);
    ValidationSecurityAlertTriggerService.TriggerResult result =
        service.trigger("M14FIXTURE20260430021347", "m14_validation_alert", "M14_REASON");

    ArgumentCaptor<Map<String, String>> metadataCaptor = ArgumentCaptor.forClass(Map.class);
    verify(auditService)
        .logSecurityAlertNow(
            eq("M14_VALIDATION_ALERT"),
            eq("Validation harness security alert trigger"),
            metadataCaptor.capture());
    assertThat(metadataCaptor.getValue())
        .containsEntry("source", "validation-harness")
        .containsEntry("reference", "M14FIXTURE20260430021347")
        .containsEntry("reason", "M14_REASON")
        .containsEntry("validationOnly", "true");
    assertThat(result.validationOnly()).isTrue();
    assertThat(result.eventId()).isEqualTo(720L);
    assertThat(result.eventType()).isEqualTo("SECURITY_ALERT");
    assertThat(result.auditStatus()).isEqualTo("WARNING");
    assertThat(result.traceId()).isEqualTo("trace-m14-validation");
    assertThat(result.toString())
        .doesNotContain("password")
        .doesNotContain("token")
        .doesNotContain("secret");
  }

  @Test
  void securityAlertTriggerRejectsSecretBearingReasonCodes() {
    ValidationSecurityAlertTriggerService service =
        new ValidationSecurityAlertTriggerService(mock(AuditService.class));

    assertThatThrownBy(() -> service.trigger("M14SAFE", "M14_ALERT", "password-reset-token"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("secret-bearing");
  }
}
