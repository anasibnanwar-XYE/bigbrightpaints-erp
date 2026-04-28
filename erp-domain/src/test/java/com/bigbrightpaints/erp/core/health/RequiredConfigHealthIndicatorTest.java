package com.bigbrightpaints.erp.core.health;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

@Tag("critical")
class RequiredConfigHealthIndicatorTest {

  @Test
  void healthUpWhenAllRequiredConfigurationPresent() {
    RequiredConfigHealthIndicator indicator =
        new RequiredConfigHealthIndicator(
            "12345678901234567890123456789012",
            "abcdefghijklmnopqrstuvwxyz123456",
            "audit-signing-key",
            "jdbc:postgresql://db:5432/erp_domain",
            "erp",
            "db-password",
            true,
            "license-key",
            true,
            "smtp-relay.example.com",
            "mailer-user",
            "secret-password",
            true,
            true,
            false);

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
            "audit-signing-key",
            "jdbc:postgresql://db:5432/erp_domain",
            "erp",
            "db-password",
            false,
            "",
            true,
            "smtp-relay.example.com",
            "",
            "secret-password",
            true,
            true,
            false);

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
            "audit-signing-key",
            "jdbc:postgresql://db:5432/erp_domain",
            "erp",
            "db-password",
            false,
            "",
            false,
            "",
            "",
            "",
            false,
            true,
            false);

    Health health = indicator.health();

    assertThat(health.getStatus()).isEqualTo(Status.UP);
    assertThat(health.getDetails()).containsEntry("mailConfigured", true);
  }

  @Test
  void healthDownWhenEnvironmentValidationDisabledWithoutBypassAndSecretsAreMissing() {
    RequiredConfigHealthIndicator indicator =
        new RequiredConfigHealthIndicator(
            "short", "tiny", "", "", "", "", true, "", true, "", "", "", true, false, false);

    Health health = indicator.health();

    assertThat(health.getStatus()).isEqualTo(Status.DOWN);
    assertThat((List<String>) health.getDetails().get("missing"))
        .contains(
            "jwt.secret",
            "erp.security.encryption.key",
            "erp.security.audit.private-key",
            "spring.datasource.url/username/password",
            "erp.licensing.license-key",
            "spring.mail.host/username/password");
  }

  @Test
  void healthUpWhenEnvironmentValidationDisabledWithExplicitBypass() {
    RequiredConfigHealthIndicator indicator =
        new RequiredConfigHealthIndicator(
            "short", "tiny", "", "", "", "", true, "", true, "", "", "", true, false, true);

    Health health = indicator.health();

    assertThat(health.getStatus()).isEqualTo(Status.UP);
    assertThat(health.getDetails())
        .containsEntry("validationEnabled", false)
        .containsEntry("skipWhenValidationDisabled", true)
        .containsEntry("checksSkipped", true);
  }

  @Test
  void healthUpWhenSmtpAuthDisabledAndMailHostPresentWithoutCredentials() {
    RequiredConfigHealthIndicator indicator =
        new RequiredConfigHealthIndicator(
            "12345678901234567890123456789012",
            "abcdefghijklmnopqrstuvwxyz123456",
            "audit-signing-key",
            "jdbc:postgresql://db:5432/erp_domain",
            "erp",
            "db-password",
            false,
            "",
            true,
            "mailhog",
            "",
            "",
            false,
            true,
            false);

    Health health = indicator.health();

    assertThat(health.getStatus()).isEqualTo(Status.UP);
    assertThat(health.getDetails())
        .containsEntry("smtpAuthRequired", false)
        .containsEntry("mailConfigured", true);
  }

  @Test
  void healthDownReportsAuditSigningAndDatasourceByNameOnly() {
    RequiredConfigHealthIndicator indicator =
        new RequiredConfigHealthIndicator(
            "12345678901234567890123456789012",
            "abcdefghijklmnopqrstuvwxyz123456",
            "",
            "",
            "erp",
            "",
            false,
            "",
            true,
            "mailhog",
            "",
            "",
            false,
            true,
            false);

    Health health = indicator.health();

    assertThat(health.getStatus()).isEqualTo(Status.DOWN);
    assertThat(health.getDetails())
        .containsEntry("auditSigningConfigured", false)
        .containsEntry("datasourceConfigured", false);
    assertThat((List<String>) health.getDetails().get("missing"))
        .contains("erp.security.audit.private-key", "spring.datasource.url/username/password");
    assertThat(health.getDetails().values().toString()).doesNotContain("db-password");
  }
}
