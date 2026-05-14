package com.bigbrightpaints.erp.core.observability;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerMapping;

import com.bigbrightpaints.erp.core.security.CompanyContextHolder;
import com.bigbrightpaints.erp.core.util.CompanyTime;
import com.bigbrightpaints.erp.core.web.RequestTraceContext;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;
import jakarta.servlet.http.HttpServletRequest;

@Service
public class DatadogTelemetryService {

  private static final String REQUEST_COUNTER = "superadmin.control_plane.requests";
  private static final String REQUEST_TIMER = "superadmin.control_plane.request.duration";
  private static final List<String> SAFE_TAG_KEYS =
      List.of(
          "service",
          "env",
          "release",
          "component",
          "method",
          "route",
          "status_class",
          "outcome",
          "actor_role",
          "actor_hash",
          "tenant_hash");
  private static final List<String> FORBIDDEN_TAG_POLICY =
      List.of(
          "credentials",
          "request_body",
          "response_body",
          "query_string",
          "email",
          "company_name_or_code",
          "support_or_bug_text",
          "raw_ids",
          "private_canaries");

  private final ObjectProvider<MeterRegistry> meterRegistryProvider;
  private final DatadogTelemetryProperties properties;
  private final AtomicLong recordedRequests = new AtomicLong();
  private final AtomicLong degradedEvents = new AtomicLong();
  private volatile DatadogTelemetrySnapshot lastRequest;
  private volatile String lastErrorCode;

  public DatadogTelemetryService(
      ObjectProvider<MeterRegistry> meterRegistryProvider, DatadogTelemetryProperties properties) {
    this.meterRegistryProvider = meterRegistryProvider;
    this.properties = properties;
  }

  public void recordSuperAdminRequest(
      HttpServletRequest request, int statusCode, long durationNanos, boolean failed) {
    if (!properties.isEnabled()) {
      degradedEvents.incrementAndGet();
      lastRequest = snapshot(request, statusCode, failed, "TELEMETRY_DISABLED");
      return;
    }
    try {
      Map<String, String> tags = safeTags(request, statusCode, failed);
      MeterRegistry registry = meterRegistryProvider.getIfAvailable();
      if (registry == null) {
        degradedEvents.incrementAndGet();
        lastErrorCode = "LOCAL_TELEMETRY_REGISTRY_MISSING";
        lastRequest = snapshot(tags, RequestTraceContext.traceId(), lastErrorCode);
        return;
      }
      Iterable<Tag> micrometerTags = toTags(tags);
      Counter.builder(REQUEST_COUNTER).tags(micrometerTags).register(registry).increment();
      Timer.builder(REQUEST_TIMER)
          .tags(micrometerTags)
          .register(registry)
          .record(Math.max(0L, durationNanos), TimeUnit.NANOSECONDS);
      recordedRequests.incrementAndGet();
      lastErrorCode = null;
      lastRequest = snapshot(tags, RequestTraceContext.traceId(), null);
    } catch (RuntimeException ex) {
      degradedEvents.incrementAndGet();
      lastErrorCode = "LOCAL_TELEMETRY_DEGRADED";
      lastRequest = snapshot(request, statusCode, failed, lastErrorCode);
    }
  }

  public DatadogTelemetryStatus status() {
    boolean configured = properties.isApiKeyConfigured();
    String registryError = registryErrorCode();
    String effectiveErrorCode = StringUtils.hasText(lastErrorCode) ? lastErrorCode : registryError;
    boolean degraded =
        !properties.isEnabled() || !configured || StringUtils.hasText(effectiveErrorCode);
    String mode;
    String status;
    if (!properties.isEnabled()) {
      mode = "DISABLED";
      status = "DEGRADED_DISABLED";
    } else if (!configured) {
      mode = "DEGRADED";
      status = "DEGRADED_NO_API_KEY";
    } else if (StringUtils.hasText(effectiveErrorCode)) {
      mode = "DEGRADED";
      status = effectiveErrorCode;
    } else {
      mode = "ENABLED";
      status = "LOCAL_SAFE_TELEMETRY_READY";
    }
    return new DatadogTelemetryStatus(
        "DATADOG",
        mode,
        status,
        configured,
        false,
        false,
        degraded,
        SAFE_TAG_KEYS,
        FORBIDDEN_TAG_POLICY,
        lastRequest,
        effectiveErrorCode,
        recordedRequests.get(),
        degradedEvents.get());
  }

  Map<String, String> safeTags(HttpServletRequest request, int statusCode, boolean failed) {
    Map<String, String> tags = new LinkedHashMap<>();
    tags.put("service", "erp-domain");
    tags.put("env", safe(properties.getEnvironment(), "dev"));
    tags.put("release", safe(properties.getRelease(), "erp-domain@unknown"));
    tags.put("component", "superadmin");
    tags.put("method", safe(request.getMethod(), "UNKNOWN"));
    tags.put("route", safe(resolveRouteTemplate(request), "/api/v1/superadmin/{unmatched}"));
    tags.put("status_class", statusClass(statusCode));
    tags.put("outcome", failed || statusCode >= 500 ? "ERROR" : "SUCCESS");
    tags.put("actor_role", safe(primaryRole(), "ANONYMOUS"));
    tags.put("actor_hash", actorHash());
    tags.put("tenant_hash", tenantHash(request));
    return tags;
  }

  private DatadogTelemetrySnapshot snapshot(
      HttpServletRequest request, int statusCode, boolean failed, String errorCode) {
    return snapshot(
        safeTags(request, statusCode, failed), RequestTraceContext.traceId(), errorCode);
  }

  private DatadogTelemetrySnapshot snapshot(
      Map<String, String> tags, String traceId, String errorCode) {
    return new DatadogTelemetrySnapshot(
        CompanyTime.now(),
        traceId,
        tags.get("method"),
        tags.get("route"),
        tags.get("status_class"),
        tags.get("outcome"),
        tags.get("actor_role"),
        StringUtils.hasText(tags.get("actor_hash")) && !"anonymous".equals(tags.get("actor_hash")),
        StringUtils.hasText(tags.get("tenant_hash")) && !"platform".equals(tags.get("tenant_hash")),
        errorCode);
  }

  private Iterable<Tag> toTags(Map<String, String> safeTags) {
    List<Tag> tags = new ArrayList<>();
    safeTags.entrySet().stream()
        .sorted(Comparator.comparing(Map.Entry::getKey))
        .forEach(entry -> tags.add(Tag.of(entry.getKey(), entry.getValue())));
    return tags;
  }

  private String resolveRouteTemplate(HttpServletRequest request) {
    Object bestPattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
    if (bestPattern != null && StringUtils.hasText(bestPattern.toString())) {
      return bestPattern.toString();
    }
    return scrubPath(request.getRequestURI());
  }

  private String scrubPath(String requestUri) {
    if (!StringUtils.hasText(requestUri)) {
      return "/api/v1/superadmin/{unmatched}";
    }
    if (requestUri.startsWith("/api/v1/superadmin")) {
      return "/api/v1/superadmin/{unmatched}";
    }
    String[] parts = requestUri.split("/");
    List<String> scrubbed = new ArrayList<>();
    for (String part : parts) {
      if (!StringUtils.hasText(part)) {
        continue;
      }
      scrubbed.add(isDynamicOrPrivateSegment(part) ? "{id}" : part);
    }
    return "/" + String.join("/", scrubbed);
  }

  private boolean isDynamicOrPrivateSegment(String part) {
    String lower = part.toLowerCase(java.util.Locale.ROOT);
    return lower.contains("@")
        || lower.length() > 32
        || lower.matches("\\d+")
        || lower.matches("[0-9a-f]{8}-[0-9a-f-]{27,}")
        || TelemetryPrivacySanitizer.containsForbiddenText(lower);
  }

  private String registryErrorCode() {
    if (!properties.isEnabled()) {
      return null;
    }
    try {
      return meterRegistryProvider.getIfAvailable() == null
          ? "LOCAL_TELEMETRY_REGISTRY_MISSING"
          : null;
    } catch (RuntimeException ex) {
      return "LOCAL_TELEMETRY_PROVIDER_UNAVAILABLE";
    }
  }

  private String statusClass(int statusCode) {
    int safeStatus = statusCode <= 0 ? 500 : statusCode;
    return (safeStatus / 100) + "xx";
  }

  private String primaryRole() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !authentication.isAuthenticated()) {
      return "ANONYMOUS";
    }
    return authentication.getAuthorities().stream()
        .map(authority -> authority == null ? null : authority.getAuthority())
        .filter(authority -> authority != null && authority.startsWith("ROLE_"))
        .sorted()
        .findFirst()
        .orElse("AUTHENTICATED");
  }

  private String actorHash() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null
        || !authentication.isAuthenticated()
        || !StringUtils.hasText(authentication.getName())
        || "anonymousUser".equals(authentication.getName())) {
      return "anonymous";
    }
    return TelemetryPrivacySanitizer.pseudonymousHash("actor", authentication.getName());
  }

  private String tenantHash(HttpServletRequest request) {
    String companyCode = CompanyContextHolder.getCompanyCode();
    if (!StringUtils.hasText(companyCode)) {
      companyCode = request.getHeader("X-Company-Code");
    }
    if (!StringUtils.hasText(companyCode)) {
      return "platform";
    }
    return TelemetryPrivacySanitizer.pseudonymousHash("tenant", companyCode);
  }

  private String safe(String value, String defaultValue) {
    return TelemetryPrivacySanitizer.safeTagValue(value, defaultValue);
  }

  public record DatadogTelemetryStatus(
      String provider,
      String mode,
      String status,
      boolean apiKeyConfigured,
      boolean credentialsExposed,
      boolean requiredForCoreFlows,
      boolean degradedMode,
      List<String> safeTagKeys,
      List<String> forbiddenTagPolicy,
      DatadogTelemetrySnapshot lastRequest,
      String lastErrorCode,
      long recordedRequests,
      long degradedEvents) {}

  public record DatadogTelemetrySnapshot(
      Instant recordedAt,
      String traceId,
      String method,
      String route,
      String statusClass,
      String outcome,
      String actorRole,
      boolean actorHashPresent,
      boolean tenantHashPresent,
      String errorCode) {}
}
