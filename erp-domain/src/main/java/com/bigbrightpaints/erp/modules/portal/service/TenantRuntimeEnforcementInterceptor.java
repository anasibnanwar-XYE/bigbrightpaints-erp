package com.bigbrightpaints.erp.modules.portal.service;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.bigbrightpaints.erp.core.exception.ApplicationException;
import com.bigbrightpaints.erp.core.exception.ErrorCode;
import com.bigbrightpaints.erp.core.security.TenantRuntimeRequestAttributes;
import com.bigbrightpaints.erp.core.web.RequestTraceContext;
import com.bigbrightpaints.erp.modules.company.domain.Company;
import com.bigbrightpaints.erp.modules.company.service.CompanyContextService;
import com.bigbrightpaints.erp.modules.company.service.TenantRuntimeEnforcementService;
import com.bigbrightpaints.erp.modules.company.service.TenantRuntimeRequestAdmissionService;
import com.bigbrightpaints.erp.shared.dto.ApiResponse;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class TenantRuntimeEnforcementInterceptor implements HandlerInterceptor {

  private final CompanyContextService companyContextService;
  private final TenantRuntimeRequestAdmissionService tenantRuntimeRequestAdmissionService;
  private final ObjectMapper objectMapper;

  public TenantRuntimeEnforcementInterceptor(
      CompanyContextService companyContextService,
      TenantRuntimeRequestAdmissionService tenantRuntimeRequestAdmissionService,
      ObjectMapper objectMapper) {
    this.companyContextService = companyContextService;
    this.tenantRuntimeRequestAdmissionService = tenantRuntimeRequestAdmissionService;
    this.objectMapper = objectMapper;
  }

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
      throws IOException {
    String path = request.getRequestURI();
    if (!isEnforcedPath(path)) {
      return true;
    }
    if (Boolean.TRUE.equals(
        request.getAttribute(TenantRuntimeRequestAttributes.CANONICAL_ADMISSION_APPLIED))) {
      return true;
    }

    Company company = companyContextService.requireCurrentCompany();
    TenantRuntimeEnforcementService.TenantRequestAdmission admission =
        tenantRuntimeRequestAdmissionService.beginRequest(
            company.getCode(), path, request.getMethod(), resolveCurrentActor(), false);
    if (admission == null || !admission.isAdmitted()) {
      if (isQuotaRejection(admission)) {
        writeRuntimeAdmissionDenied(response, admission);
        return false;
      }
      throw admissionException(company.getCode(), path, admission);
    }
    request.setAttribute(TenantRuntimeRequestAttributes.INTERCEPTOR_ADMISSION, admission);
    return true;
  }

  @Override
  public void afterCompletion(
      HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
    Object admission = request.getAttribute(TenantRuntimeRequestAttributes.INTERCEPTOR_ADMISSION);
    if (admission
        instanceof TenantRuntimeEnforcementService.TenantRequestAdmission trackedAdmission) {
      tenantRuntimeRequestAdmissionService.completeRequest(trackedAdmission, response.getStatus());
    }
  }

  private boolean isEnforcedPath(String path) {
    if (!StringUtils.hasText(path)) {
      return false;
    }
    return path.startsWith("/api/v1/reports/") || path.startsWith("/api/v1/portal/");
  }

  private RuntimeException admissionException(
      String companyCode,
      String path,
      TenantRuntimeEnforcementService.TenantRequestAdmission admission) {
    String normalizedCompanyCode = StringUtils.hasText(companyCode) ? companyCode.trim() : null;
    String normalizedPath = StringUtils.hasText(path) ? path.trim() : null;
    if (admission == null) {
      return new ApplicationException(
              ErrorCode.BUSINESS_INVALID_STATE, "Tenant runtime admission is unavailable")
          .withDetail("companyCode", normalizedCompanyCode)
          .withDetail("path", normalizedPath);
    }
    if (!StringUtils.hasText(normalizedCompanyCode)
        && StringUtils.hasText(admission.companyCode())) {
      normalizedCompanyCode = admission.companyCode().trim();
    }
    if (isQuotaRejection(admission)) {
      TenantRuntimeEnforcementService.TenantRuntimeSnapshot snapshot =
          requiresQuotaSnapshot(admission) ? snapshotOrNull(normalizedCompanyCode) : null;
      return new ApplicationException(ErrorCode.BUSINESS_LIMIT_EXCEEDED, admission.message())
          .withDetail("companyCode", normalizedCompanyCode)
          .withDetail("quotaType", admission.limitType())
          .withDetail("quotaValue", parseIntOrZero(admission.limitValue()))
          .withDetail("observed", parseIntOrZero(admission.observedValue()))
          .withDetail("policyReference", policyReference(snapshot, admission))
          .withDetail("path", normalizedPath);
    }
    TenantRuntimeEnforcementService.TenantRuntimeSnapshot snapshot =
        requiresStateSnapshot(admission) ? snapshotOrNull(normalizedCompanyCode) : null;
    return new ApplicationException(ErrorCode.BUSINESS_INVALID_STATE, admission.message())
        .withDetail("companyCode", normalizedCompanyCode)
        .withDetail("holdState", holdState(snapshot, admission))
        .withDetail("holdReason", holdReason(snapshot, admission))
        .withDetail("policyReference", policyReference(snapshot, admission))
        .withDetail("path", normalizedPath);
  }

  private boolean isQuotaRejection(
      TenantRuntimeEnforcementService.TenantRequestAdmission admission) {
    return admission != null
        && StringUtils.hasText(admission.limitType())
        && !"TENANT_STATE".equalsIgnoreCase(admission.limitType().trim());
  }

  private boolean requiresQuotaSnapshot(
      TenantRuntimeEnforcementService.TenantRequestAdmission admission) {
    return !StringUtils.hasText(admission.auditChainId());
  }

  private boolean requiresStateSnapshot(
      TenantRuntimeEnforcementService.TenantRequestAdmission admission) {
    return !StringUtils.hasText(admission.observedValue())
        || !StringUtils.hasText(admission.tenantReasonCode())
        || !StringUtils.hasText(admission.auditChainId());
  }

  private TenantRuntimeEnforcementService.TenantRuntimeSnapshot snapshotOrNull(String companyCode) {
    if (!StringUtils.hasText(companyCode)) {
      return null;
    }
    try {
      return tenantRuntimeRequestAdmissionService.snapshot(companyCode);
    } catch (RuntimeException ex) {
      return null;
    }
  }

  private String holdState(
      TenantRuntimeEnforcementService.TenantRuntimeSnapshot snapshot,
      TenantRuntimeEnforcementService.TenantRequestAdmission admission) {
    if (StringUtils.hasText(admission.observedValue())) {
      return admission.observedValue().trim();
    }
    if (snapshot != null && snapshot.state() != null) {
      return snapshot.state().name();
    }
    return null;
  }

  private String holdReason(
      TenantRuntimeEnforcementService.TenantRuntimeSnapshot snapshot,
      TenantRuntimeEnforcementService.TenantRequestAdmission admission) {
    if (StringUtils.hasText(admission.tenantReasonCode())) {
      return admission.tenantReasonCode().trim();
    }
    if (snapshot != null) {
      return snapshot.reasonCode();
    }
    return null;
  }

  private String policyReference(
      TenantRuntimeEnforcementService.TenantRuntimeSnapshot snapshot,
      TenantRuntimeEnforcementService.TenantRequestAdmission admission) {
    if (StringUtils.hasText(admission.auditChainId())) {
      return admission.auditChainId().trim();
    }
    if (snapshot != null) {
      return snapshot.auditChainId();
    }
    return null;
  }

  private int parseIntOrZero(String value) {
    if (!StringUtils.hasText(value)) {
      return 0;
    }
    try {
      return Integer.parseInt(value.trim());
    } catch (NumberFormatException ex) {
      return 0;
    }
  }

  private void writeRuntimeAdmissionDenied(
      HttpServletResponse response,
      TenantRuntimeEnforcementService.TenantRequestAdmission admission)
      throws IOException {
    String message =
        StringUtils.hasText(admission.message()) ? admission.message().trim() : "Access denied";
    Map<String, Object> data = new LinkedHashMap<>();
    data.put(
        "code",
        StringUtils.hasText(admission.reasonCode())
            ? admission.reasonCode().trim()
            : "TENANT_REQUEST_DENIED");
    data.put("message", message);
    data.put("traceId", RequestTraceContext.traceId());
    if (StringUtils.hasText(admission.reasonCode())) {
      data.put("reason", admission.reasonCode().trim());
    }
    if (StringUtils.hasText(admission.auditChainId())) {
      data.put("auditChainId", admission.auditChainId().trim());
    }
    if (StringUtils.hasText(admission.tenantReasonCode())) {
      data.put("tenantReasonCode", admission.tenantReasonCode().trim());
    }
    if (StringUtils.hasText(admission.limitType())) {
      data.put("limitType", admission.limitType().trim());
    }
    if (StringUtils.hasText(admission.observedValue())) {
      data.put("observedValue", admission.observedValue().trim());
    }
    if (StringUtils.hasText(admission.limitValue())) {
      data.put("limitValue", admission.limitValue().trim());
    }
    if (admission.retryAfterSeconds() != null) {
      data.put("retryAfterSeconds", admission.retryAfterSeconds());
    }
    if (admission.resetAtEpochSecond() != null) {
      data.put("resetAtEpochSecond", admission.resetAtEpochSecond());
    }
    if (admission.statusCode() == 429) {
      writeRateLimitHeaders(response, admission);
    }
    response.setStatus(admission.statusCode());
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");
    objectMapper.writeValue(response.getWriter(), ApiResponse.failure(message, data));
  }

  private void writeRateLimitHeaders(
      HttpServletResponse response,
      TenantRuntimeEnforcementService.TenantRequestAdmission admission) {
    if (admission.retryAfterSeconds() != null) {
      response.setHeader(HttpHeaders.RETRY_AFTER, Long.toString(admission.retryAfterSeconds()));
    }
    if (StringUtils.hasText(admission.limitValue())) {
      response.setHeader("X-RateLimit-Limit", admission.limitValue().trim());
    }
    response.setHeader("X-RateLimit-Remaining", "0");
    if (admission.resetAtEpochSecond() != null) {
      response.setHeader("X-RateLimit-Reset", Long.toString(admission.resetAtEpochSecond()));
    }
  }

  private String resolveCurrentActor() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !StringUtils.hasText(authentication.getName())) {
      return null;
    }
    return authentication.getName().trim();
  }
}
