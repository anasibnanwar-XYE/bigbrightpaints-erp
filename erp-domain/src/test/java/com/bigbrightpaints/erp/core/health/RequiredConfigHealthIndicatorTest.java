package com.bigbrightpaints.erp.core.health;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

class RequiredConfigHealthIndicatorTest {

  @Test
  void healthUpWhenAllRequiredConfigurationPresent() {
    RequiredConfigHealthIndicator indicator =
        new RequiredConfigHealthIndicator(
            "12345678901234567890123456789012",
            "abcdefghijklmnopqrstuvwxyz123456",
            true,
            "license-key",
            true,
            "smtp-relay.example.com",
            "mailer-user",
            "secret-password",
            true);

    Health health = indicator.health();

    assertThat(health.getStatus()).isEqualTo(Status.UP);
    assertThat(health.getDetails()).containsEntry("mailConfigured", true);
  }

  @Test
  void healthDownWhenMailUsernameMissingWhileMailEnabled() {
    RequiredConfigHealthIndicator indicator =
        new RequiredConfigHealthIndicator(
            "12345678901234567890123456789012",
            "abcdefghijklmnopqrstuvwxyz123456",
            false,
            "",
            true,
            "smtp-relay.example.com",
            "",
            "secret-password",
            true);

    Health health = indicator.health();

    assertThat(health.getStatus()).isEqualTo(Status.DOWN);
    assertThat(health.getDetails()).containsEntry("mailConfigured", false);
    assertThat((List<String>) health.getDetails().get("missing"))
        .contains("spring.mail.host/username/password");
  }

  @Test
  void healthUpWhenMailDisabledEvenWithoutCredentials() {
    RequiredConfigHealthIndicator indicator =
        new RequiredConfigHealthIndicator(
            "12345678901234567890123456789012",
            "abcdefghijklmnopqrstuvwxyz123456",
            false,
            "",
            false,
            "",
            "",
            "",
            true);

    Health health = indicator.health();

    assertThat(health.getStatus()).isEqualTo(Status.UP);
    assertThat(health.getDetails()).containsEntry("mailConfigured", true);
  }

  @Test
  void healthUpWhenEnvironmentValidationDisabledEvenWithMissingSecrets() {
    RequiredConfigHealthIndicator indicator =
        new RequiredConfigHealthIndicator("short", "tiny", true, "", true, "", "", "", false);

    Health health = indicator.health();

    assertThat(health.getStatus()).isEqualTo(Status.UP);
    assertThat(health.getDetails())
        .containsEntry("validationEnabled", false)
        .containsEntry("checksSkipped", true);
  }
}
