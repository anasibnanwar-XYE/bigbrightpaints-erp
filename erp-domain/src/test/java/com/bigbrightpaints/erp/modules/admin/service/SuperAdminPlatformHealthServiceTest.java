package com.bigbrightpaints.erp.modules.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.util.Locale;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.availability.ApplicationAvailability;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.core.env.Environment;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import com.bigbrightpaints.erp.core.audittrail.AuditActionEventRetryRepository;
import com.bigbrightpaints.erp.core.config.SentryIssueProperties;
import com.bigbrightpaints.erp.core.observability.DatadogTelemetryService;
import com.bigbrightpaints.erp.modules.admin.dto.SuperAdminPlatformHealthDto;
import com.bigbrightpaints.erp.orchestrator.repository.OutboxEvent;
import com.bigbrightpaints.erp.orchestrator.repository.OutboxEventRepository;

@ExtendWith(MockitoExtension.class)
class SuperAdminPlatformHealthServiceTest {

  @Mock private ApplicationAvailability applicationAvailability;
  @Mock private ObjectProvider<HealthEndpoint> healthEndpointProvider;
  @Mock private HealthEndpoint healthEndpoint;
  @Mock private DataSource dataSource;
  @Mock private Connection connection;
  @Mock private ObjectProvider<ConnectionFactory> rabbitConnectionFactoryProvider;
  @Mock private ObjectProvider<JavaMailSenderImpl> mailSenderProvider;
  @Mock private DatadogTelemetryService datadogTelemetryService;
  @Mock private OutboxEventRepository outboxEventRepository;
  @Mock private AuditActionEventRetryRepository auditActionEventRetryRepository;
  @Mock private Environment environment;

  @Test
  void currentHealthReportsRequiredComponentsAndRedactsDegradedDetails() throws Exception {
    when(applicationAvailability.getReadinessState()).thenReturn(ReadinessState.ACCEPTING_TRAFFIC);
    when(healthEndpointProvider.getIfAvailable()).thenReturn(healthEndpoint);
    when(healthEndpoint.health()).thenReturn(Health.up().build());
    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.isValid(2)).thenReturn(true);
    when(rabbitConnectionFactoryProvider.getIfAvailable()).thenReturn(null);
    JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
    mailSender.setHost("localhost");
    mailSender.setPort(1025);
    when(mailSenderProvider.getIfAvailable()).thenReturn(mailSender);
    SentryIssueProperties sentryIssueProperties = new SentryIssueProperties();
    sentryIssueProperties.setAuthToken("placeholder-sentry-token");
    sentryIssueProperties.setOrg("bbp-test");
    sentryIssueProperties.setProject("erp-test");
    when(environment.getProperty("sentry.dsn")).thenReturn("");
    when(environment.getProperty("erp.backup.status")).thenReturn("");
    when(environment.getProperty("ERP_BACKUP_STATUS")).thenReturn("");
    when(datadogTelemetryService.status())
        .thenReturn(
            new DatadogTelemetryService.DatadogTelemetryStatus(
                "DATADOG",
                "DEGRADED",
                "DEGRADED_NO_API_KEY",
                false,
                false,
                false,
                true,
                java.util.List.of("route", "status_class", "actor_hash", "tenant_hash"),
                java.util.List.of("request_body", "credentials"),
                null,
                "LOCAL_TELEMETRY_REGISTRY_MISSING",
                4L,
                2L));
    when(outboxEventRepository.countByStatusAndDeadLetterFalse(OutboxEvent.Status.PENDING))
        .thenReturn(2L);
    when(outboxEventRepository.countByStatusAndDeadLetterFalse(OutboxEvent.Status.PUBLISHING))
        .thenReturn(1L);
    when(outboxEventRepository.countByStatusAndDeadLetterFalseAndRetryCountGreaterThan(
            OutboxEvent.Status.PENDING, 0))
        .thenReturn(1L);
    when(outboxEventRepository.countByStatusAndDeadLetterTrue(OutboxEvent.Status.FAILED))
        .thenReturn(1L);
    when(outboxEventRepository.countByStatusAndDeadLetterFalse(OutboxEvent.Status.FAILED))
        .thenReturn(0L);
    when(auditActionEventRetryRepository.count()).thenReturn(3L);

    SuperAdminPlatformHealthDto response = service(sentryIssueProperties).currentHealth();

    assertThat(response.overallStatus()).isEqualTo("DEGRADED");
    assertThat(response.components())
        .extracting(SuperAdminPlatformHealthDto.Component::name)
        .containsExactly(
            "appReadiness",
            "database",
            "rabbitMq",
            "queue",
            "email",
            "sentry",
            "datadog",
            "backup",
            "failedJobs");
    assertThat(response.database().status()).isEqualTo("UP");
    assertThat(response.rabbitMq().reasonCode()).isEqualTo("RABBITMQ_NOT_CONFIGURED");
    assertThat(response.datadog().metrics()).containsEntry("apiKeyConfigured", false);
    assertThat(response.failedJobs().metrics())
        .containsEntry("failedOutboxEvents", 1L)
        .containsEntry("auditRetryJobs", 3L);
    assertThat(response.redactionPolicy().degradedDetailsRedacted()).isTrue();

    String json =
        new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .writeValueAsString(response)
            .toLowerCase(Locale.ROOT);
    assertThat(json)
        .doesNotContain(
            "placeholder-sentry-token",
            "password",
            "secret",
            "stacktrace",
            "queuepayload",
            "jobarguments",
            "jdbc:",
            "amqp://",
            "dsn");
  }

  @Test
  void appReadinessDegradesWhenActuatorIsMissingUnknownOutOfServiceOrThrows() throws Exception {
    stubCommonInfrastructure();
    stubHealthyQueueCounts();
    stubHealthyFailedJobCounts();
    when(healthEndpointProvider.getIfAvailable())
        .thenReturn(null, healthEndpoint, healthEndpoint, healthEndpoint);
    when(healthEndpoint.health())
        .thenReturn(Health.unknown().build())
        .thenReturn(Health.status(Status.OUT_OF_SERVICE).build())
        .thenThrow(
            new IllegalStateException(
                "jdbc:postgresql://private stacktrace password token actuator failure"));

    SuperAdminPlatformHealthDto missing = service(new SentryIssueProperties()).currentHealth();
    SuperAdminPlatformHealthDto unknown = service(new SentryIssueProperties()).currentHealth();
    SuperAdminPlatformHealthDto outOfService = service(new SentryIssueProperties()).currentHealth();
    SuperAdminPlatformHealthDto exception = service(new SentryIssueProperties()).currentHealth();

    assertThat(missing.appReadiness().status()).isEqualTo("DEGRADED");
    assertThat(missing.appReadiness().metrics()).containsEntry("actuatorStatus", "UNKNOWN");
    assertThat(unknown.appReadiness().status()).isEqualTo("DEGRADED");
    assertThat(unknown.appReadiness().metrics()).containsEntry("actuatorStatus", "UNKNOWN");
    assertThat(outOfService.appReadiness().status()).isEqualTo("DEGRADED");
    assertThat(outOfService.appReadiness().metrics())
        .containsEntry("actuatorStatus", "OUT_OF_SERVICE");
    assertThat(exception.appReadiness().status()).isEqualTo("DEGRADED");
    assertThat(exception.appReadiness().metrics()).containsEntry("actuatorStatus", "DEGRADED");
  }

  @Test
  void queueProbeExceptionReturnsDegradedRedactedComponent() throws Exception {
    stubCommonInfrastructure();
    stubHealthyActuator();
    when(outboxEventRepository.countByStatusAndDeadLetterFalse(OutboxEvent.Status.PENDING))
        .thenThrow(
            new IllegalStateException(
                "jdbc:postgresql://private stacktrace queuePayload password token"));
    stubHealthyFailedJobCounts();

    SuperAdminPlatformHealthDto response = service(new SentryIssueProperties()).currentHealth();

    assertThat(response.queue().status()).isEqualTo("DEGRADED");
    assertThat(response.queue().reasonCode()).isEqualTo("QUEUE_PROBE_UNAVAILABLE");
    assertThat(response.queue().checkedAt()).isNotNull();
    assertThat(response.queue().traceId()).isNotBlank();
    assertThat(response.queue().metrics()).containsEntry("probeAvailable", false);
    assertThat(response.queue().metrics()).doesNotContainKeys("pendingEvents", "publishingEvents");

    String json =
        new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .writeValueAsString(response)
            .toLowerCase(Locale.ROOT);
    assertThat(json)
        .doesNotContain("jdbc:", "postgresql", "stacktrace", "queuepayload", "password", "secret");
  }

  @Test
  void failedJobsProbeExceptionReturnsDegradedRedactedComponent() throws Exception {
    stubCommonInfrastructure();
    stubHealthyActuator();
    stubHealthyQueueCounts();
    when(outboxEventRepository.countByStatusAndDeadLetterTrue(OutboxEvent.Status.FAILED))
        .thenReturn(1L);
    when(outboxEventRepository.countByStatusAndDeadLetterFalse(OutboxEvent.Status.FAILED))
        .thenReturn(0L);
    when(auditActionEventRetryRepository.count())
        .thenThrow(
            new IllegalStateException(
                "amqp://private stacktrace jobArguments password token audit failure"));

    SuperAdminPlatformHealthDto response = service(new SentryIssueProperties()).currentHealth();

    assertThat(response.failedJobs().status()).isEqualTo("DEGRADED");
    assertThat(response.failedJobs().reasonCode()).isEqualTo("FAILED_JOBS_PROBE_UNAVAILABLE");
    assertThat(response.failedJobs().checkedAt()).isNotNull();
    assertThat(response.failedJobs().traceId()).isNotBlank();
    assertThat(response.failedJobs().metrics()).containsEntry("probeAvailable", false);
    assertThat(response.failedJobs().metrics())
        .doesNotContainKeys("failedOutboxEvents", "auditRetryJobs");

    String json =
        new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .writeValueAsString(response)
            .toLowerCase(Locale.ROOT);
    assertThat(json)
        .doesNotContain(
            "amqp://", "stacktrace", "jobarguments", "password", "secret", "audit failure");
  }

  private void stubCommonInfrastructure() throws Exception {
    when(applicationAvailability.getReadinessState()).thenReturn(ReadinessState.ACCEPTING_TRAFFIC);
    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.isValid(2)).thenReturn(true);
    when(rabbitConnectionFactoryProvider.getIfAvailable()).thenReturn(null);
    JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
    mailSender.setHost("localhost");
    mailSender.setPort(1025);
    when(mailSenderProvider.getIfAvailable()).thenReturn(mailSender);
    when(environment.getProperty("sentry.dsn")).thenReturn("");
    when(environment.getProperty("erp.backup.status")).thenReturn("");
    when(environment.getProperty("ERP_BACKUP_STATUS")).thenReturn("");
    when(datadogTelemetryService.status())
        .thenReturn(
            new DatadogTelemetryService.DatadogTelemetryStatus(
                "DATADOG",
                "DEGRADED",
                "DEGRADED_NO_API_KEY",
                false,
                false,
                false,
                true,
                java.util.List.of("route", "status_class", "actor_hash", "tenant_hash"),
                java.util.List.of("request_body", "credentials"),
                null,
                "LOCAL_TELEMETRY_REGISTRY_MISSING",
                0L,
                0L));
  }

  private void stubHealthyActuator() {
    when(healthEndpointProvider.getIfAvailable()).thenReturn(healthEndpoint);
    when(healthEndpoint.health()).thenReturn(Health.up().build());
  }

  private void stubHealthyQueueCounts() {
    when(outboxEventRepository.countByStatusAndDeadLetterFalse(OutboxEvent.Status.PENDING))
        .thenReturn(0L);
    when(outboxEventRepository.countByStatusAndDeadLetterFalse(OutboxEvent.Status.PUBLISHING))
        .thenReturn(0L);
    when(outboxEventRepository.countByStatusAndDeadLetterFalseAndRetryCountGreaterThan(
            OutboxEvent.Status.PENDING, 0))
        .thenReturn(0L);
  }

  private void stubHealthyFailedJobCounts() {
    when(outboxEventRepository.countByStatusAndDeadLetterTrue(OutboxEvent.Status.FAILED))
        .thenReturn(0L);
    when(outboxEventRepository.countByStatusAndDeadLetterFalse(OutboxEvent.Status.FAILED))
        .thenReturn(0L);
    when(auditActionEventRetryRepository.count()).thenReturn(0L);
  }

  private SuperAdminPlatformHealthService service(SentryIssueProperties sentryIssueProperties) {
    return new SuperAdminPlatformHealthService(
        applicationAvailability,
        healthEndpointProvider,
        dataSource,
        rabbitConnectionFactoryProvider,
        mailSenderProvider,
        sentryIssueProperties,
        datadogTelemetryService,
        outboxEventRepository,
        auditActionEventRetryRepository,
        environment);
  }
}
