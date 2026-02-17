package com.bigbrightpaints.erp.shared.dto;

import com.bigbrightpaints.erp.core.util.CompanyTime;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        boolean success,
        String message,
        T data,
        Instant timestamp
) {
    public static <T> ApiResponse<T> success(String message, T data) {
        return of(true, message, data);
    }

    public static <T> ApiResponse<T> success(T data) {
        return success(null, data);
    }

    public static <T> ApiResponse<T> failure(String message) {
        return failure(message, null);
    }

    public static <T> ApiResponse<T> failure(String message, T data) {
        return of(false, message, data);
    }

    private static <T> ApiResponse<T> of(boolean success, String message, T data) {
        return new ApiResponse<>(success, message, data, CompanyTime.now());
    }
}
