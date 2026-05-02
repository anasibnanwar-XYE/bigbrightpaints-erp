package com.bigbrightpaints.erp.core.observability;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@ConfigurationProperties(prefix = "erp.datadog.telemetry")
public class DatadogTelemetryProperties {

  private boolean enabled = true;
  private String apiKey;
  private String site = "us5.datadoghq.com";
  private String environment = "dev";
  private String release = "erp-domain@unknown";

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public String getApiKey() {
    return apiKey;
  }

  public void setApiKey(String apiKey) {
    this.apiKey = apiKey;
  }

  public String getSite() {
    return StringUtils.hasText(site) ? site.trim() : "us5.datadoghq.com";
  }

  public void setSite(String site) {
    this.site = site;
  }

  public String getEnvironment() {
    return StringUtils.hasText(environment) ? environment.trim() : "dev";
  }

  public void setEnvironment(String environment) {
    this.environment = environment;
  }

  public String getRelease() {
    return StringUtils.hasText(release) ? release.trim() : "erp-domain@unknown";
  }

  public void setRelease(String release) {
    this.release = release;
  }

  public boolean isApiKeyConfigured() {
    return StringUtils.hasText(apiKey);
  }
}
