package com.bigbrightpaints.erp.core.security;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.bigbrightpaints.erp.core.audit.AuditEvent;
import com.bigbrightpaints.erp.core.audit.AuditService;
import com.bigbrightpaints.erp.core.exception.ErrorCode;
import com.bigbrightpaints.erp.core.web.RequestTraceContext;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class AuditAwareAccessDeniedHandler implements AccessDeniedHandler {

  public static final String ACCESS_DENIED_AUDIT_REASON = "security-access-denied-handler";
  public static final String ROLE_MUTATION_DENIED_AUDIT_REASON =
      "ROLE_MUTATION_REQUIRES_SUPER_ADMIN";

  private final AuditService auditService;
  private final ObjectMapper objectMapper;
  private final SecurityErrorResponseWriter responseWriter;

  public AuditAwareAccessDeniedHandler(
      AuditService auditService,
      ObjectMapper objectMapper,
      SecurityErrorResponseWriter responseWriter) {
    this.auditService = auditService;
    this.objectMapper = objectMapper;
    this.responseWriter = responseWriter;
  }

  @Override
  public void handle(
      HttpServletRequest request, HttpServletResponse response, AccessDeniedException ex)
      throws IOException {
    String traceId = RequestTraceContext.traceId();
    String userMessage =
        PortalRoleActionMatrix.resolveAccessDeniedMessage(
            org.springframework.security.core.context.SecurityContextHolder.getContext()
                .getAuthentication(),
            request);
    if (!StringUtils.hasText(userMessage)) {
      userMessage = "Access denied";
    }

    if (!AccessDeniedAuditMarker.isCurrentRequestAlreadyAudited(request)) {
      Map<String, String> metadata = new HashMap<>();
      metadata.put("traceId", traceId);
      metadata.put("deniedPath", request.getRequestURI());
      metadata.put("deniedMethod", request.getMethod());
      if (isSuperadminRoleMutationRequest(request)) {
        metadata.put("reason", ROLE_MUTATION_DENIED_AUDIT_REASON);
        RequestBodyCachingFilter.resolveRequestedRole(request, objectMapper)
            .ifPresent(targetRole -> metadata.put("targetRole", targetRole));
      } else {
        metadata.put("reason", ACCESS_DENIED_AUDIT_REASON);
      }
      String actor = SecurityActorResolver.resolveActorWithSystemProcessFallback();
      metadata.put("actor", actor);
      String tenantScope = AccessDeniedAuditMarker.resolveTenantScope(request);
      if (StringUtils.hasText(tenantScope)) {
        metadata.put("tenantScope", tenantScope);
      }
      auditService.logAuthFailure(AuditEvent.ACCESS_DENIED, actor, tenantScope, metadata);
      AccessDeniedAuditMarker.markCurrentRequestAudited();
    }

    responseWriter.write(
        request,
        response,
        HttpStatus.FORBIDDEN,
        ErrorCode.AUTH_INSUFFICIENT_PERMISSIONS,
        userMessage);
  }

  private boolean isSuperadminRoleMutationRequest(HttpServletRequest request) {
    return RequestBodyCachingFilter.isSuperadminRoleMutationRequest(request);
  }
}
