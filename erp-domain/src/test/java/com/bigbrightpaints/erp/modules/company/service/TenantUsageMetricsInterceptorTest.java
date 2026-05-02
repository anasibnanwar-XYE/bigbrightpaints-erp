package com.bigbrightpaints.erp.modules.company.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.bigbrightpaints.erp.core.security.CompanyContextHolder;

@ExtendWith(MockitoExtension.class)
class TenantUsageMetricsInterceptorTest {

  @Mock private TenantUsageMetricsService tenantUsageMetricsService;

  @AfterEach
  void clearTenantContext() {
    CompanyContextHolder.clear();
  }

  @Test
  void preHandle_recordsApiTrafficWhenTenantContextExists() {
    TenantUsageMetricsInterceptor interceptor =
        new TenantUsageMetricsInterceptor(tenantUsageMetricsService);
    CompanyContextHolder.setCompanyCode("ACME");

    boolean allowed =
        interceptor.preHandle(
            new MockHttpServletRequest("GET", "/api/v1/auth/me"),
            new MockHttpServletResponse(),
            new Object());

    assertThat(allowed).isTrue();
    verify(tenantUsageMetricsService).recordApiCall("ACME");
  }

  @Test
  void preHandle_skipsWhenTenantContextIsMissing() {
    TenantUsageMetricsInterceptor interceptor =
        new TenantUsageMetricsInterceptor(tenantUsageMetricsService);

    boolean allowed =
        interceptor.preHandle(
            new MockHttpServletRequest("GET", "/api/v1/auth/me"),
            new MockHttpServletResponse(),
            new Object());

    assertThat(allowed).isTrue();
    verifyNoInteractions(tenantUsageMetricsService);
  }
}
