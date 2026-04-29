package com.bigbrightpaints.erp.core.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.HandlerMapping;

import com.bigbrightpaints.erp.core.security.CompanyContextHolder;
import com.bigbrightpaints.erp.core.web.RequestTraceContext;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

class DatadogTelemetryServiceTest {

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
    CompanyContextHolder.clear();
    RequestTraceContext.clear();
  }

  @Test
  void safeTagsUseRouteTemplateAndPseudonymousIdentifiersWithoutRawPayloadText() {
    DatadogTelemetryService service = service(new SimpleMeterRegistry(), properties("set"));
    MockHttpServletRequest request =
        new MockHttpServletRequest("GET", "/api/v1/superadmin/support/tickets/42");
    request.setAttribute(
        HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE,
        "/api/v1/superadmin/support/tickets/{ticketId}");
    request.addHeader("X-Company-Code", "TENANT-RAW-CODE");
    SecurityContextHolder.getContext()
        .setAuthentication(
            new TestingAuthenticationToken(
                "operator@example.test",
                "placeholder",
                java.util.List.of(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"))));

    Map<String, String> tags = service.safeTags(request, 200, false);

    assertThat(tags)
        .containsEntry("route", "/api/v1/superadmin/support/tickets/{ticketId}")
        .containsEntry("status_class", "2xx")
        .containsEntry("outcome", "SUCCESS")
        .containsEntry("actor_role", "ROLE_SUPER_ADMIN");
    assertThat(tags.get("actor_hash")).hasSize(16).doesNotContain("operator", "@");
    assertThat(tags.get("tenant_hash")).hasSize(16).doesNotContain("TENANT-RAW-CODE");
    assertThat(tags.toString().toLowerCase())
        .doesNotContain(
            "operator@example.test",
            "tenant-raw-code",
            "message body",
            "bug description",
            "private-canary",
            "dd_api_key");
  }

  @Test
  void statusIsCredentialSafeAndCoreFlowsAreNotRequiredToFailClosedWhenDatadogIsMissing() {
    DatadogTelemetryService service = service(new SimpleMeterRegistry(), properties(""));

    DatadogTelemetryService.DatadogTelemetryStatus status = service.status();

    assertThat(status.provider()).isEqualTo("DATADOG");
    assertThat(status.mode()).isEqualTo("DEGRADED");
    assertThat(status.apiKeyConfigured()).isFalse();
    assertThat(status.credentialsExposed()).isFalse();
    assertThat(status.requiredForCoreFlows()).isFalse();
    assertThat(status.safeTagKeys())
        .contains("route", "status_class", "actor_role", "actor_hash", "tenant_hash")
        .doesNotContain("request_body", "query_string", "email");
    assertThat(status.toString()).doesNotContain("placeholder", "api-key", "token");
  }

  @Test
  void localMetricFailureIsDegradedAndDoesNotEscapeRecorder() {
    MeterRegistry brokenRegistry = Mockito.mock(MeterRegistry.class);
    DatadogTelemetryService service = service(brokenRegistry, properties("set"));
    MockHttpServletRequest request =
        new MockHttpServletRequest("POST", "/api/v1/superadmin/tenants/99");
    request.setAttribute(
        HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/api/v1/superadmin/tenants/{id}");

    service.recordSuperAdminRequest(request, 201, 1_000_000L, false);

    DatadogTelemetryService.DatadogTelemetryStatus status = service.status();
    assertThat(status.degradedMode()).isTrue();
    assertThat(status.lastErrorCode()).isEqualTo("LOCAL_TELEMETRY_DEGRADED");
    assertThat(status.lastRequest().route()).isEqualTo("/api/v1/superadmin/tenants/{id}");
  }

  @SuppressWarnings("unchecked")
  private DatadogTelemetryService service(
      MeterRegistry registry, DatadogTelemetryProperties properties) {
    ObjectProvider<MeterRegistry> provider = Mockito.mock(ObjectProvider.class);
    when(provider.getIfAvailable()).thenReturn(registry);
    return new DatadogTelemetryService(provider, properties);
  }

  private DatadogTelemetryProperties properties(String apiKey) {
    DatadogTelemetryProperties properties = new DatadogTelemetryProperties();
    properties.setApiKey(apiKey);
    properties.setEnvironment("test");
    properties.setRelease("erp-domain@test");
    return properties;
  }
}
