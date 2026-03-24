package com.bigbrightpaints.erp.shared.dto;

import java.time.Instant;
import java.util.Map;

import com.bigbrightpaints.erp.core.util.CompanyTime;

public record ErrorResponse(
    Instant timestamp,
    int status,
    String error,
    String message,
    String path,
    Map<String, Object> details) {
  public static ErrorResponse of(
      int status, String error, String message, String path, Map<String, Object> details) {
    return new ErrorResponse(CompanyTime.now(), status, error, message, path, details);
  }
}
