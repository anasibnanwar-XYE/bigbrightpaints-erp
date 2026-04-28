package com.bigbrightpaints.erp.core.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.servlet.ServletException;

class SecurityHeadersFilterTest {

  private final SecurityHeadersFilter filter = new SecurityHeadersFilter();

  @Test
  void superAdminResponsesReceiveSensitiveSecurityHeaders() throws ServletException, IOException {
    MockHttpServletRequest request =
        new MockHttpServletRequest("GET", "/api/v1/superadmin/dashboard");
    request.setServletPath("/api/v1/superadmin/dashboard");
    request.setRequestURI("/api/v1/superadmin/dashboard");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, new MockFilterChain());

    assertThat(response.getHeader("Cache-Control"))
        .isEqualTo("no-store, no-cache, max-age=0, must-revalidate");
    assertThat(response.getHeader("Pragma")).isEqualTo("no-cache");
    assertThat(response.getHeader("Expires")).isEqualTo("0");
    assertThat(response.getHeader("X-Content-Type-Options")).isEqualTo("nosniff");
    assertThat(response.getHeader("X-Frame-Options")).isEqualTo("DENY");
    assertThat(response.getHeader("Referrer-Policy")).isEqualTo("no-referrer");
    assertThat(response.getHeader("Permissions-Policy"))
        .contains("geolocation=()", "camera=()", "microphone=()", "payment=()");
    assertThat(response.getHeader("Content-Security-Policy"))
        .contains("default-src 'none'", "frame-ancestors 'none'", "base-uri 'none'");
  }

  @Test
  void nonApiAssetsAreNotDecoratedWithApiNoStoreHeaders() throws ServletException, IOException {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/favicon.ico");
    request.setServletPath("/favicon.ico");
    request.setRequestURI("/favicon.ico");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, new MockFilterChain());

    assertThat(response.getHeader("Cache-Control")).isNull();
    assertThat(response.getHeader("Content-Security-Policy")).isNull();
  }
}
