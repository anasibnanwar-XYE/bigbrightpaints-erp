package com.bigbrightpaints.erp.core.observability;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.util.StringUtils;

import com.bigbrightpaints.erp.core.exception.ApplicationException;
import com.bigbrightpaints.erp.core.exception.ErrorCode;

public final class TelemetryPrivacySanitizer {

  private static final int HASH_LENGTH = 16;
  private static final Pattern TAG_SAFE_CHARACTERS =
      Pattern.compile("[A-Za-z0-9_./:{}|@=-]{1,128}");
  private static final Set<String> FORBIDDEN_FREE_TEXT_FRAGMENTS =
      Set.of(
          "bearer ",
          "authorization:",
          "cookie:",
          "password=",
          "password:",
          "token=",
          "token:",
          "secret=",
          "secret:",
          "sentry_dsn",
          "sentry_auth_token",
          "dd_api_key",
          "api-key",
          "begin private key",
          "private-canary",
          "secret-canary",
          "tenant-private-canary",
          "invoice-canary",
          "ledger-canary",
          "inventory-canary",
          "salary-canary",
          "vendor-canary",
          "customer-canary",
          "gst-return-canary",
          "canary");

  private TelemetryPrivacySanitizer() {}

  public static void rejectForbiddenFreeText(String field, String value) {
    if (!StringUtils.hasText(value)) {
      return;
    }
    if (containsForbiddenText(value)) {
      throw new ApplicationException(
          ErrorCode.VALIDATION_INVALID_INPUT,
          field + " contains unsupported private or secret text");
    }
  }

  public static String safeTagValue(String rawValue, String fallback) {
    String value = StringUtils.hasText(rawValue) ? rawValue.trim() : fallback;
    if (!StringUtils.hasText(value)) {
      value = "unknown";
    }
    if (containsForbiddenText(value)) {
      return "redacted";
    }
    String bounded = value.length() > 128 ? value.substring(0, 128) : value;
    if (TAG_SAFE_CHARACTERS.matcher(bounded).matches()) {
      return bounded;
    }
    String slug = bounded.replaceAll("[^A-Za-z0-9_./:{}|@=-]", "_");
    return slug.length() > 128 ? slug.substring(0, 128) : slug;
  }

  public static String pseudonymousHash(String namespace, String rawValue) {
    if (!StringUtils.hasText(rawValue)) {
      return "unknown";
    }
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] bytes =
          digest.digest((namespace + ":" + rawValue.trim()).getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(bytes).substring(0, HASH_LENGTH);
    } catch (Exception ex) {
      return "hash_unavailable";
    }
  }

  public static boolean containsForbiddenText(String value) {
    if (!StringUtils.hasText(value)) {
      return false;
    }
    String lower = value.toLowerCase(Locale.ROOT);
    return FORBIDDEN_FREE_TEXT_FRAGMENTS.stream().anyMatch(lower::contains);
  }
}
