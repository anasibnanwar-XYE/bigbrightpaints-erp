package com.bigbrightpaints.erp.modules.admin.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Locale;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.bigbrightpaints.erp.core.config.SentryIssueProperties;
import com.bigbrightpaints.erp.core.exception.ApplicationException;
import com.bigbrightpaints.erp.core.exception.ErrorCode;
import com.bigbrightpaints.erp.core.util.CompanyTime;

@Component
public class SentryIssueClient {

  private static final String SENTRY_HOST = "https://sentry.io";

  private final SentryIssueProperties sentryIssueProperties;
  private final RestTemplate restTemplate;
  private final ObjectMapper objectMapper;

  public SentryIssueClient(
      SentryIssueProperties sentryIssueProperties,
      RestTemplateBuilder restTemplateBuilder,
      ObjectMapper objectMapper) {
    this.sentryIssueProperties = sentryIssueProperties;
    this.restTemplate = restTemplateBuilder.build();
    this.objectMapper = objectMapper;
  }

  public boolean isEnabledAndConfigured() {
    return sentryIssueProperties.isConfigured() && SENTRY_HOST.equals(normalizedHost());
  }

  public String localIssueUrl(String issueId) {
    if (!StringUtils.hasText(sentryIssueProperties.getOrg())) {
      return SENTRY_HOST + "/issues/" + url(issueId) + "/";
    }
    return SENTRY_HOST
        + "/organizations/"
        + url(sentryIssueProperties.getOrg())
        + "/issues/"
        + url(issueId)
        + "/";
  }

  public SentryIssueResult fetchIssue(String issueId) {
    ensureConfigured();
    String url = SENTRY_HOST + "/api/0/issues/" + url(issueId) + "/";
    try {
      ResponseEntity<String> response =
          restTemplate.exchange(
              url, HttpMethod.GET, new HttpEntity<>(defaultHeaders()), String.class);
      JsonNode root = requireBody(response.getBody());
      validateConfiguredProject(root);
      String permalink = root.path("permalink").asText(localIssueUrl(issueId));
      String status = normalizeStatus(root.path("status").asText(null));
      return new SentryIssueResult(issueId, permalink, status, CompanyTime.now());
    } catch (HttpStatusCodeException ex) {
      throw mapHttpException(ex);
    } catch (RestClientException ex) {
      throw new ApplicationException(
          ErrorCode.INTEGRATION_CONNECTION_FAILED, "Failed to sync Sentry issue", ex);
    }
  }

  private void ensureConfigured() {
    if (!isEnabledAndConfigured()) {
      throw new ApplicationException(
          ErrorCode.SYSTEM_CONFIGURATION_ERROR,
          "Sentry issue integration is disabled or not configured");
    }
  }

  private HttpHeaders defaultHeaders() {
    HttpHeaders headers = new HttpHeaders();
    headers.setAccept(MediaType.parseMediaTypes(MediaType.APPLICATION_JSON_VALUE));
    headers.setBearerAuth(sentryIssueProperties.getAuthToken().trim());
    headers.set("User-Agent", "bigbright-erp-superadmin-sentry");
    return headers;
  }

  private JsonNode requireBody(String responseBody) {
    if (!StringUtils.hasText(responseBody)) {
      throw new ApplicationException(
          ErrorCode.INTEGRATION_INVALID_RESPONSE, "Sentry issue response body is empty");
    }
    try {
      return objectMapper.readTree(responseBody);
    } catch (Exception ex) {
      throw new ApplicationException(
          ErrorCode.INTEGRATION_INVALID_RESPONSE, "Sentry issue response is not valid JSON", ex);
    }
  }

  private void validateConfiguredProject(JsonNode root) {
    JsonNode project = root.path("project");
    String configuredProject = sentryIssueProperties.getProject();
    if (!project.isMissingNode() && StringUtils.hasText(configuredProject)) {
      String slug = project.path("slug").asText("");
      String id = project.path("id").asText("");
      if (StringUtils.hasText(slug)
          && !configuredProject.equalsIgnoreCase(slug)
          && !configuredProject.equalsIgnoreCase(id)) {
        throw new ApplicationException(
            ErrorCode.INTEGRATION_INVALID_RESPONSE,
            "Sentry issue does not belong to the configured project");
      }
    }
  }

  private ApplicationException mapHttpException(HttpStatusCodeException ex) {
    int status = ex.getStatusCode().value();
    if (status == 401 || status == 403) {
      return new ApplicationException(
          ErrorCode.INTEGRATION_AUTHENTICATION_FAILED, "Sentry authentication failed", ex);
    }
    if (status == 404) {
      return new ApplicationException(
          ErrorCode.BUSINESS_ENTITY_NOT_FOUND, "Sentry issue was not found", ex);
    }
    if (status == 408 || status == 429 || status >= 500) {
      return new ApplicationException(
          ErrorCode.INTEGRATION_TIMEOUT, "Sentry service unavailable", ex);
    }
    return new ApplicationException(
        ErrorCode.INTEGRATION_INVALID_RESPONSE, "Sentry rejected the issue sync request", ex);
  }

  private String normalizeStatus(String rawStatus) {
    if (!StringUtils.hasText(rawStatus)) {
      return "UNKNOWN";
    }
    return rawStatus.trim().toUpperCase(Locale.ROOT);
  }

  private String normalizedHost() {
    return sentryIssueProperties.getHost().trim().replaceAll("/+$", "");
  }

  private String url(String value) {
    return URLEncoder.encode(value.trim(), StandardCharsets.UTF_8);
  }

  public record SentryIssueResult(
      String issueId, String issueUrl, String status, Instant syncedAt) {}
}
