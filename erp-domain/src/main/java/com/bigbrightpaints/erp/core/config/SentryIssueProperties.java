package com.bigbrightpaints.erp.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@ConfigurationProperties(prefix = "erp.sentry.issue-link")
public class SentryIssueProperties {

  private static final String DEFAULT_HOST = "https://sentry.io";

  private boolean enabled = true;
  private String authToken;
  private String org;
  private String project;
  private String host = DEFAULT_HOST;

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public String getAuthToken() {
    return authToken;
  }

  public void setAuthToken(String authToken) {
    this.authToken = authToken;
  }

  public String getOrg() {
    return org;
  }

  public void setOrg(String org) {
    this.org = org;
  }

  public String getProject() {
    return project;
  }

  public void setProject(String project) {
    this.project = project;
  }

  public String getHost() {
    return StringUtils.hasText(host) ? host.trim() : DEFAULT_HOST;
  }

  public void setHost(String host) {
    this.host = host;
  }

  public boolean isConfigured() {
    return enabled
        && StringUtils.hasText(authToken)
        && StringUtils.hasText(org)
        && StringUtils.hasText(project);
  }
}
