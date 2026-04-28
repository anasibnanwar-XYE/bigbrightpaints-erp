package com.bigbrightpaints.erp.shared.dto;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonInclude;

import com.bigbrightpaints.erp.core.util.CompanyTime;
import com.bigbrightpaints.erp.core.web.RequestTraceContext;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
    boolean success, String message, T data, Instant timestamp, Metadata metadata) {

  public ApiResponse(boolean success, String message, T data, Instant timestamp) {
    this(success, message, data, timestamp, Metadata.current());
  }

  public static <T> ApiResponse<T> success(String message, T data) {
    return new ApiResponse<>(true, message, data, CompanyTime.now(), Metadata.current());
  }

  public static <T> ApiResponse<T> success(T data) {
    return success(null, data);
  }

  public static <T> ApiResponse<T> failure(String message) {
    return new ApiResponse<>(false, message, null, CompanyTime.now(), Metadata.current());
  }

  public static <T> ApiResponse<T> failure(String message, T data) {
    return new ApiResponse<>(false, message, data, CompanyTime.now(), Metadata.current());
  }

  public record Metadata(String traceId, String correlationId) {
    public static Metadata current() {
      RequestTraceContext.TraceMetadata trace = RequestTraceContext.currentOrCreate();
      return new Metadata(trace.traceId(), trace.correlationId());
    }
  }
}
