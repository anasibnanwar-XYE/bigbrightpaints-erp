package com.bigbrightpaints.erp.core.exception;

import java.util.HashMap;
import java.util.Map;

import org.springframework.util.StringUtils;

import com.bigbrightpaints.erp.core.audit.AuditEvent;
import com.bigbrightpaints.erp.core.audit.AuditService;
import com.bigbrightpaints.erp.core.audit.IntegrationFailureAlertRoutingPolicy;
import com.bigbrightpaints.erp.core.audit.IntegrationFailureMetadataSchema;

import jakarta.servlet.http.HttpServletRequest;

public class AuditExceptionRoutingService {

  private final SettlementExceptionHandler settlementExceptionHandler;

  public AuditExceptionRoutingService(SettlementExceptionHandler settlementExceptionHandler) {
    this.settlementExceptionHandler = settlementExceptionHandler;
  }

  public void routeApplicationException(
      AuditService auditService,
      HttpServletRequest request,
      String traceId,
      ApplicationException ex) {
    if (auditService == null || request == null || ex == null) {
      return;
    }
    if (!settlementExceptionHandler.isSettlementRequest(request)) {
      return;
    }
    auditService.logFailure(
        AuditEvent.INTEGRATION_FAILURE,
        settlementExceptionHandler.buildSettlementFailureMetadata(request, traceId, ex));
  }

  public void routeMalformedRequest(
      AuditService auditService,
      HttpServletRequest request,
      String traceId,
      String reason,
      String detail) {
    if (auditService == null) {
      return;
    }
    Map<String, String> metadata = new HashMap<>();
    metadata.put("category", "request-parse");
    metadata.put("code", ErrorCode.VALIDATION_INVALID_INPUT.getCode());
    metadata.put("traceId", traceId);
    metadata.put("reason", reason);
    String failureCode = IntegrationFailureAlertRoutingPolicy.MALFORMED_REQUEST_FAILURE_CODE;
    IntegrationFailureMetadataSchema.applyRequiredFields(
        metadata,
        failureCode,
        "VALIDATION",
        IntegrationFailureAlertRoutingPolicy.ROUTING_VERSION,
        IntegrationFailureAlertRoutingPolicy.resolveRoute(failureCode, "request-parse"));
    if (request != null) {
      metadata.put("requestMethod", request.getMethod());
      metadata.put("requestPath", request.getRequestURI());
    }
    if (StringUtils.hasText(detail)) {
      settlementExceptionHandler.putTrimmedMetadataIfPresent(metadata, "detail", detail);
    }
    auditService.logFailure(AuditEvent.INTEGRATION_FAILURE, metadata);
  }
}
