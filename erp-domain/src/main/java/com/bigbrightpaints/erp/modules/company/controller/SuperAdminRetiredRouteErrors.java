package com.bigbrightpaints.erp.modules.company.controller;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;

import com.bigbrightpaints.erp.core.web.RequestTraceContext;
import com.bigbrightpaints.erp.shared.dto.ApiResponse;

import jakarta.servlet.http.HttpServletRequest;

final class SuperAdminRetiredRouteErrors {

  private SuperAdminRetiredRouteErrors() {}

  static ResponseEntity<ApiResponse<Map<String, Object>>> gone(
      String code, String message, HttpServletRequest request, String fallbackPath) {
    Map<String, Object> data = new LinkedHashMap<>();
    data.put("code", code);
    data.put("message", message);
    data.put("reason", message);
    data.put("traceId", RequestTraceContext.traceId());
    data.put("path", resolvePath(request, fallbackPath));
    return ResponseEntity.status(HttpStatus.GONE).body(ApiResponse.failure(message, data));
  }

  private static String resolvePath(HttpServletRequest request, String fallbackPath) {
    if (request != null && StringUtils.hasText(request.getRequestURI())) {
      return request.getRequestURI();
    }
    return fallbackPath;
  }
}
