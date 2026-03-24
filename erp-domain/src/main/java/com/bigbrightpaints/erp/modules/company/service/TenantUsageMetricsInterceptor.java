package com.bigbrightpaints.erp.modules.company.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import com.bigbrightpaints.erp.core.security.CompanyContextHolder;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class TenantUsageMetricsInterceptor implements HandlerInterceptor {

  private static final Logger log = LoggerFactory.getLogger(TenantUsageMetricsInterceptor.class);

  private final TenantUsageMetricsService tenantUsageMetricsService;

  public TenantUsageMetricsInterceptor(TenantUsageMetricsService tenantUsageMetricsService) {
    this.tenantUsageMetricsService = tenantUsageMetricsService;
  }

  @Override
  public boolean preHandle(
      HttpServletRequest request, HttpServletResponse response, Object handler) {
    String path = request != null ? request.getRequestURI() : null;
    if (!StringUtils.hasText(path) || !path.startsWith("/api/v1/")) {
      return true;
    }
    String companyCode = CompanyContextHolder.getCompanyCode();
    if (!StringUtils.hasText(companyCode)) {
      return true;
    }
    try {
      tenantUsageMetricsService.recordApiCall(companyCode);
    } catch (RuntimeException ex) {
      log.debug("Unable to record tenant usage metrics for company {}", companyCode, ex);
    }
    return true;
  }
}
