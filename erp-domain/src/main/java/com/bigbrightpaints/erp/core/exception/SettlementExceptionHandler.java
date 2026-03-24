package com.bigbrightpaints.erp.core.exception;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.util.StringUtils;

import com.bigbrightpaints.erp.core.audit.IntegrationFailureAlertRoutingPolicy;
import com.bigbrightpaints.erp.core.audit.IntegrationFailureMetadataSchema;

import jakarta.servlet.http.HttpServletRequest;

public class SettlementExceptionHandler {

  public static final Set<String> SETTLEMENT_FAILURE_DETAIL_ALLOWLIST =
      Set.of(
          IntegrationFailureMetadataSchema.KEY_IDEMPOTENCY_KEY,
          IntegrationFailureMetadataSchema.KEY_PARTNER_TYPE,
          IntegrationFailureMetadataSchema.KEY_PARTNER_ID,
          IntegrationFailureMetadataSchema.KEY_INVOICE_ID,
          IntegrationFailureMetadataSchema.KEY_PURCHASE_ID,
          IntegrationFailureMetadataSchema.KEY_OUTSTANDING_AMOUNT,
          IntegrationFailureMetadataSchema.KEY_APPLIED_AMOUNT,
          IntegrationFailureMetadataSchema.KEY_ALLOCATION_COUNT);

  public boolean isSettlementRequest(HttpServletRequest request) {
    if (request == null) {
      return false;
    }
    String requestPath = request.getRequestURI();
    return StringUtils.hasText(requestPath)
        && requestPath.startsWith("/api/v1/accounting/settlements/");
  }

  public Map<String, String> buildSettlementFailureMetadata(
      HttpServletRequest request, String traceId, ApplicationException ex) {
    String requestPath = request.getRequestURI();
    String failureCode = IntegrationFailureAlertRoutingPolicy.SETTLEMENT_OPERATION_FAILURE_CODE;
    String errorCategory = classifyIntegrationFailureCategory(ex.getErrorCode());

    Map<String, String> metadata = new HashMap<>();
    metadata.put("category", "settlement-failure");
    IntegrationFailureMetadataSchema.applyRequiredFields(
        metadata,
        failureCode,
        errorCategory,
        IntegrationFailureAlertRoutingPolicy.ROUTING_VERSION,
        IntegrationFailureAlertRoutingPolicy.resolveRoute(failureCode, errorCategory));
    metadata.put(
        "errorCode",
        ex.getErrorCode() != null
            ? ex.getErrorCode().getCode()
            : ErrorCode.UNKNOWN_ERROR.getCode());
    metadata.put("traceId", traceId);
    metadata.put("requestMethod", request.getMethod());
    metadata.put("requestPath", requestPath);
    metadata.put("settlementType", resolveSettlementType(requestPath));
    appendSettlementFailureDetails(metadata, ex);
    return metadata;
  }

  void appendSettlementFailureDetails(Map<String, String> metadata, ApplicationException ex) {
    if (metadata == null || ex == null || ex.getDetails() == null || ex.getDetails().isEmpty()) {
      return;
    }
    for (String key : SETTLEMENT_FAILURE_DETAIL_ALLOWLIST) {
      putTrimmedMetadataIfPresent(metadata, key, ex.getDetails().get(key));
    }
  }

  String resolveSettlementType(String requestPath) {
    if (!StringUtils.hasText(requestPath)) {
      return "UNKNOWN";
    }
    if (requestPath.contains("/dealers")) {
      return "DEALER";
    }
    if (requestPath.contains("/suppliers")) {
      return "SUPPLIER";
    }
    return "UNKNOWN";
  }

  String classifyIntegrationFailureCategory(ErrorCode errorCode) {
    if (errorCode == null) {
      return "UNKNOWN";
    }
    String code = errorCode.getCode();
    if (!StringUtils.hasText(code)) {
      return "UNKNOWN";
    }
    if (code.startsWith("VAL_")) {
      return "VALIDATION";
    }
    if (code.startsWith("CONC_")) {
      return "CONCURRENCY";
    }
    if (code.startsWith("DATA_")) {
      return "DATA_INTEGRITY";
    }
    if (code.startsWith("SYS_") || code.startsWith("INT_")) {
      return "SYSTEM";
    }
    if (code.startsWith("BUS_")) {
      return "BUSINESS";
    }
    return "UNKNOWN";
  }

  void putTrimmedMetadataIfPresent(Map<String, String> metadata, String key, Object value) {
    if (metadata == null || value == null) {
      return;
    }
    String normalized = trimMetadata(String.valueOf(value));
    if (StringUtils.hasText(normalized)) {
      metadata.put(key, normalized);
    }
  }

  String trimMetadata(String value) {
    if (value == null) {
      return "";
    }
    String sanitized = sanitizeMetadataValue(value);
    if (sanitized.length() <= 500) {
      return sanitized;
    }
    return sanitized.substring(0, 500);
  }

  String sanitizeMetadataValue(String value) {
    StringBuilder normalized = new StringBuilder(value.length());
    boolean previousWhitespace = false;
    for (int index = 0; index < value.length(); index++) {
      char current = value.charAt(index);
      boolean isControlCharacter = Character.isISOControl(current);
      boolean isWhitespaceCharacter = Character.isWhitespace(current);
      if (isControlCharacter || isWhitespaceCharacter) {
        if (!previousWhitespace) {
          normalized.append(' ');
          previousWhitespace = true;
        }
        continue;
      }
      normalized.append(current);
      previousWhitespace = false;
    }
    return normalized.toString().trim();
  }
}
