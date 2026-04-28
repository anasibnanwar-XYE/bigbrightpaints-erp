package com.bigbrightpaints.erp.core.validationharness;

import java.util.Arrays;
import java.util.Locale;

public enum ValidationFaultKind {
  SEED_FAILURE,
  PARTIAL_SEED_FAILURE,
  SMTP_FAILURE,
  AUDIT_FAILURE,
  SENTRY_FAILURE,
  DATADOG_FAILURE,
  HEALTH_DEGRADED;

  public String code() {
    return name().toLowerCase(Locale.ROOT).replace('_', '-');
  }

  public static ValidationFaultKind fromCode(String code) {
    String normalized = code == null ? "" : code.trim().toUpperCase(Locale.ROOT).replace('-', '_');
    return Arrays.stream(values())
        .filter(kind -> kind.name().equals(normalized))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Unsupported validation fault: " + code));
  }
}
