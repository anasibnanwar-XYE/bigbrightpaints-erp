package com.bigbrightpaints.erp.core.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;

class ControlPlaneRateLimitFilterTest {

  private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

  @Test
  void publicAuthEndpointsReturnSafe429AfterConfiguredBurstLimit() throws Exception {
    ControlPlaneRateLimitFilter filter =
        new ControlPlaneRateLimitFilter(objectMapper, null, true, 2, 100);
    AtomicInteger passed = new AtomicInteger();

    MockHttpServletResponse first = invoke(filter, "/api/v1/auth/login", passed);
    MockHttpServletResponse second = invoke(filter, "/api/v1/auth/login", passed);
    MockHttpServletResponse third = invoke(filter, "/api/v1/auth/login", passed);

    assertThat(first.getStatus()).isEqualTo(200);
    assertThat(second.getStatus()).isEqualTo(200);
    assertThat(third.getStatus()).isEqualTo(429);
    assertThat(third.getHeader("Retry-After")).isNotBlank();
    assertThat(third.getContentAsString())
        .contains("\"success\":false")
        .contains("\"code\":\"SYS_006\"")
        .contains("\"reason\":\"RATE_LIMIT_EXCEEDED\"")
        .contains("\"scope\":\"public-auth\"");
    assertThat(passed).hasValue(2);
  }

  @Test
  void superAdminEndpointsUseSeparatePlatformLimit() throws Exception {
    ControlPlaneRateLimitFilter filter =
        new ControlPlaneRateLimitFilter(objectMapper, null, true, 100, 1);
    AtomicInteger passed = new AtomicInteger();

    MockHttpServletResponse first = invoke(filter, "/api/v1/superadmin/dashboard", passed);
    MockHttpServletResponse second = invoke(filter, "/api/v1/superadmin/dashboard", passed);

    assertThat(first.getStatus()).isEqualTo(200);
    assertThat(second.getStatus()).isEqualTo(429);
    assertThat(second.getContentAsString()).contains("\"scope\":\"platform\"");
    assertThat(passed).hasValue(1);
  }

  @Test
  void disabledFilterDoesNotThrottleControlPlaneRequests() throws Exception {
    ControlPlaneRateLimitFilter filter =
        new ControlPlaneRateLimitFilter(objectMapper, null, false, 1, 1);
    AtomicInteger passed = new AtomicInteger();

    invoke(filter, "/api/v1/auth/login", passed);
    MockHttpServletResponse response = invoke(filter, "/api/v1/auth/login", passed);

    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(passed).hasValue(2);
  }

  private MockHttpServletResponse invoke(
      ControlPlaneRateLimitFilter filter, String path, AtomicInteger passed) throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", path);
    request.setRemoteAddr("192.0.2.10");
    MockHttpServletResponse response = new MockHttpServletResponse();
    filter.doFilter(
        request,
        response,
        (servletRequest, servletResponse) -> {
          passed.incrementAndGet();
          ((MockHttpServletResponse) servletResponse).setStatus(200);
        });
    return response;
  }
}
