package com.bigbrightpaints.erp.core.config;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.bigbrightpaints.erp.core.security.CompanyContextHolder;
import com.bigbrightpaints.erp.core.web.RequestTraceContext;

import io.sentry.Hint;
import io.sentry.SentryEvent;
import io.sentry.SentryOptions;

@Configuration
@ConditionalOnExpression("T(org.springframework.util.StringUtils).hasText('${sentry.dsn:}')")
public class SentryConfig {

  @Bean
  public SentryOptions.BeforeSendCallback beforeSendCallback() {
    return (SentryEvent event, Hint hint) -> {
      enrichWithTenantContext(event);
      enrichWithUserContext(event);
      event.setTag("trace_id", RequestTraceContext.traceId());
      event.setTag("correlation_id", RequestTraceContext.correlationId());
      return event;
    };
  }

  private void enrichWithTenantContext(SentryEvent event) {
    String companyCode = CompanyContextHolder.getCompanyCode();
    if (companyCode != null) {
      event.setTag("tenant_hash", hash("tenant:" + companyCode));
    }
  }

  private void enrichWithUserContext(SentryEvent event) {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
      event.setTag("actor_hash", hash("actor:" + auth.getName()));
      event.setTag("actor_role", primaryRole(auth));
      event.setUser(null);
    }
  }

  private String primaryRole(Authentication auth) {
    return auth.getAuthorities().stream()
        .map(authority -> authority.getAuthority())
        .filter(role -> role != null && role.startsWith("ROLE_"))
        .sorted()
        .findFirst()
        .orElse("AUTHENTICATED");
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
