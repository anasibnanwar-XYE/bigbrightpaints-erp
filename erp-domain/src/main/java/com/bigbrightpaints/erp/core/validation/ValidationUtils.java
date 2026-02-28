package com.bigbrightpaints.erp.core.validation;

import com.bigbrightpaints.erp.core.exception.ApplicationException;
import com.bigbrightpaints.erp.core.exception.ErrorCode;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

public final class ValidationUtils {

    private ValidationUtils() {
    }

    public static BigDecimal requirePositive(BigDecimal value, String fieldName) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            throw invalidInput(fieldName + " must be positive");
        }
        return value;
    }

    public static String requireNotBlank(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new ApplicationException(ErrorCode.VALIDATION_MISSING_REQUIRED_FIELD, fieldName + " is required");
        }
        return value.trim();
    }

    public static <T> T requireNotNull(T value, String fieldName) {
        if (value == null) {
            throw new ApplicationException(ErrorCode.VALIDATION_MISSING_REQUIRED_FIELD, fieldName + " is required");
        }
        return value;
    }

    public static <E extends Enum<E>> E parseEnum(Class<E> enumType, String rawValue, String fieldName) {
        String normalized = requireNotBlank(rawValue, fieldName).toUpperCase();
        try {
            return Enum.valueOf(enumType, normalized);
        } catch (IllegalArgumentException ex) {
            throw new ApplicationException(ErrorCode.VALIDATION_INVALID_INPUT,
                    "Invalid " + fieldName + ": " + rawValue,
                    ex);
        }
    }

    public static void validateDateRange(LocalDate startDate,
                                         LocalDate endDate,
                                         String startFieldName,
                                         String endFieldName) {
        requireNotNull(startDate, startFieldName);
        requireNotNull(endDate, endFieldName);
        if (startDate.isAfter(endDate)) {
            throw new ApplicationException(ErrorCode.VALIDATION_INVALID_DATE,
                    startFieldName + " must be on or before " + endFieldName);
        }
    }

    public static <T> T requireEntity(Optional<T> entity, String entityName, Object identifier) {
        if (entity == null || entity.isEmpty()) {
            throw new ApplicationException(
                    ErrorCode.BUSINESS_ENTITY_NOT_FOUND,
                    entityName + " not found: " + identifier);
        }
        return entity.get();
    }

    public static IllegalArgumentException invalidInput(String message) {
        return new IllegalArgumentException(message);
    }

    public static IllegalArgumentException invalidInput(String message, Throwable cause) {
        return new IllegalArgumentException(message, cause);
    }

    public static IllegalStateException invalidState(String message) {
        return new IllegalStateException(message);
    }

    public static IllegalStateException invalidState(String message, Throwable cause) {
        return new IllegalStateException(message, cause);
    }
}
