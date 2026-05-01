package com.bigbrightpaints.erp.core.security;

import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.bigbrightpaints.erp.core.audit.AuditService;
import com.bigbrightpaints.erp.core.exception.ErrorCode;
import com.bigbrightpaints.erp.core.idempotency.IdempotencyUtils;
import com.bigbrightpaints.erp.core.web.RequestTraceContext;
import com.bigbrightpaints.erp.shared.dto.ApiResponse;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 30)
public class ControlPlaneRateLimitFilter extends OncePerRequestFilter {

  private static final Set<String> PUBLIC_AUTH_PATHS =
      Set.of(
          "/api/v1/auth/login",
          "/api/v1/auth/refresh-token",
          "/api/v1/auth/password/forgot",
          "/api/v1/auth/password/reset",
          "/api/v1/auth/activation/verify",
          "/api/v1/auth/activation/complete");
  private static final long CLEANUP_INTERVAL_REQUESTS = 256;

  private final ObjectMapper objectMapper;
  private final AuditService auditService;
  private final boolean enabled;
  private final boolean trustedProxyHeadersEnabled;
  private final int publicAuthRequestsPerMinute;
  private final int platformRequestsPerMinute;
  private final ConcurrentHashMap<String, AtomicInteger> counters = new ConcurrentHashMap<>();
  private final AtomicLong requestSequence = new AtomicLong();

  @Autowired
  public ControlPlaneRateLimitFilter(
      ObjectMapper objectMapper,
      @Autowired(required = false) AuditService auditService,
      @Value("${erp.control-plane.rate-limit.enabled:true}") boolean enabled,
      @Value("${erp.control-plane.rate-limit.trusted-proxy-headers-enabled:false}")
          boolean trustedProxyHeadersEnabled,
      @Value("${erp.control-plane.rate-limit.public-auth-requests-per-minute:600}")
          int publicAuthRequestsPerMinute,
      @Value("${erp.control-plane.rate-limit.platform-requests-per-minute:1200}")
          int platformRequestsPerMinute) {
    this.objectMapper = objectMapper;
    this.auditService = auditService;
    this.enabled = enabled;
    this.trustedProxyHeadersEnabled = trustedProxyHeadersEnabled;
    this.publicAuthRequestsPerMinute = publicAuthRequestsPerMinute;
    this.platformRequestsPerMinute = platformRequestsPerMinute;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    RateLimitTarget target = resolveTarget(request);
    if (target == null || allow(target, request)) {
      filterChain.doFilter(request, response);
      return;
    }
    writeRateLimitResponse(target, request, response);
  }

  private RateLimitTarget resolveTarget(HttpServletRequest request) {
    if (!enabled || request == null || "OPTIONS".equalsIgnoreCase(request.getMethod())) {
      return null;
    }
    String path = normalizePath(request.getRequestURI(), request.getContextPath());
    if (!StringUtils.hasText(path)) {
      return null;
    }
    if (PUBLIC_AUTH_PATHS.contains(path)) {
      return new RateLimitTarget("public-auth", path, publicAuthRequestsPerMinute);
    }
    if (path.startsWith("/api/v1/superadmin/") || path.equals("/api/v1/superadmin")) {
      return new RateLimitTarget("platform", "/api/v1/superadmin/**", platformRequestsPerMinute);
    }
    return null;
  }

  private boolean allow(RateLimitTarget target, HttpServletRequest request) {
    if (target.limit() <= 0) {
      return true;
    }
    long currentMinute = currentEpochMinute();
    String clientKey = resolveClientKey(request);
    String counterKey =
        target.family()
            + ':'
            + currentMinute
            + ':'
            + request.getMethod()
            + ':'
            + target.route()
            + ':'
            + clientKey;
    int count =
        counters.computeIfAbsent(counterKey, ignored -> new AtomicInteger()).incrementAndGet();
    if (requestSequence.incrementAndGet() % CLEANUP_INTERVAL_REQUESTS == 0) {
      cleanupOldWindows(currentMinute);
    }
    return count <= target.limit();
  }

  private void writeRateLimitResponse(
      RateLimitTarget target, HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    long resetEpochSecond = (currentEpochMinute() + 1) * 60;
    long retryAfterSeconds = Math.max(1L, resetEpochSecond - Instant.now().getEpochSecond());
    response.setStatus(429);
    response.setHeader(HttpHeaders.RETRY_AFTER, Long.toString(retryAfterSeconds));
    response.setHeader("X-RateLimit-Limit", Integer.toString(target.limit()));
    response.setHeader("X-RateLimit-Remaining", "0");
    response.setHeader("X-RateLimit-Reset", Long.toString(resetEpochSecond));
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding("UTF-8");

    Map<String, Object> data = new LinkedHashMap<>();
    data.put("code", ErrorCode.SYSTEM_RATE_LIMIT_EXCEEDED.getCode());
    data.put("message", "Rate limit exceeded");
    data.put("reason", "RATE_LIMIT_EXCEEDED");
    data.put("traceId", RequestTraceContext.traceId());
    data.put("path", request.getRequestURI());
    data.put("retryAfterSeconds", retryAfterSeconds);
    data.put("resetAtEpochSecond", resetEpochSecond);
    data.put("limit", target.limit());
    data.put("scope", target.family());
    logRateLimitEvent(target, request);
    objectMapper.writeValue(response.getWriter(), ApiResponse.failure("Rate limit exceeded", data));
  }

  private void logRateLimitEvent(RateLimitTarget target, HttpServletRequest request) {
    if (auditService == null) {
      return;
    }
    try {
      Map<String, String> metadata = new LinkedHashMap<>();
      metadata.put("scope", target.family());
      metadata.put("route", target.route());
      metadata.put("method", request.getMethod());
      metadata.put("clientHash", IdempotencyUtils.sha256Hex(resolveClientKey(request), 12));
      metadata.put("traceId", RequestTraceContext.traceId());
      metadata.put("limit", Integer.toString(target.limit()));
      auditService.logSecurityAlert(
          "CONTROL_PLANE_RATE_LIMIT", "Control-plane rate limit exceeded", metadata);
    } catch (RuntimeException ignored) {
      // Rate limiting must fail closed for the request but never expose audit persistence details.
    }
  }

  private void cleanupOldWindows(long currentMinute) {
    String currentToken = ":" + currentMinute + ":";
    counters.keySet().removeIf(key -> !key.contains(currentToken));
  }

  private String resolveClientKey(HttpServletRequest request) {
    if (trustedProxyHeadersEnabled) {
      String forwardedClient = firstForwardedForClient(request.getHeader("X-Forwarded-For"));
      if (StringUtils.hasText(forwardedClient)) {
        return forwardedClient;
      }
      String realIp = request.getHeader("X-Real-IP");
      if (StringUtils.hasText(realIp)) {
        return realIp.trim();
      }
    }
    String remoteAddress = request.getRemoteAddr();
    return StringUtils.hasText(remoteAddress) ? remoteAddress.trim() : "unknown-client";
  }

  private String firstForwardedForClient(String forwardedFor) {
    if (!StringUtils.hasText(forwardedFor)) {
      return null;
    }
    String firstHop = forwardedFor.split(",", 2)[0].trim();
    return StringUtils.hasText(firstHop) && !"unknown".equalsIgnoreCase(firstHop) ? firstHop : null;
  }

  private String normalizePath(String requestUri, String contextPath) {
    if (!StringUtils.hasText(requestUri)) {
      return null;
    }
    String path = requestUri.trim();
    if (StringUtils.hasText(contextPath) && path.startsWith(contextPath)) {
      path = path.substring(contextPath.length());
    }
    if (!path.startsWith("/")) {
      path = "/" + path;
    }
    while (path.length() > 1 && path.endsWith("/")) {
      path = path.substring(0, path.length() - 1);
    }
    return path;
  }

  private long currentEpochMinute() {
    return Instant.now().getEpochSecond() / 60;
  }

  private record RateLimitTarget(String family, String route, int limit) {}
}
