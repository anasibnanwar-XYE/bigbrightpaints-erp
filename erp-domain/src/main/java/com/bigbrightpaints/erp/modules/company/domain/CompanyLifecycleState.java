package com.bigbrightpaints.erp.modules.company.domain;

import com.bigbrightpaints.erp.core.validation.ValidationUtils;
import java.util.Locale;
import java.util.Optional;
import org.springframework.util.StringUtils;

public enum CompanyLifecycleState {
    ACTIVE,
    SUSPENDED,
    DEACTIVATED;

    public static CompanyLifecycleState fromRequestValue(String rawValue) {
        return fromStoredValue(rawValue)
                .orElseThrow(() -> ValidationUtils.invalidInput("Unsupported company lifecycle state: " + rawValue));
    }

    public static Optional<CompanyLifecycleState> fromStoredValue(String rawValue) {
        if (!StringUtils.hasText(rawValue)) {
            return Optional.empty();
        }
        String normalized = rawValue.trim().toUpperCase(Locale.ROOT);
        if ("HOLD".equals(normalized)) {
            return Optional.of(SUSPENDED);
        }
        if ("BLOCKED".equals(normalized)) {
            return Optional.of(DEACTIVATED);
        }
        try {
            return Optional.of(CompanyLifecycleState.valueOf(normalized));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }
}
