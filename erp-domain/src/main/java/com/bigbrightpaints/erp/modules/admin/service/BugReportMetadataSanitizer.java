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
    ticket.setBugEnvironment(safeToken(request.environment(), "environment", 64, false));
    ticket.setBugRelease(safeToken(request.release(), "release", 128, false));
    ticket.setBugTraceId(safeTraceId(request.traceId()));
    Map<String, String> metadata = sanitizeMetadata(request.metadata());
    ticket.setBugMetadataJson(metadata.isEmpty() ? null : writeMetadata(metadata));
  }

  public SupportTicketResponse.BugReport bugReport(SupportTicket ticket) {
    if (ticket.getCategory() != SupportTicketCategory.BUG) {
      return null;
    }
    Map<String, String> metadata = readMetadata(ticket.getBugMetadataJson());
    return new SupportTicketResponse.BugReport(
        ticket.getBugReproductionSteps(),
        ticket.getBugEnvironment(),
        ticket.getBugRelease(),
        ticket.getBugTraceId(),
        metadata,
        safeSentryMetadata(ticket));
  }

  public Map<String, String> safeSentryMetadata(SupportTicket ticket) {
    Map<String, String> tags = new LinkedHashMap<>();
    putIfPresent(tags, "environment", ticket.getBugEnvironment());
    putIfPresent(tags, "release", ticket.getBugRelease());
    putIfPresent(tags, "traceId", ticket.getBugTraceId());
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
          safeToken(entry.getValue(), "metadata." + key, MAX_METADATA_VALUE_LENGTH, true);
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

  private String safeToken(String rawValue, String field, int maxLength, boolean required) {
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
    if (forbidden) {
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
      return Map.of();
    }
  }

  private void copyAllowed(Map<String, String> tags, Map<String, String> metadata, String key) {
    putIfPresent(tags, key, metadata.get(key));
  }

  private void putIfPresent(Map<String, String> tags, String key, String value) {
    if (StringUtils.hasText(value)) {
      tags.put(key, value);
    }
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
