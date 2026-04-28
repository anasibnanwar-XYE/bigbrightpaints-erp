package com.bigbrightpaints.erp.core.validationharness;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

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
}
