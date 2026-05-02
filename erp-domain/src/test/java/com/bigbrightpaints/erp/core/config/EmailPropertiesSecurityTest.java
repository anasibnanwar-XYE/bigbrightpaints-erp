package com.bigbrightpaints.erp.core.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class EmailPropertiesSecurityTest {

  @Test
  void baseUrlIsNormalizedAsConfiguredSafeOrigin() {
    EmailProperties properties = new EmailProperties();

    properties.setBaseUrl(" https://app.bigbrightpaints.com/ ");

    assertThat(properties.getBaseUrl()).isEqualTo("https://app.bigbrightpaints.com");
  }

  @Test
  void baseUrlRejectsHostHeaderStylePoisoningMaterial() {
    EmailProperties properties = new EmailProperties();

    assertThatThrownBy(
            () -> properties.setBaseUrl("https://app.bigbrightpaints.com\r\nHost: evil.test"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Invalid mail base URL");
  }

  @Test
  void baseUrlRejectsPublicHttpAndWildcardHosts() {
    EmailProperties properties = new EmailProperties();

    assertThatThrownBy(() -> properties.setBaseUrl("http://attacker.test"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("https");
    assertThatThrownBy(() -> properties.setBaseUrl("https://*.example.com"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("wildcards");
  }

  @Test
  void localHttpBaseUrlRemainsAllowedForValidationRuntime() {
    EmailProperties properties = new EmailProperties();

    properties.setBaseUrl("http://localhost:3004/");

    assertThat(properties.getBaseUrl()).isEqualTo("http://localhost:3004");
  }
}
