package com.bigbrightpaints.erp.core.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

class ControlPlaneRateLimitFilterTest {

  private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

  @Test
  void publicAuthEndpointsReturnSafe429AfterConfiguredBurstLimit() throws Exception {
    ControlPlaneRateLimitFilter filter =
        new ControlPlaneRateLimitFilter(objectMapper, null, true, false, 2, 100);
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
        new ControlPlaneRateLimitFilter(objectMapper, null, true, false, 100, 1);
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
        new ControlPlaneRateLimitFilter(objectMapper, null, false, false, 1, 1);
    AtomicInteger passed = new AtomicInteger();

    invoke(filter, "/api/v1/auth/login", passed);
    MockHttpServletResponse response = invoke(filter, "/api/v1/auth/login", passed);

    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(passed).hasValue(2);
  }

  @Test
  void trustedProxyHeadersDisabledKeysByRemoteAddress() throws Exception {
    ControlPlaneRateLimitFilter filter =
        new ControlPlaneRateLimitFilter(objectMapper, null, true, false, 1, 100);
    AtomicInteger passed = new AtomicInteger();

    MockHttpServletResponse first =
        invoke(
            filter,
            "/api/v1/auth/login",
            passed,
            request -> request.addHeader("X-Forwarded-For", "198.51.100.1"));
    MockHttpServletResponse second =
        invoke(
            filter,
            "/api/v1/auth/login",
            passed,
            request -> request.addHeader("X-Forwarded-For", "198.51.100.2"));

    assertThat(first.getStatus()).isEqualTo(200);
    assertThat(second.getStatus()).isEqualTo(429);
    assertThat(passed).hasValue(1);
  }

  @Test
  void trustedProxyHeadersEnabledKeysByFirstForwardedForClient() throws Exception {
    ControlPlaneRateLimitFilter filter =
        new ControlPlaneRateLimitFilter(objectMapper, null, true, true, 1, 100);
    AtomicInteger passed = new AtomicInteger();

    MockHttpServletResponse first =
        invoke(
            filter,
            "/api/v1/auth/login",
            passed,
            request -> request.addHeader("X-Forwarded-For", "198.51.100.1, 10.0.0.9"));
    MockHttpServletResponse second =
        invoke(
            filter,
            "/api/v1/auth/login",
            passed,
            request -> request.addHeader("X-Forwarded-For", "198.51.100.2, 10.0.0.9"));

    assertThat(first.getStatus()).isEqualTo(200);
    assertThat(second.getStatus()).isEqualTo(200);
    assertThat(passed).hasValue(2);
  }

  @Test
  void trustedProxyHeadersEnabledFallsBackToRealIpThenRemoteAddress() throws Exception {
    ControlPlaneRateLimitFilter filter =
        new ControlPlaneRateLimitFilter(objectMapper, null, true, true, 1, 100);
    AtomicInteger passed = new AtomicInteger();

    MockHttpServletResponse first =
        invoke(
            filter,
            "/api/v1/auth/login",
            passed,
            request -> {
              request.addHeader("X-Forwarded-For", "unknown");
              request.addHeader("X-Real-IP", "198.51.100.3");
            });
    MockHttpServletResponse second =
        invoke(
            filter,
            "/api/v1/auth/login",
            passed,
            request -> request.addHeader("X-Forwarded-For", "unknown"));
    MockHttpServletResponse third =
        invoke(
            filter,
            "/api/v1/auth/login",
            passed,
            request -> request.addHeader("X-Forwarded-For", "unknown"));

    assertThat(first.getStatus()).isEqualTo(200);
    assertThat(second.getStatus()).isEqualTo(200);
    assertThat(third.getStatus()).isEqualTo(429);
    assertThat(passed).hasValue(2);
  }

  @Test
  void cleanupOldWindowsParsesWindowMinuteInsteadOfMatchingClientText() {
    ControlPlaneRateLimitFilter filter =
        new ControlPlaneRateLimitFilter(objectMapper, null, true, true, 1, 100);
    long currentMinute = Instant.now().getEpochSecond() / 60;
    long oldMinute = currentMinute - 1;
    String oldKeyWithCurrentMinuteInClient =
        "public-auth:" + oldMinute + ":POST:/api/v1/auth/login:2001:db8:" + currentMinute + ":1";
    String currentKey = "public-auth:" + currentMinute + ":POST:/api/v1/auth/login:2001:db8::2";
    @SuppressWarnings("unchecked")
    ConcurrentHashMap<String, AtomicInteger> counters =
        (ConcurrentHashMap<String, AtomicInteger>) ReflectionTestUtils.getField(filter, "counters");
    counters.put(oldKeyWithCurrentMinuteInClient, new AtomicInteger(1));
    counters.put(currentKey, new AtomicInteger(1));

    ReflectionTestUtils.invokeMethod(filter, "cleanupOldWindows", currentMinute);

    assertThat(counters).containsOnlyKeys(currentKey);
  }

  private MockHttpServletResponse invoke(
      ControlPlaneRateLimitFilter filter, String path, AtomicInteger passed) throws Exception {
    return invoke(filter, path, passed, request -> {});
  }

  private MockHttpServletResponse invoke(
      ControlPlaneRateLimitFilter filter,
      String path,
      AtomicInteger passed,
      Consumer<MockHttpServletRequest> customizeRequest)
      throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", path);
    request.setRemoteAddr("192.0.2.10");
    customizeRequest.accept(request);
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
