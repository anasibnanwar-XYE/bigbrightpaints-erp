package com.bigbrightpaints.erp.core.config;

import java.net.URI;
import java.util.Locale;
import java.util.Set;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@ConfigurationProperties(prefix = "erp.mail")
public class EmailProperties {

  private static final Set<String> LOOPBACK_HOSTS = Set.of("localhost", "127.0.0.1", "::1");

  private boolean enabled = false;
  private String fromAddress = "noreply@bigbrightpaints.com";
  private String baseUrl = "http://localhost:3004";
  private boolean sendCredentials = true;
  private boolean sendPasswordReset = true;

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public String getFromAddress() {
    return fromAddress;
  }

  public void setFromAddress(String fromAddress) {
    this.fromAddress = fromAddress;
  }

  public String getBaseUrl() {
    return baseUrl;
  }

  public void setBaseUrl(String baseUrl) {
    this.baseUrl = normalizeSafeBaseUrl(baseUrl);
  }

  public boolean isSendCredentials() {
    return sendCredentials;
  }

  public void setSendCredentials(boolean sendCredentials) {
    this.sendCredentials = sendCredentials;
  }

  public boolean isSendPasswordReset() {
    return sendPasswordReset;
  }

  public void setSendPasswordReset(boolean sendPasswordReset) {
    this.sendPasswordReset = sendPasswordReset;
  }

  public static String normalizeSafeBaseUrl(String rawBaseUrl) {
    if (!StringUtils.hasText(rawBaseUrl)) {
      throw new IllegalArgumentException("Invalid mail base URL: value is required");
    }
    String value = rawBaseUrl.trim();
    if (containsDisallowedControlCharacter(value)) {
      throw new IllegalArgumentException("Invalid mail base URL: control characters are forbidden");
    }
    if (value.contains("*")) {
      throw new IllegalArgumentException("Invalid mail base URL: wildcards are forbidden");
    }
    URI uri;
    try {
      uri = URI.create(value);
    } catch (IllegalArgumentException ex) {
      throw new IllegalArgumentException("Invalid mail base URL: malformed URI", ex);
    }
    String scheme = uri.getScheme();
    String host = uri.getHost();
    if (!StringUtils.hasText(scheme) || !StringUtils.hasText(host)) {
      throw new IllegalArgumentException("Invalid mail base URL: scheme and host are required");
    }
    String normalizedScheme = scheme.toLowerCase(Locale.ROOT);
    String normalizedHost = host.toLowerCase(Locale.ROOT);
    if (!"https".equals(normalizedScheme)
        && !("http".equals(normalizedScheme) && LOOPBACK_HOSTS.contains(normalizedHost))) {
      throw new IllegalArgumentException(
          "Invalid mail base URL: https is required except for loopback validation URLs");
    }
    if (uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null) {
      throw new IllegalArgumentException(
          "Invalid mail base URL: user-info, query, and fragment are forbidden");
    }
    String path = uri.getPath();
    if (path != null && !path.isBlank() && !path.equals("/")) {
      throw new IllegalArgumentException("Invalid mail base URL: path must be omitted");
    }
    String normalized = normalizedScheme + "://" + normalizedHost;
    if (uri.getPort() != -1) {
      normalized = normalized + ":" + uri.getPort();
    }
    return normalized;
  }

  private static boolean containsDisallowedControlCharacter(String value) {
    for (int i = 0; i < value.length(); i++) {
      char ch = value.charAt(i);
      if (ch == '\r' || ch == '\n' || Character.isISOControl(ch)) {
        return true;
      }
    }
    return false;
  }
}
