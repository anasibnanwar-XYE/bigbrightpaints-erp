package com.bigbrightpaints.erp.core.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.ServletException;

class ApiTraceFilterTest {

  private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
  private final ApiTraceFilter filter = new ApiTraceFilter(objectMapper);

  @AfterEach
  void clearTrace() {
    RequestTraceContext.clear();
  }

  @Test
  void acceptedCorrelationIdIsReturnedInHeadersAndBodyMetadata()
      throws ServletException, IOException {
    MockHttpServletRequest request =
        request("GET", "/api/v1/superadmin/dashboard", "trace-m2-001", "corr-m2-001");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(
        request,
        response,
        (servletRequest, servletResponse) ->
            objectMapper.writeValue(
                servletResponse.getWriter(), Map.of("metadata", ApiResponseMetadata.current())));

    assertThat(response.getHeader(ApiTraceFilter.TRACE_HEADER)).isEqualTo("trace-m2-001");
    assertThat(response.getHeader(ApiTraceFilter.CORRELATION_HEADER)).isEqualTo("corr-m2-001");
    Map<String, Object> body =
        objectMapper.readValue(response.getContentAsString(), new TypeReference<>() {});
    @SuppressWarnings("unchecked")
    Map<String, Object> metadata = (Map<String, Object>) body.get("metadata");
    assertThat(metadata)
        .containsEntry("traceId", "trace-m2-001")
        .containsEntry("correlationId", "corr-m2-001");
  }

  @Test
  void invalidCorrelationIdFallsBackToTraceWithoutEchoingRawValue()
      throws ServletException, IOException {
    MockHttpServletRequest request =
        request("GET", "/api/v1/superadmin/dashboard", "trace-m2-002", "bad correlation");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, new MockFilterChain());

    assertThat(response.getHeader(ApiTraceFilter.TRACE_HEADER)).isEqualTo("trace-m2-002");
    assertThat(response.getHeader(ApiTraceFilter.CORRELATION_HEADER)).isEqualTo("trace-m2-002");
    assertThat(response.getHeader(ApiTraceFilter.CORRELATION_HEADER)).doesNotContain("bad");
  }

  @Test
  void oversizedCorrelationIdIsHashedRatherThanEchoedRaw() throws ServletException, IOException {
    String oversized = "req-" + "a".repeat(220);
    MockHttpServletRequest request =
        request("GET", "/api/v1/superadmin/dashboard", "trace-m2-003", oversized);
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, new MockFilterChain());

    assertThat(response.getHeader(ApiTraceFilter.CORRELATION_HEADER))
        .startsWith("RIDH|")
        .hasSizeLessThanOrEqualTo(128)
        .doesNotContain("aaaa");
  }

  @Test
  void filterRejectedErrorsUseSameTraceInHeaderAndBody() throws ServletException, IOException {
    MockHttpServletRequest request =
        request(
            "GET", "/api/v1/superadmin/dashboard?probe=" + "x".repeat(2_100), "trace-m2-004", null);
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, new MockFilterChain());

    assertThat(response.getStatus()).isEqualTo(400);
    Map<String, Object> body =
        objectMapper.readValue(response.getContentAsString(), new TypeReference<>() {});
    @SuppressWarnings("unchecked")
    Map<String, Object> data = (Map<String, Object>) body.get("data");
    @SuppressWarnings("unchecked")
    Map<String, Object> metadata = (Map<String, Object>) body.get("metadata");
    assertThat(data.get("traceId")).isEqualTo("trace-m2-004");
    assertThat(metadata.get("traceId")).isEqualTo("trace-m2-004");
    assertThat(response.getHeader(ApiTraceFilter.TRACE_HEADER)).isEqualTo("trace-m2-004");
  }

  private MockHttpServletRequest request(
      String method, String pathAndQuery, String traceId, String correlationId) {
    String[] parts = pathAndQuery.split("\\?", 2);
    MockHttpServletRequest request = new MockHttpServletRequest(method, parts[0]);
    request.setServletPath(parts[0]);
    request.setRequestURI(parts[0]);
    if (parts.length == 2) {
      request.setQueryString(parts[1]);
    }
    if (traceId != null) {
      request.addHeader(ApiTraceFilter.TRACE_HEADER, traceId);
    }
    if (correlationId != null) {
      request.addHeader(ApiTraceFilter.CORRELATION_HEADER, correlationId);
    }
    return request;
  }

  private record ApiResponseMetadata(String traceId, String correlationId) {
    static ApiResponseMetadata current() {
      RequestTraceContext.TraceMetadata metadata = RequestTraceContext.currentOrCreate();
      return new ApiResponseMetadata(metadata.traceId(), metadata.correlationId());
    }
  }
}
