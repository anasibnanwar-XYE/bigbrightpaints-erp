package com.bigbrightpaints.erp.shared.dto;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonInclude;

import com.bigbrightpaints.erp.core.util.CompanyTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(boolean success, String message, T data, Instant timestamp) {
  public static <T> ApiResponse<T> success(String message, T data) {
    return new ApiResponse<>(true, message, data, CompanyTime.now());
  }

  public static <T> ApiResponse<T> success(T data) {
    return success(null, data);
  }

  public static <T> ApiResponse<T> failure(String message) {
    return new ApiResponse<>(false, message, null, CompanyTime.now());
  }

  public static <T> ApiResponse<T> failure(String message, T data) {
    return new ApiResponse<>(false, message, data, CompanyTime.now());
  }
}
