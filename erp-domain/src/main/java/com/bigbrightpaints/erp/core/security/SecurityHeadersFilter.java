package com.bigbrightpaints.erp.core.security;

import java.io.IOException;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 30)
public class SecurityHeadersFilter extends OncePerRequestFilter {

  private static final String CACHE_CONTROL_VALUE =
      "no-store, no-cache, max-age=0, must-revalidate";
  private static final String CONTENT_SECURITY_POLICY_VALUE =
      "default-src 'none'; frame-ancestors 'none'; base-uri 'none'";
  private static final String PERMISSIONS_POLICY_VALUE =
      "geolocation=(), camera=(), microphone=(), payment=()";

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    if (isSensitiveApiPath(request)) {
      applySecurityHeaders(response);
    }
    filterChain.doFilter(request, response);
  }

  private boolean isSensitiveApiPath(HttpServletRequest request) {
    String path = resolveApplicationPath(request);
    return StringUtils.hasText(path)
        && (path.startsWith("/api/v1/superadmin") || path.startsWith("/api/v1/auth"));
  }

  private void applySecurityHeaders(HttpServletResponse response) {
    response.setHeader("Cache-Control", CACHE_CONTROL_VALUE);
    response.setHeader("Pragma", "no-cache");
    response.setHeader("Expires", "0");
    response.setHeader("X-Content-Type-Options", "nosniff");
    response.setHeader("X-Frame-Options", "DENY");
    response.setHeader("Referrer-Policy", "no-referrer");
    response.setHeader("Permissions-Policy", PERMISSIONS_POLICY_VALUE);
    response.setHeader("Content-Security-Policy", CONTENT_SECURITY_POLICY_VALUE);
  }

  private String resolveApplicationPath(HttpServletRequest request) {
    if (request == null) {
      return null;
    }
    String servletPath = request.getServletPath();
    String requestUri = request.getRequestURI();
    if (StringUtils.hasText(servletPath)) {
      return servletPath.trim();
    }
    if (StringUtils.hasText(requestUri)) {
      return requestUri.trim();
    }
    return null;
  }
}
