package com.bigbrightpaints.erp.core.health;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Verifies required runtime configuration for prod readiness.
 */
@Component
@ConditionalOnProperty(
    prefix = "erp.environment.validation",
    name = "health-indicator.enabled",
    havingValue = "true")
public class RequiredConfigHealthIndicator implements HealthIndicator {

  private final String jwtSecret;
  private final String encryptionKey;
  private final boolean licenseEnforce;
  private final String licenseKey;
  private final boolean mailEnabled;
  private final String mailHost;
  private final String mailUsername;
  private final String mailPassword;
  private final boolean environmentValidationEnabled;
  private final boolean skipChecksWhenValidationDisabled;

  public RequiredConfigHealthIndicator(
      @Value("${jwt.secret:}") String jwtSecret,
      @Value("${erp.security.encryption.key:}") String encryptionKey,
      @Value("${erp.licensing.enforce:false}") boolean licenseEnforce,
      @Value("${erp.licensing.license-key:}") String licenseKey,
      @Value("${erp.mail.enabled:true}") boolean mailEnabled,
      @Value("${spring.mail.host:}") String mailHost,
      @Value("${spring.mail.username:}") String mailUsername,
      @Value("${spring.mail.password:}") String mailPassword,
      @Value("${erp.environment.validation.enabled:false}") boolean environmentValidationEnabled,
      @Value(
              "${erp.environment.validation.health-indicator.skip-when-validation-disabled:false}")
          boolean skipChecksWhenValidationDisabled) {
    this.jwtSecret = jwtSecret;
    this.encryptionKey = encryptionKey;
    this.licenseEnforce = licenseEnforce;
    this.licenseKey = licenseKey;
    this.mailEnabled = mailEnabled;
    this.mailHost = mailHost;
    this.mailUsername = mailUsername;
    this.mailPassword = mailPassword;
    this.environmentValidationEnabled = environmentValidationEnabled;
    this.skipChecksWhenValidationDisabled = skipChecksWhenValidationDisabled;
  }

  @Override
  public Health health() {
    if (!environmentValidationEnabled && skipChecksWhenValidationDisabled) {
      return Health.up()
          .withDetail("validationEnabled", false)
          .withDetail("skipWhenValidationDisabled", true)
          .withDetail("checksSkipped", true)
          .build();
    }

    Map<String, Object> details = new LinkedHashMap<>();
    List<String> missing = new ArrayList<>();

    boolean jwtOk = hasMinLength(jwtSecret, 32);
    boolean encryptionOk = hasMinLength(encryptionKey, 32);
    details.put("jwtSecretConfigured", jwtOk);
    details.put("encryptionKeyConfigured", encryptionOk);
    if (!jwtOk) {
      missing.add("jwt.secret");
    }
    if (!encryptionOk) {
      missing.add("erp.security.encryption.key");
    }

    boolean licenseOk = !licenseEnforce || StringUtils.hasText(licenseKey);
    details.put("licenseEnforced", licenseEnforce);
    details.put("licenseConfigured", licenseOk);
    if (!licenseOk) {
      missing.add("erp.licensing.license-key");
    }

    boolean mailOk = true;
    if (mailEnabled) {
      mailOk =
          StringUtils.hasText(mailHost)
              && StringUtils.hasText(mailUsername)
              && mailPasswordConfigured(mailPassword);
    }
    details.put("mailEnabled", mailEnabled);
    details.put("mailConfigured", mailOk);
    if (!mailOk) {
      missing.add("spring.mail.host/username/password");
    }

    Health.Builder builder = missing.isEmpty() ? Health.up() : Health.down();
    if (!missing.isEmpty()) {
      details.put("missing", missing);
    }
    return builder.withDetails(details).build();
  }

  private boolean hasMinLength(String value, int minLength) {
    return value != null && value.length() >= minLength;
  }

  private boolean mailPasswordConfigured(String password) {
    if (!StringUtils.hasText(password)) {
      return false;
    }
    return !"changeme".equalsIgnoreCase(password.trim());
  }
}
