package com.bigbrightpaints.erp.core.validationharness;

import java.util.regex.Pattern;

import org.springframework.util.StringUtils;

public final class ValidationRunNamespace {

  private static final Pattern SAFE_RUN_MARKER =
      Pattern.compile("^[A-Za-z0-9][A-Za-z0-9._:-]{2,96}$");

  private ValidationRunNamespace() {}

  public static String requireSafeRunMarker(String runMarker) {
    String normalized = runMarker == null ? "" : runMarker.trim();
    if (!StringUtils.hasText(normalized) || !SAFE_RUN_MARKER.matcher(normalized).matches()) {
      throw new IllegalArgumentException(
          "runMarker must be 3-97 safe characters: letters, numbers, dot, underscore, colon, or"
              + " hyphen");
    }
    return normalized;
  }
}
