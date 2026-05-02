package com.bigbrightpaints.erp.modules.admin.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.util.HtmlUtils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.bigbrightpaints.erp.core.exception.ApplicationException;
import com.bigbrightpaints.erp.core.exception.ErrorCode;
import com.bigbrightpaints.erp.core.observability.TelemetryPrivacySanitizer;
import com.bigbrightpaints.erp.modules.admin.domain.SupportTicket;
import com.bigbrightpaints.erp.modules.admin.domain.SupportTicketCategory;
import com.bigbrightpaints.erp.modules.admin.dto.SupportTicketCreateRequest;
import com.bigbrightpaints.erp.modules.admin.dto.SupportTicketResponse;

@Component
public class BugReportMetadataSanitizer {

  private static final int MAX_METADATA_ENTRIES = 10;
  private static final int MAX_METADATA_VALUE_LENGTH = 128;
  private static final Pattern SAFE_TRACE_ID = Pattern.compile("[A-Za-z0-9._:-]{1,128}");
  private static final Pattern SAFE_ROUTE_TEMPLATE = Pattern.compile("/[A-Za-z0-9_{}./:-]{0,120}");
  private static final Pattern SAFE_ENVIRONMENT = Pattern.compile("[a-z0-9_.:-]{1,64}");
  private static final Pattern SAFE_RELEASE = Pattern.compile("[a-z0-9_.:@-]{1,128}");
  private static final Pattern SAFE_METADATA_TOKEN = Pattern.compile("[a-z0-9_.:-]{1,128}");
  private static final Pattern EMAIL_PATTERN =
      Pattern.compile("(?i)\\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}\\b");
  private static final Pattern URL_PATTERN = Pattern.compile("(?i)\\b[a-z][a-z0-9+.-]*://");
  private static final Pattern UUID_PATTERN =
      Pattern.compile("(?i)\\b[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\b");
  private static final Pattern LONG_NUMERIC_ID_PATTERN = Pattern.compile("\\b\\d{6,}\\b");
  private static final Pattern TENANT_CODE_PATTERN =
      Pattern.compile("\\b[A-Z]{2,}(?:-[A-Z0-9]{2,})+\\b|\\b[A-Z]{4,}\\d{0,4}\\b");
  private static final Set<String> ALLOWED_KEYS =
      Set.of("route", "status", "outcome", "component", "browser", "os", "viewport");
  private static final Set<String> FORBIDDEN_KEY_FRAGMENTS =
      Set.of(
          "body",
          "payload",
          "request",
          "response",
          "storage",
          "stack",
          "token",
          "password",
          "secret",
          "email",
          "authorization",
          "cookie",
          "dsn",
          "url",
          "query",
          "company",
          "tenant");
  private static final Set<String> FORBIDDEN_VALUE_FRAGMENTS =
      Set.of(
          "bearer ",
          "password",
          "secret",
          "token=",
          "authorization",
          "cookie:",
          "sentry_dsn",
          "dd_api_key",
          "invoice",
          "ledger",
          "salary",
          "vendor",
          "customer",
          "tenant-",
          "company-",
          "tenant ",
          "company ",
          "acme",
          "bigbright",
          "gst-return",
          "begin private key",
          "eyj");

  private final ObjectMapper objectMapper;

  public BugReportMetadataSanitizer(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public void applyBugReportFields(SupportTicket ticket, SupportTicketCreateRequest request) {
    if (ticket.getCategory() != SupportTicketCategory.BUG) {
      rejectNonBugMetadata(request);
      return;
    }
    ticket.setBugReproductionSteps(
        safeText(request.reproductionSteps(), "reproductionSteps", 2000));
    ticket.setBugEnvironment(
        safeToken(request.environment(), "environment", 64, false, SAFE_ENVIRONMENT));
    ticket.setBugRelease(safeToken(request.release(), "release", 128, false, SAFE_RELEASE));
    ticket.setBugTraceId(safeTraceId(request.traceId()));
    Map<String, String> metadata = sanitizeMetadata(request.metadata());
    ticket.setBugMetadataJson(metadata.isEmpty() ? null : writeMetadata(metadata));
  }

  public SupportTicketResponse.BugReport bugReport(SupportTicket ticket) {
    return bugReport(ticket, true);
  }

  public SupportTicketResponse.BugReport tenantBugReport(SupportTicket ticket) {
    return bugReport(ticket, false);
  }

  private SupportTicketResponse.BugReport bugReport(
      SupportTicket ticket, boolean includeSafeSentryMetadata) {
    if (ticket.getCategory() != SupportTicketCategory.BUG) {
      return null;
    }
    Map<String, String> metadata = currentStoredMetadata(readMetadata(ticket.getBugMetadataJson()));
    return new SupportTicketResponse.BugReport(
        currentStoredText(ticket.getBugReproductionSteps()),
        currentStoredToken(ticket.getBugEnvironment(), SAFE_ENVIRONMENT),
        currentStoredToken(ticket.getBugRelease(), SAFE_RELEASE),
        currentStoredToken(ticket.getBugTraceId(), SAFE_TRACE_ID),
        metadata,
        includeSafeSentryMetadata ? safeSentryMetadata(ticket) : null);
  }

  public Map<String, String> safeSentryMetadata(SupportTicket ticket) {
    Map<String, String> tags = new LinkedHashMap<>();
    putIfSafe(tags, "environment", ticket.getBugEnvironment(), SAFE_ENVIRONMENT);
    putIfSafe(tags, "release", ticket.getBugRelease(), SAFE_RELEASE);
    putIfSafe(tags, "traceId", ticket.getBugTraceId(), SAFE_TRACE_ID);
    Map<String, String> metadata = readMetadata(ticket.getBugMetadataJson());
    copyAllowed(tags, metadata, "route");
    copyAllowed(tags, metadata, "status");
    copyAllowed(tags, metadata, "outcome");
    copyAllowed(tags, metadata, "component");
    if (ticket.getCompany() != null && StringUtils.hasText(ticket.getCompany().getCode())) {
      tags.put("tenantHash", hash("tenant:" + ticket.getCompany().getCode()));
    }
    tags.put("actorRole", "TENANT_USER");
    if (ticket.getUserId() != null) {
      tags.put("actorHash", hash("actor:" + ticket.getUserId()));
    }
    return tags;
  }

  private void rejectNonBugMetadata(SupportTicketCreateRequest request) {
    if (StringUtils.hasText(request.reproductionSteps())
        || StringUtils.hasText(request.environment())
        || StringUtils.hasText(request.release())
        || StringUtils.hasText(request.traceId())
        || (request.metadata() != null && !request.metadata().isEmpty())) {
      throw new ApplicationException(
          ErrorCode.VALIDATION_INVALID_INPUT, "Bug metadata is only allowed for BUG tickets");
    }
  }

  private Map<String, String> sanitizeMetadata(Map<String, String> rawMetadata) {
    if (rawMetadata == null || rawMetadata.isEmpty()) {
      return Map.of();
    }
    if (rawMetadata.size() > MAX_METADATA_ENTRIES) {
      throw new ApplicationException(
          ErrorCode.VALIDATION_OUT_OF_RANGE,
          "Bug metadata supports at most " + MAX_METADATA_ENTRIES + " keys");
    }
    Map<String, String> sanitized = new LinkedHashMap<>();
    for (Map.Entry<String, String> entry : rawMetadata.entrySet()) {
      String key = normalizeKey(entry.getKey());
      if (!ALLOWED_KEYS.contains(key)) {
        throw new ApplicationException(
            ErrorCode.VALIDATION_INVALID_INPUT, "Unsupported bug metadata key: " + key);
      }
      String value =
          safeToken(
              entry.getValue(),
              "metadata." + key,
              MAX_METADATA_VALUE_LENGTH,
              true,
              "route".equals(key) ? SAFE_ROUTE_TEMPLATE : SAFE_METADATA_TOKEN);
      if ("route".equals(key) && !SAFE_ROUTE_TEMPLATE.matcher(value).matches()) {
        throw new ApplicationException(
            ErrorCode.VALIDATION_INVALID_INPUT,
            "metadata.route must be a route template without query strings or full URLs");
      }
      if (!"route".equals(key)
          && (value.contains("{") || value.contains("[") || value.contains("\n"))) {
        throw new ApplicationException(
            ErrorCode.VALIDATION_INVALID_INPUT,
            "metadata." + key + " must be bounded metadata, not payload text");
      }
      sanitized.put(key, value);
    }
    return sanitized;
  }

  private String normalizeKey(String rawKey) {
    if (!StringUtils.hasText(rawKey)) {
      throw new ApplicationException(
          ErrorCode.VALIDATION_MISSING_REQUIRED_FIELD, "metadata key is required");
    }
    String key = rawKey.trim();
    String lower = key.toLowerCase(Locale.ROOT);
    boolean forbidden = FORBIDDEN_KEY_FRAGMENTS.stream().anyMatch(lower::contains);
    if (forbidden || key.length() > 40) {
      throw new ApplicationException(
          ErrorCode.VALIDATION_INVALID_INPUT, "Unsupported bug metadata key: " + key);
    }
    return lower;
  }

  private String safeText(String rawValue, String field, int maxLength) {
    if (!StringUtils.hasText(rawValue)) {
      return null;
    }
    String value = rawValue.trim();
    if (value.length() > maxLength) {
      throw new ApplicationException(
          ErrorCode.VALIDATION_OUT_OF_RANGE, field + " exceeds max length " + maxLength);
    }
    rejectForbiddenValue(field, value);
    return HtmlUtils.htmlEscape(value);
  }

  private String safeToken(
      String rawValue, String field, int maxLength, boolean required, Pattern allowedPattern) {
    if (!StringUtils.hasText(rawValue)) {
      if (required) {
        throw new ApplicationException(
            ErrorCode.VALIDATION_MISSING_REQUIRED_FIELD, field + " is required");
      }
      return null;
    }
    String value = rawValue.trim();
    if (value.length() > maxLength) {
      throw new ApplicationException(
          ErrorCode.VALIDATION_OUT_OF_RANGE, field + " exceeds max length " + maxLength);
    }
    rejectForbiddenValue(field, value);
    if (!allowedPattern.matcher(value).matches()) {
      throw new ApplicationException(
          ErrorCode.VALIDATION_INVALID_INPUT, field + " must be bounded allowlisted metadata");
    }
    return HtmlUtils.htmlEscape(value);
  }

  private String safeTraceId(String rawTraceId) {
    if (!StringUtils.hasText(rawTraceId)) {
      return null;
    }
    String traceId = rawTraceId.trim();
    if (!SAFE_TRACE_ID.matcher(traceId).matches()) {
      throw new ApplicationException(ErrorCode.VALIDATION_INVALID_INPUT, "traceId is invalid");
    }
    rejectForbiddenValue("traceId", traceId);
    return traceId;
  }

  private void rejectForbiddenValue(String field, String value) {
    TelemetryPrivacySanitizer.rejectForbiddenFreeText(field, value);
    String lower = value.toLowerCase(Locale.ROOT);
    boolean forbidden = FORBIDDEN_VALUE_FRAGMENTS.stream().anyMatch(lower::contains);
    if (forbidden
        || EMAIL_PATTERN.matcher(value).find()
        || URL_PATTERN.matcher(value).find()
        || UUID_PATTERN.matcher(value).find()
        || LONG_NUMERIC_ID_PATTERN.matcher(value).find()
        || TENANT_CODE_PATTERN.matcher(value).find()) {
      throw new ApplicationException(
          ErrorCode.VALIDATION_INVALID_INPUT,
          field + " contains unsupported private or secret text");
    }
  }

  private String writeMetadata(Map<String, String> metadata) {
    try {
      return objectMapper.writeValueAsString(metadata);
    } catch (Exception ex) {
      throw new ApplicationException(
          ErrorCode.SYSTEM_INTERNAL_ERROR, "Unable to serialize bug metadata", ex);
    }
  }

  private Map<String, String> readMetadata(String metadataJson) {
    if (!StringUtils.hasText(metadataJson)) {
      return Map.of();
    }
    try {
      return objectMapper.readValue(
          metadataJson, new TypeReference<LinkedHashMap<String, String>>() {});
    } catch (Exception ex) {
      throw new ApplicationException(
          ErrorCode.SYSTEM_INTERNAL_ERROR, "Stored bug metadata is invalid JSON", ex);
    }
  }

  private Map<String, String> currentStoredMetadata(Map<String, String> metadata) {
    if (metadata.isEmpty()) {
      return Map.of();
    }
    Map<String, String> sanitized = new LinkedHashMap<>();
    for (Map.Entry<String, String> entry : metadata.entrySet()) {
      String key = entry.getKey();
      if (!ALLOWED_KEYS.contains(key)) {
        throw currentStateViolation("Stored bug metadata has unsupported key: " + key, null);
      }
      String value =
          currentStoredToken(
              entry.getValue(), "route".equals(key) ? SAFE_ROUTE_TEMPLATE : SAFE_METADATA_TOKEN);
      if (value != null) {
        sanitized.put(key, value);
      }
    }
    return sanitized;
  }

  private String currentStoredText(String value) {
    if (!StringUtils.hasText(value)) {
      return null;
    }
    try {
      rejectForbiddenValue("bugReport", value);
      return value;
    } catch (ApplicationException ex) {
      throw currentStateViolation("Stored bug report text violates current privacy contract", ex);
    }
  }

  private String currentStoredToken(String value, Pattern allowedPattern) {
    if (!StringUtils.hasText(value)) {
      return null;
    }
    String candidate = value.trim();
    try {
      rejectForbiddenValue("bugReport", candidate);
    } catch (ApplicationException ex) {
      throw currentStateViolation("Stored bug metadata violates current privacy contract", ex);
    }
    if (!allowedPattern.matcher(candidate).matches()) {
      throw currentStateViolation("Stored bug metadata violates current allowlist contract", null);
    }
    return candidate;
  }

  private void copyAllowed(Map<String, String> tags, Map<String, String> metadata, String key) {
    putIfSafe(
        tags,
        key,
        metadata.get(key),
        "route".equals(key) ? SAFE_ROUTE_TEMPLATE : SAFE_METADATA_TOKEN);
  }

  private void putIfSafe(
      Map<String, String> tags, String key, String value, Pattern allowedPattern) {
    if (!StringUtils.hasText(value)) {
      return;
    }
    String candidate = value.trim();
    try {
      rejectForbiddenValue(key, candidate);
    } catch (ApplicationException ex) {
      throw currentStateViolation("Stored bug metadata violates current privacy contract", ex);
    }
    if (!allowedPattern.matcher(candidate).matches()) {
      throw currentStateViolation("Stored bug metadata violates current allowlist contract", null);
    }
    tags.put(key, candidate);
  }

  private ApplicationException currentStateViolation(String message, Exception cause) {
    return cause == null
        ? new ApplicationException(ErrorCode.SYSTEM_INTERNAL_ERROR, message)
        : new ApplicationException(ErrorCode.SYSTEM_INTERNAL_ERROR, message, cause);
  }

  private String hash(String value) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return HexFormat.of()
          .formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)))
          .substring(0, 16);
    } catch (Exception ex) {
      return "hash_unavailable";
    }
  }
}
