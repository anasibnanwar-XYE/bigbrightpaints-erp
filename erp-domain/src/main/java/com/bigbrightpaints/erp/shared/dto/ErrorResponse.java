package com.bigbrightpaints.erp.shared.dto;

import com.bigbrightpaints.erp.core.util.CompanyTime;
import java.time.Instant;
import java.util.Map;

public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, Object> details
) {
    public static ErrorResponse of(int status, String error, String message, String path, Map<String, Object> details) {
        return new ErrorResponse(CompanyTime.now(), status, error, message, path, details);
    }
}
