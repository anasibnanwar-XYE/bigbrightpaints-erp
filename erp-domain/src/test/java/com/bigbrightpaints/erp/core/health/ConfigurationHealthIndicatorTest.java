package com.bigbrightpaints.erp.core.health;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

@ExtendWith(MockitoExtension.class)
@Tag("critical")
class ConfigurationHealthIndicatorTest {

  @Mock private ConfigurationHealthService configurationHealthService;

  @Test
  void healthUsesConfigurationReportWhenEnvironmentValidationDisabledWithoutBypass() {
    ConfigurationHealthService.ConfigurationHealthReport report =
        new ConfigurationHealthService.ConfigurationHealthReport(
            false,
            List.of(
                new ConfigurationHealthService.ConfigurationIssue(
                    "MOCK", "DEFAULT_ACCOUNTS", "COMPANY_DEFAULTS", "missing defaults")));
    when(configurationHealthService.evaluate()).thenReturn(report);

    ConfigurationHealthIndicator indicator =
        new ConfigurationHealthIndicator(configurationHealthService, 60, false, false);

    Health health = indicator.health();

    assertThat(health.getStatus()).isEqualTo(Status.DOWN);
    assertThat(health.getDetails()).containsEntry("issuesCount", 1);
    verify(configurationHealthService).evaluate();
  }

  @Test
  void healthUsesConfigurationReportWhenEnvironmentValidationEnabled() {
    ConfigurationHealthService.ConfigurationHealthReport report =
        new ConfigurationHealthService.ConfigurationHealthReport(
            false,
            List.of(
                new ConfigurationHealthService.ConfigurationIssue(
                    "MOCK", "DEFAULT_ACCOUNTS", "COMPANY_DEFAULTS", "missing defaults")));
    when(configurationHealthService.evaluate()).thenReturn(report);

    ConfigurationHealthIndicator indicator =
        new ConfigurationHealthIndicator(configurationHealthService, 60, true, false);

    Health health = indicator.health();

    assertThat(health.getStatus()).isEqualTo(Status.DOWN);
    assertThat(health.getDetails()).containsEntry("issuesCount", 1);
    verify(configurationHealthService).evaluate();
  }

  @Test
  void healthUpAndSkipsValidationWhenEnvironmentValidationDisabledWithBypass() {
    ConfigurationHealthIndicator indicator =
        new ConfigurationHealthIndicator(configurationHealthService, 60, false, true);

    Health health = indicator.health();

    assertThat(health.getStatus()).isEqualTo(Status.UP);
    assertThat(health.getDetails())
        .containsEntry("validationEnabled", false)
        .containsEntry("skipWhenValidationDisabled", true)
        .containsEntry("checksSkipped", true);
    verifyNoInteractions(configurationHealthService);
  }
}
