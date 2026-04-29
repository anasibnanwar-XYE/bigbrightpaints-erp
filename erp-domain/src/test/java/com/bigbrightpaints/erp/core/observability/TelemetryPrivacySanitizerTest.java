package com.bigbrightpaints.erp.core.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.bigbrightpaints.erp.core.exception.ApplicationException;

class TelemetryPrivacySanitizerTest {

  @Test
  void freeTextRejectsSecretsAndPrivateCanaries() {
    assertThatThrownBy(
            () ->
                TelemetryPrivacySanitizer.rejectForbiddenFreeText(
                    "content", "please inspect invoice-canary-tenant-private value"))
        .isInstanceOf(ApplicationException.class)
        .hasMessageContaining("private or secret text");

    assertThatThrownBy(
            () ->
                TelemetryPrivacySanitizer.rejectForbiddenFreeText(
                    "description", "Authorization: Bearer should-redact"))
        .isInstanceOf(ApplicationException.class)
        .hasMessageContaining("private or secret text");
  }

  @Test
  void tagValuesAreBoundedAndRedactPayloadCanaries() {
    assertThat(TelemetryPrivacySanitizer.safeTagValue("bug private-canary text", "fallback"))
        .isEqualTo("redacted");
    assertThat(TelemetryPrivacySanitizer.safeTagValue("route with spaces", "fallback"))
        .isEqualTo("route_with_spaces");
    assertThat(TelemetryPrivacySanitizer.safeTagValue("x".repeat(150), "fallback")).hasSize(128);
  }
}
