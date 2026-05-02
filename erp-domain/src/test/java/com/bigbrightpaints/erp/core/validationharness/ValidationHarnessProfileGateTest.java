package com.bigbrightpaints.erp.core.validationharness;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Profile;
import org.springframework.mock.env.MockEnvironment;

import com.bigbrightpaints.erp.core.audit.AuditService;

@Tag("critical")
class ValidationHarnessProfileGateTest {

  @Test
  void controllerAndHelpersRequireExplicitValidationHarnessProfile() {
    assertThat(ValidationHarnessController.class.getAnnotation(Profile.class).value())
        .containsExactly("validation-harness");
    assertThat(ValidationFaultInjectionService.class.getAnnotation(Profile.class).value())
        .containsExactly("validation-harness");
    assertThat(ValidationTimeControlService.class.getAnnotation(Profile.class).value())
        .containsExactly("validation-harness");
    assertThat(ValidationFaultHealthIndicator.class.getAnnotation(Profile.class).value())
        .containsExactly("validation-harness");
    assertThat(ValidationSecurityAlertTriggerService.class.getAnnotation(Profile.class).value())
        .containsExactly("validation-harness");
  }

  @Test
  void controllerReturnsSafeStatusAndNoSecretBearingFields() {
    ValidationFaultInjectionService faultInjectionService = new ValidationFaultInjectionService();
    ValidationTimeControlService timeControlService = new ValidationTimeControlService();
    MockEnvironment environment = new MockEnvironment();
    environment.setActiveProfiles("prod", "flyway-v2", "validation-harness");
    ValidationHarnessController controller =
        new ValidationHarnessController(
            faultInjectionService,
            timeControlService,
            new ValidationSecurityAlertTriggerService(mock(AuditService.class)),
            environment);

    controller.freezeTime(
        new ValidationHarnessController.TimeControlRequest(
            "m0-validation-run", Instant.parse("2026-04-28T00:00:00Z")));
    controller.setFault(
        "audit-failure",
        new ValidationHarnessController.FaultRequest("m0-validation-run", true, "audit down"));

    ValidationHarnessController.HarnessStatus status =
        controller.status("m0-validation-run").data();

    assertThat(status.validationOnly()).isTrue();
    assertThat(status.requiredProfile()).isEqualTo("validation-harness");
    assertThat(status.activeProfiles()).contains("validation-harness");
    assertThat(status.evidenceSummary())
        .containsAllEntriesOf(
            Map.of("validationOnly", true, "requiredProfile", "validation-harness"));
    assertThat(status.toString())
        .doesNotContain("password")
        .doesNotContain("token")
        .doesNotContain("secret");
  }
}
