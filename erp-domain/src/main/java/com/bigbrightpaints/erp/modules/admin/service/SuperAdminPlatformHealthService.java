package com.bigbrightpaints.erp.modules.admin.service;

import java.sql.Connection;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.health.HealthComponent;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.availability.ApplicationAvailability;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.core.env.Environment;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.bigbrightpaints.erp.core.audittrail.AuditActionEventRetryRepository;
import com.bigbrightpaints.erp.core.config.SentryIssueProperties;
import com.bigbrightpaints.erp.core.observability.DatadogTelemetryService;
import com.bigbrightpaints.erp.core.util.CompanyTime;
import com.bigbrightpaints.erp.core.web.RequestTraceContext;
import com.bigbrightpaints.erp.modules.admin.dto.SuperAdminPlatformHealthDto;
import com.bigbrightpaints.erp.orchestrator.repository.OutboxEvent;
import com.bigbrightpaints.erp.orchestrator.repository.OutboxEventRepository;

@Service
public class SuperAdminPlatformHealthService {

  private static final List<String> EXPOSED_FIELDS =
      List.of("name", "status", "reasonCode", "checkedAt", "traceId", "metrics");
  private static final List<String> HIDDEN_FIELDS =
      List.of(
          "credentials",
          "urls",
          "tokens",
          "providerConnectionStrings",
          "exceptionDetails",
          "queueMessageBodies",
          "jobInputs",
          "tenantBusinessData");

  private final ApplicationAvailability applicationAvailability;
  private final ObjectProvider<HealthEndpoint> healthEndpointProvider;
  private final DataSource dataSource;
  private final ObjectProvider<ConnectionFactory> rabbitConnectionFactoryProvider;
  private final ObjectProvider<JavaMailSenderImpl> mailSenderProvider;
  private final SentryIssueProperties sentryIssueProperties;
  private final DatadogTelemetryService datadogTelemetryService;
  private final OutboxEventRepository outboxEventRepository;
  private final AuditActionEventRetryRepository auditActionEventRetryRepository;
  private final Environment environment;

  public SuperAdminPlatformHealthService(
      ApplicationAvailability applicationAvailability,
      ObjectProvider<HealthEndpoint> healthEndpointProvider,
      DataSource dataSource,
      ObjectProvider<ConnectionFactory> rabbitConnectionFactoryProvider,
      ObjectProvider<JavaMailSenderImpl> mailSenderProvider,
      SentryIssueProperties sentryIssueProperties,
      DatadogTelemetryService datadogTelemetryService,
      OutboxEventRepository outboxEventRepository,
      AuditActionEventRetryRepository auditActionEventRetryRepository,
      Environment environment) {
    this.applicationAvailability = applicationAvailability;
    this.healthEndpointProvider = healthEndpointProvider;
    this.dataSource = dataSource;
    this.rabbitConnectionFactoryProvider = rabbitConnectionFactoryProvider;
    this.mailSenderProvider = mailSenderProvider;
    this.sentryIssueProperties = sentryIssueProperties;
    this.datadogTelemetryService = datadogTelemetryService;
    this.outboxEventRepository = outboxEventRepository;
    this.auditActionEventRetryRepository = auditActionEventRetryRepository;
    this.environment = environment;
  }

  public SuperAdminPlatformHealthDto currentHealth() {
    Instant checkedAt = CompanyTime.now();
    String traceId = RequestTraceContext.traceId();
    List<SuperAdminPlatformHealthDto.Component> components = new ArrayList<>();
    SuperAdminPlatformHealthDto.Component appReadiness = appReadiness(checkedAt, traceId);
    SuperAdminPlatformHealthDto.Component database = database(checkedAt, traceId);
    SuperAdminPlatformHealthDto.Component rabbitMq = rabbitMq(checkedAt, traceId);
    SuperAdminPlatformHealthDto.Component queue = queue(checkedAt, traceId);
    SuperAdminPlatformHealthDto.Component email = email(checkedAt, traceId);
    SuperAdminPlatformHealthDto.Component sentry = sentry(checkedAt, traceId);
    SuperAdminPlatformHealthDto.Component datadog = datadog(checkedAt, traceId);
    SuperAdminPlatformHealthDto.Component backup = backup(checkedAt, traceId);
    SuperAdminPlatformHealthDto.Component failedJobs = failedJobs(checkedAt, traceId);
    components.addAll(
        List.of(
            appReadiness, database, rabbitMq, queue, email, sentry, datadog, backup, failedJobs));
    return new SuperAdminPlatformHealthDto(
        checkedAt,
        overallStatus(components),
        traceId,
        appReadiness,
        database,
        rabbitMq,
        queue,
        email,
        sentry,
        datadog,
        backup,
        failedJobs,
        List.copyOf(components),
        new SuperAdminPlatformHealthDto.RedactionPolicy(true, EXPOSED_FIELDS, HIDDEN_FIELDS));
  }

  private SuperAdminPlatformHealthDto.Component appReadiness(Instant checkedAt, String traceId) {
    ReadinessState readinessState = applicationAvailability.getReadinessState();
    String readinessStatus = readinessState == ReadinessState.ACCEPTING_TRAFFIC ? "UP" : "DEGRADED";
    HealthEndpoint healthEndpoint = healthEndpointProvider.getIfAvailable();
    String actuatorStatus = "UNKNOWN";
    if (healthEndpoint != null) {
      try {
        HealthComponent health = healthEndpoint.health();
        actuatorStatus = normalizeStatus(health.getStatus());
      } catch (RuntimeException ex) {
        actuatorStatus = "DEGRADED";
      }
    }
    String status =
        "UP".equals(readinessStatus) && !"DOWN".equals(actuatorStatus) ? "UP" : "DEGRADED";
    return component(
        "appReadiness",
        status,
        "UP".equals(status) ? "APP_READY" : "APP_READINESS_DEGRADED",
        checkedAt,
        traceId,
        Map.of("readinessState", readinessState.name(), "actuatorStatus", actuatorStatus));
  }

  private SuperAdminPlatformHealthDto.Component database(Instant checkedAt, String traceId) {
    try (Connection connection = dataSource.getConnection()) {
      boolean valid = connection.isValid(2);
      return component(
          "database",
          valid ? "UP" : "DOWN",
          valid ? "DATABASE_READY" : "DATABASE_VALIDATION_FAILED",
          checkedAt,
          traceId,
          Map.of("reachable", valid));
    } catch (Exception ex) {
      return component(
          "database",
          "DOWN",
          "DATABASE_UNREACHABLE",
          checkedAt,
          traceId,
          Map.of("reachable", false));
    }
  }

  private SuperAdminPlatformHealthDto.Component rabbitMq(Instant checkedAt, String traceId) {
    ConnectionFactory connectionFactory = rabbitConnectionFactoryProvider.getIfAvailable();
    if (connectionFactory == null) {
      return component(
          "rabbitMq",
          "DEGRADED",
          "RABBITMQ_NOT_CONFIGURED",
          checkedAt,
          traceId,
          Map.of("reachable", false));
    }
    try (org.springframework.amqp.rabbit.connection.Connection connection =
        connectionFactory.createConnection()) {
      return component(
          "rabbitMq",
          connection.isOpen() ? "UP" : "DOWN",
          connection.isOpen() ? "RABBITMQ_READY" : "RABBITMQ_CONNECTION_CLOSED",
          checkedAt,
          traceId,
          Map.of("reachable", connection.isOpen()));
    } catch (Exception ex) {
      return component(
          "rabbitMq",
          "DEGRADED",
          "RABBITMQ_UNREACHABLE",
          checkedAt,
          traceId,
          Map.of("reachable", false));
    }
  }

  private SuperAdminPlatformHealthDto.Component queue(Instant checkedAt, String traceId) {
    long pending =
        outboxEventRepository.countByStatusAndDeadLetterFalse(OutboxEvent.Status.PENDING);
    long publishing =
        outboxEventRepository.countByStatusAndDeadLetterFalse(OutboxEvent.Status.PUBLISHING);
    long pendingRetries =
        outboxEventRepository.countByStatusAndDeadLetterFalseAndRetryCountGreaterThan(
            OutboxEvent.Status.PENDING, 0);
    String status = pending + publishing > 1_000 ? "DEGRADED" : "UP";
    return component(
        "queue",
        status,
        "UP".equals(status) ? "QUEUE_READY" : "QUEUE_BACKLOG_HIGH",
        checkedAt,
        traceId,
        Map.of(
            "pendingEvents",
            pending,
            "publishingEvents",
            publishing,
            "pendingRetries",
            pendingRetries));
  }

  private SuperAdminPlatformHealthDto.Component email(Instant checkedAt, String traceId) {
    JavaMailSenderImpl mailSender = mailSenderProvider.getIfAvailable();
    boolean configured =
        mailSender != null && StringUtils.hasText(mailSender.getHost()) && mailSender.getPort() > 0;
    return component(
        "email",
        configured ? "UP" : "DEGRADED",
        configured ? "EMAIL_CONFIGURED" : "EMAIL_NOT_CONFIGURED",
        checkedAt,
        traceId,
        Map.of("configured", configured));
  }

  private SuperAdminPlatformHealthDto.Component sentry(Instant checkedAt, String traceId) {
    boolean dsnConfigured = StringUtils.hasText(environment.getProperty("sentry.dsn"));
    boolean issueLinkConfigured = sentryIssueProperties.isConfigured();
    boolean configured = dsnConfigured || issueLinkConfigured;
    return component(
        "sentry",
        configured ? "UP" : "DEGRADED",
        configured ? "SENTRY_CONFIGURED" : "SENTRY_NOT_CONFIGURED",
        checkedAt,
        traceId,
        Map.of("dsnConfigured", dsnConfigured, "issueLinkConfigured", issueLinkConfigured));
  }

  private SuperAdminPlatformHealthDto.Component datadog(Instant checkedAt, String traceId) {
    DatadogTelemetryService.DatadogTelemetryStatus status = datadogTelemetryService.status();
    boolean ready = !status.degradedMode() && status.apiKeyConfigured();
    return component(
        "datadog",
        ready ? "UP" : "DEGRADED",
        ready ? "DATADOG_READY" : reasonCode(status.status(), "DATADOG_DEGRADED"),
        checkedAt,
        traceId,
        Map.of(
            "apiKeyConfigured",
            status.apiKeyConfigured(),
            "degradedMode",
            status.degradedMode(),
            "recordedRequests",
            status.recordedRequests(),
            "degradedEvents",
            status.degradedEvents()));
  }

  private SuperAdminPlatformHealthDto.Component backup(Instant checkedAt, String traceId) {
    boolean configured =
        StringUtils.hasText(environment.getProperty("erp.backup.status"))
            || StringUtils.hasText(environment.getProperty("ERP_BACKUP_STATUS"));
    return component(
        "backup",
        configured ? "UP" : "DEGRADED",
        configured ? "BACKUP_STATUS_CONFIGURED" : "BACKUP_STATUS_UNCONFIGURED",
        checkedAt,
        traceId,
        Map.of("configured", configured));
  }

  private SuperAdminPlatformHealthDto.Component failedJobs(Instant checkedAt, String traceId) {
    long failedOutbox =
        outboxEventRepository.countByStatusAndDeadLetterTrue(OutboxEvent.Status.FAILED)
            + outboxEventRepository.countByStatusAndDeadLetterFalse(OutboxEvent.Status.FAILED);
    long auditRetries = auditActionEventRetryRepository.count();
    long total = failedOutbox + auditRetries;
    return component(
        "failedJobs",
        total == 0 ? "UP" : "DEGRADED",
        total == 0 ? "NO_FAILED_JOBS" : "FAILED_JOBS_PRESENT",
        checkedAt,
        traceId,
        Map.of("failedOutboxEvents", failedOutbox, "auditRetryJobs", auditRetries));
  }

  private String overallStatus(List<SuperAdminPlatformHealthDto.Component> components) {
    boolean down = components.stream().anyMatch(component -> "DOWN".equals(component.status()));
    if (down) {
      return "DOWN";
    }
    boolean degraded =
        components.stream().anyMatch(component -> "DEGRADED".equals(component.status()));
    return degraded ? "DEGRADED" : "UP";
  }

  private SuperAdminPlatformHealthDto.Component component(
      String name,
      String status,
      String reasonCode,
      Instant checkedAt,
      String traceId,
      Map<String, Object> metrics) {
    return new SuperAdminPlatformHealthDto.Component(
        name, status, reasonCode(reasonCode, "UNKNOWN"), checkedAt, traceId, safeMetrics(metrics));
  }

  private Map<String, Object> safeMetrics(Map<String, Object> metrics) {
    Map<String, Object> safe = new LinkedHashMap<>();
    if (metrics == null) {
      return safe;
    }
    metrics.forEach(
        (key, value) -> {
          if (StringUtils.hasText(key) && !forbiddenKey(key)) {
            safe.put(key, value);
          }
        });
    return safe;
  }

  private boolean forbiddenKey(String key) {
    String lower = key.toLowerCase(Locale.ROOT);
    return lower.contains("password")
        || lower.contains("secret")
        || lower.contains("token")
        || lower.contains("dsn")
        || lower.contains("url")
        || lower.contains("payload")
        || lower.contains("argument")
        || lower.contains("stack")
        || lower.contains("credential");
  }

  private String normalizeStatus(Status status) {
    if (status == null || !StringUtils.hasText(status.getCode())) {
      return "UNKNOWN";
    }
    String code = status.getCode().trim().toUpperCase(Locale.ROOT);
    if ("UP".equals(code) || "DOWN".equals(code) || "OUT_OF_SERVICE".equals(code)) {
      return code;
    }
    return "DEGRADED";
  }

  private String reasonCode(String value, String defaultCode) {
    if (!StringUtils.hasText(value)) {
      return defaultCode;
    }
    return value.trim().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9_]", "_");
  }
}
