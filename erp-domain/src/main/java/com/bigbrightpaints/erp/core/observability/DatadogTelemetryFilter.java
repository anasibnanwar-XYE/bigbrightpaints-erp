package com.bigbrightpaints.erp.core.observability;

import java.io.IOException;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@Order(Ordered.LOWEST_PRECEDENCE - 20)
public class DatadogTelemetryFilter extends OncePerRequestFilter {

  private final DatadogTelemetryService datadogTelemetryService;

  public DatadogTelemetryFilter(DatadogTelemetryService datadogTelemetryService) {
    this.datadogTelemetryService = datadogTelemetryService;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    long startedNanos = System.nanoTime();
    boolean failed = false;
    try {
      filterChain.doFilter(request, response);
    } catch (ServletException | IOException | RuntimeException ex) {
      failed = true;
      throw ex;
    } finally {
      if (isControlPlanePath(request)) {
        recordTelemetrySafely(request, response, System.nanoTime() - startedNanos, failed);
      }
    }
  }

  private void recordTelemetrySafely(
      HttpServletRequest request,
      HttpServletResponse response,
      long durationNanos,
      boolean failed) {
    try {
      int statusCode = failed && response.getStatus() < 400 ? 500 : response.getStatus();
      datadogTelemetryService.recordSuperAdminRequest(request, statusCode, durationNanos, failed);
    } catch (RuntimeException ex) {
      // Observability is degraded-mode by default: local API flows must not fail because telemetry
      // export, meter construction, or provider-safe tagging failed.
    }
  }

  private boolean isControlPlanePath(HttpServletRequest request) {
    String path = request.getRequestURI();
    return path != null && path.startsWith("/api/v1/superadmin");
  }
}
