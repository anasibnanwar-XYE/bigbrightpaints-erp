package com.bigbrightpaints.erp.core.validationharness;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.bigbrightpaints.erp.core.audit.AuditLog;
import com.bigbrightpaints.erp.core.audit.AuditService;
import com.bigbrightpaints.erp.core.web.RequestTraceContext;

@Service
@Profile("validation-harness")
public class ValidationSecurityAlertTriggerService {

  private static final Pattern SAFE_ALERT_TYPE = Pattern.compile("^[A-Z0-9][A-Z0-9_:-]{2,63}$");
  private static final Pattern SAFE_REASON_CODE =
      Pattern.compile("^[A-Za-z0-9][A-Za-z0-9._:-]{2,96}$");
  private static final String DEFAULT_ALERT_TYPE = "VALIDATION_SECURITY_ALERT";
  private static final String DEFAULT_REASON_CODE = "M14_VALIDATION_TRIGGER";

  private final AuditService auditService;

  public ValidationSecurityAlertTriggerService(AuditService auditService) {
    this.auditService = auditService;
  }

  public TriggerResult trigger(String runMarker, String alertType, String reasonCode) {
    String safeRunMarker = ValidationRunNamespace.requireSafeRunMarker(runMarker);
    String safeAlertType = normalizeAlertType(alertType);
    String safeReasonCode = normalizeReasonCode(reasonCode);
    String traceId = RequestTraceContext.traceId();

    Map<String, String> metadata = new LinkedHashMap<>();
    metadata.put("source", "validation-harness");
    metadata.put("reference", safeRunMarker);
    metadata.put("reason", safeReasonCode);
    metadata.put("validationOnly", "true");
    if (StringUtils.hasText(traceId)) {
      metadata.put("traceId", traceId);
    }

    AuditLog saved =
        auditService.logSecurityAlertNow(
            safeAlertType, "Validation harness security alert trigger", metadata);
    return new TriggerResult(
        true,
        safeRunMarker,
        saved.getId(),
        saved.getEventType().name(),
        saved.getStatus().name(),
        safeAlertType,
        safeReasonCode,
        firstNonBlank(saved.getTraceId(), traceId),
        saved.getTimestamp(),
        "validation-harness");
  }

  private String normalizeAlertType(String alertType) {
    String normalized =
        StringUtils.hasText(alertType)
            ? alertType.trim().toUpperCase(Locale.ROOT)
            : DEFAULT_ALERT_TYPE;
    if (!SAFE_ALERT_TYPE.matcher(normalized).matches()) {
      throw new IllegalArgumentException(
          "alertType must be 3-64 safe uppercase characters: letters, numbers, underscore,"
              + " colon, or hyphen");
    }
    return normalized;
  }

  private String normalizeReasonCode(String reasonCode) {
    String normalized = StringUtils.hasText(reasonCode) ? reasonCode.trim() : DEFAULT_REASON_CODE;
    if (!SAFE_REASON_CODE.matcher(normalized).matches()) {
      throw new IllegalArgumentException(
          "reasonCode must be 3-97 safe characters: letters, numbers, dot, underscore, colon, or"
              + " hyphen");
    }
    String lower = normalized.toLowerCase(Locale.ROOT);
    if (lower.contains("password")
        || lower.contains("token")
        || lower.contains("secret")
        || lower.contains("bearer")
        || lower.contains("private")) {
      throw new IllegalArgumentException("reasonCode must not contain secret-bearing words");
    }
    return normalized;
  }

  private String firstNonBlank(String first, String second) {
    return StringUtils.hasText(first) ? first : second;
  }

  public record TriggerResult(
      boolean validationOnly,
      String runMarker,
      Long eventId,
      String eventType,
      String auditStatus,
      String alertType,
      String reasonCode,
      String traceId,
      LocalDateTime occurredAt,
      String requiredProfile) {}
}
