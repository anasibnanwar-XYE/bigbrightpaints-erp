package com.bigbrightpaints.erp.core.web;

import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.bigbrightpaints.erp.core.exception.ErrorCode;
import com.bigbrightpaints.erp.orchestrator.service.CorrelationIdentifierSanitizer;
import com.bigbrightpaints.erp.shared.dto.ApiResponse;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class ApiTraceFilter extends OncePerRequestFilter {

  public static final String TRACE_HEADER = "X-Trace-Id";
  public static final String CORRELATION_HEADER = "X-Correlation-ID";
  private static final long MAX_CONTROL_PLANE_BODY_BYTES = 1_048_576L;
  private static final int MAX_CONTROL_PLANE_QUERY_CHARS = 2_048;
  private static final int MAX_CONTROL_PLANE_HEADER_CHARS = 4_096;

  private final ObjectMapper objectMapper;

  public ApiTraceFilter(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String traceId = resolveTraceId(request);
    RequestTraceContext.TraceMetadata metadata =
        RequestTraceContext.start(traceId, resolveCorrelationId(request, traceId));
    response.setHeader(TRACE_HEADER, metadata.traceId());
    response.setHeader(CORRELATION_HEADER, metadata.correlationId());
    try {
      if (isControlPlaneContractPath(request) && rejectOversizedInput(request, response)) {
        return;
      }
      filterChain.doFilter(request, response);
    } finally {
      response.setHeader(TRACE_HEADER, metadata.traceId());
      response.setHeader(CORRELATION_HEADER, metadata.correlationId());
      RequestTraceContext.clear();
    }
  }

  private boolean rejectOversizedInput(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    if (!acceptsJson(request)) {
      writeError(
          request,
          response,
          HttpStatus.NOT_ACCEPTABLE,
          ErrorCode.VALIDATION_INVALID_INPUT,
          "Requested response media type is not available",
          Map.of("accept", "must allow application/json"));
      return true;
    }
    String query = request.getQueryString();
    if (query != null && query.length() > MAX_CONTROL_PLANE_QUERY_CHARS) {
      writeError(
          request,
          response,
          HttpStatus.BAD_REQUEST,
          ErrorCode.VALIDATION_OUT_OF_RANGE,
          "Query string is too large",
          Map.of("query", "must be at most " + MAX_CONTROL_PLANE_QUERY_CHARS + " characters"));
      return true;
    }
    if (request.getContentLengthLong() > MAX_CONTROL_PLANE_BODY_BYTES) {
      writeError(
          request,
          response,
          HttpStatus.PAYLOAD_TOO_LARGE,
          ErrorCode.FILE_SIZE_EXCEEDED,
          "Request body is too large",
          Map.of("body", "must be at most " + MAX_CONTROL_PLANE_BODY_BYTES + " bytes"));
      return true;
    }
    Enumeration<String> names = request.getHeaderNames();
    while (names != null && names.hasMoreElements()) {
      String name = names.nextElement();
      Enumeration<String> values = request.getHeaders(name);
      while (values != null && values.hasMoreElements()) {
        String value = values.nextElement();
        if (value != null && value.length() > MAX_CONTROL_PLANE_HEADER_CHARS) {
          writeError(
              request,
              response,
              HttpStatus.BAD_REQUEST,
              ErrorCode.VALIDATION_OUT_OF_RANGE,
              "Request header is too large",
              Map.of(name, "must be at most " + MAX_CONTROL_PLANE_HEADER_CHARS + " characters"));
          return true;
        }
      }
    }
    return false;
  }

  private boolean acceptsJson(HttpServletRequest request) {
    String accept = request.getHeader(HttpHeaders.ACCEPT);
    if (!StringUtils.hasText(accept)) {
      return true;
    }
    try {
      return MediaType.parseMediaTypes(accept).stream()
          .anyMatch(
              mediaType ->
                  mediaType.includes(MediaType.APPLICATION_JSON)
                      || MediaType.APPLICATION_JSON.includes(mediaType));
    } catch (RuntimeException ex) {
      return false;
    }
  }

  private void writeError(
      HttpServletRequest request,
      HttpServletResponse response,
      HttpStatus status,
      ErrorCode code,
      String message,
      Map<String, String> errors)
      throws IOException {
    Map<String, Object> data = new HashMap<>();
    data.put("code", code.getCode());
    data.put("message", message);
    data.put("reason", message);
    data.put("traceId", RequestTraceContext.traceId());
    data.put("path", request.getRequestURI());
    data.put("errors", errors);
    response.setStatus(status.value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding("UTF-8");
    objectMapper.writeValue(response.getWriter(), ApiResponse.failure(message, data));
  }

  private boolean isControlPlaneContractPath(HttpServletRequest request) {
    String path = request.getRequestURI();
    return path != null
        && (path.startsWith("/api/v1/superadmin") || path.startsWith("/api/v1/auth"));
  }

  private String resolveTraceId(HttpServletRequest request) {
    String candidate = firstHeader(request, TRACE_HEADER, "X-Request-ID", "X-Request-Id");
    return CorrelationIdentifierSanitizer.sanitizeTraceIdOrGenerate(
        candidate, () -> UUID.randomUUID().toString());
  }

  private String resolveCorrelationId(HttpServletRequest request, String generatedTraceId) {
    String candidate = firstHeader(request, CORRELATION_HEADER, "X-Correlation-Id", "X-Request-ID");
    try {
      String sanitized = CorrelationIdentifierSanitizer.sanitizeOptionalRequestId(candidate);
      return StringUtils.hasText(sanitized) ? sanitized : generatedTraceId;
    } catch (RuntimeException ex) {
      return generatedTraceId;
    }
  }

  private String firstHeader(HttpServletRequest request, String... names) {
    for (String name : names) {
      String value = request.getHeader(name);
      if (StringUtils.hasText(value)) {
        return value;
      }
    }
    return null;
  }
}
