package com.bigbrightpaints.erp.core.security;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.bigbrightpaints.erp.core.exception.ErrorCode;
import com.bigbrightpaints.erp.core.web.RequestTraceContext;
import com.bigbrightpaints.erp.shared.dto.ApiResponse;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class SecurityErrorResponseWriter {

  private final ObjectMapper objectMapper;

  public SecurityErrorResponseWriter(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public void write(
      HttpServletRequest request,
      HttpServletResponse response,
      HttpStatus status,
      ErrorCode code,
      String message)
      throws IOException {
    String safeMessage = StringUtils.hasText(message) ? message : code.getDefaultMessage();
    Map<String, Object> data = new HashMap<>();
    data.put("code", code.getCode());
    data.put("message", safeMessage);
    data.put("reason", safeMessage);
    data.put("traceId", RequestTraceContext.traceId());
    data.put("path", request.getRequestURI());

    response.setStatus(status.value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding("UTF-8");
    objectMapper.writeValue(response.getWriter(), ApiResponse.failure(safeMessage, data));
  }
}
