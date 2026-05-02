package com.bigbrightpaints.erp.core.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import com.bigbrightpaints.erp.core.security.CompanyContextHolder;
import com.bigbrightpaints.erp.core.web.RequestTraceContext;

import io.sentry.SentryEvent;

class SentryConfigTest {

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
    CompanyContextHolder.clear();
    RequestTraceContext.clear();
  }

  @Test
  void beforeSendUsesPseudonymousBoundedMetadataOnly() {
    CompanyContextHolder.setCompanyCode("RAW-TENANT-CODE");
    RequestTraceContext.start("trace-m12-safe", "corr-m12-safe");
    SecurityContextHolder.getContext()
        .setAuthentication(
            new TestingAuthenticationToken(
                "raw.actor@example.test",
                "n/a",
                List.of(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"))));

    SentryEvent event = new SentryEvent();
    new SentryConfig().beforeSendCallback().execute(event, null);

    assertThat(event.getTags())
        .containsEntry("trace_id", "trace-m12-safe")
        .containsEntry("correlation_id", "corr-m12-safe")
        .containsEntry("actor_role", "ROLE_SUPER_ADMIN")
        .containsKeys("tenant_hash", "actor_hash")
        .doesNotContainEntry("tenant", "RAW-TENANT-CODE");
    assertThat(event.getTags().get("tenant_hash")).doesNotContain("RAW-TENANT-CODE");
    assertThat(event.getTags().get("actor_hash")).doesNotContain("raw.actor@example.test");
    assertThat(event.getUser()).isNull();
  }
}
