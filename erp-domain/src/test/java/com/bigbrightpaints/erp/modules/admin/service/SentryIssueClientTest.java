package com.bigbrightpaints.erp.modules.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.bigbrightpaints.erp.core.config.SentryIssueProperties;
import com.bigbrightpaints.erp.core.exception.ApplicationException;
import com.bigbrightpaints.erp.core.util.CompanyClock;
import com.bigbrightpaints.erp.core.util.CompanyTime;

class SentryIssueClientTest {

  @Test
  void fetchIssueUsesFixedSentryHostAndServerSideToken() {
    installCompanyTime(Instant.parse("2026-04-29T12:02:00Z"));
    SentryIssueProperties properties = configuredProperties();
    RestTemplateBuilder builder = Mockito.mock(RestTemplateBuilder.class);
    RestTemplate restTemplate = Mockito.mock(RestTemplate.class);
    when(builder.build()).thenReturn(restTemplate);
    when(restTemplate.exchange(
            eq("https://sentry.io/api/0/issues/ERP-123/"),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(String.class)))
        .thenReturn(
            ResponseEntity.ok(
                "{\"id\":\"ERP-123\",\"status\":\"unresolved\",\"permalink\":\"https://sentry.io/organizations/bbp-test/issues/ERP-123/\",\"project\":{\"slug\":\"erp-test\"}}"));

    SentryIssueClient client = new SentryIssueClient(properties, builder, new ObjectMapper());
    SentryIssueClient.SentryIssueResult result = client.fetchIssue("ERP-123");

    assertThat(result.status()).isEqualTo("UNRESOLVED");
    assertThat(result.issueUrl()).contains("sentry.io").doesNotContain("placeholder-sentry-token");
    @SuppressWarnings("unchecked")
    ArgumentCaptor<HttpEntity<Map<String, Object>>> requestCaptor =
        ArgumentCaptor.forClass(HttpEntity.class);
    verify(restTemplate)
        .exchange(
            eq("https://sentry.io/api/0/issues/ERP-123/"),
            eq(HttpMethod.GET),
            requestCaptor.capture(),
            eq(String.class));
    assertThat(requestCaptor.getValue().getHeaders().getFirst("Authorization"))
        .isEqualTo("Bearer placeholder-sentry-token");
  }

  @Test
  void customHostConfigurationIsNotUsedForSsrffetches() {
    SentryIssueProperties properties = configuredProperties();
    properties.setHost("https://169.254.169.254");
    RestTemplateBuilder builder = Mockito.mock(RestTemplateBuilder.class);
    when(builder.build()).thenReturn(Mockito.mock(RestTemplate.class));

    SentryIssueClient client = new SentryIssueClient(properties, builder, new ObjectMapper());

    assertThat(client.isEnabledAndConfigured()).isFalse();
    assertThatThrownBy(() -> client.fetchIssue("ERP-123")).isInstanceOf(ApplicationException.class);
  }

  private SentryIssueProperties configuredProperties() {
    SentryIssueProperties properties = new SentryIssueProperties();
    properties.setEnabled(true);
    properties.setAuthToken("placeholder-sentry-token");
    properties.setOrg("bbp-test");
    properties.setProject("erp-test");
    properties.setHost("https://sentry.io");
    return properties;
  }

  private void installCompanyTime(Instant now) {
    CompanyClock companyClock = Mockito.mock(CompanyClock.class);
    LocalDate today = LocalDate.ofInstant(now, ZoneOffset.UTC);
    when(companyClock.now(any())).thenReturn(now);
    when(companyClock.now(null)).thenReturn(now);
    when(companyClock.today(any())).thenReturn(today);
    when(companyClock.today(null)).thenReturn(today);
    new CompanyTime(companyClock);
  }
}
