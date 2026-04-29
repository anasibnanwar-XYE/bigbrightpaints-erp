package com.bigbrightpaints.erp.core.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.servlet.ServletException;

class DatadogTelemetryFilterTest {

  @Test
  void telemetryProviderFailureDoesNotBreakSuperAdminCoreFlow()
      throws ServletException, IOException {
    DatadogTelemetryService telemetryService = Mockito.mock(DatadogTelemetryService.class);
    doThrow(new IllegalStateException("provider degraded with token redacted"))
        .when(telemetryService)
        .recordSuperAdminRequest(any(), anyInt(), anyLong(), anyBoolean());
    DatadogTelemetryFilter filter = new DatadogTelemetryFilter(telemetryService);
    MockHttpServletRequest request =
        new MockHttpServletRequest("GET", "/api/v1/superadmin/dashboard");
    MockHttpServletResponse response = new MockHttpServletResponse();

    assertThatCode(
            () ->
                filter.doFilter(
                    request,
                    response,
                    (servletRequest, servletResponse) ->
                        servletResponse.getWriter().write("{\"success\":true}")))
        .doesNotThrowAnyException();

    assertThat(response.getContentAsString()).contains("\"success\":true");
  }

  @Test
  void nonSuperAdminPathsAreIgnored() throws ServletException, IOException {
    DatadogTelemetryService telemetryService = Mockito.mock(DatadogTelemetryService.class);
    DatadogTelemetryFilter filter = new DatadogTelemetryFilter(telemetryService);
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/invoices/123");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, new MockFilterChain());

    Mockito.verifyNoInteractions(telemetryService);
  }
}
