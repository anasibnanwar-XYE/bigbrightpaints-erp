package com.bigbrightpaints.erp.core.audit;

import java.util.Map;
import org.springframework.util.StringUtils;

/**
 * Shared schema contract for {@link AuditEvent#INTEGRATION_FAILURE} metadata.
 * Producers should populate required keys through this helper to avoid drift.
 */
public final class IntegrationFailureMetadataSchema {

    public static final String KEY_FAILURE_CODE = "failureCode";
    public static final String KEY_ERROR_CATEGORY = "errorCategory";
    public static final String KEY_ALERT_ROUTING_VERSION = "alertRoutingVersion";
    public static final String KEY_ALERT_ROUTE = "alertRoute";
    public static final String FALLBACK_VALUE = "UNKNOWN";

    private IntegrationFailureMetadataSchema() {
    }

    public static void applyRequiredFields(Map<String, String> metadata,
                                           String failureCode,
                                           String errorCategory,
                                           String alertRoutingVersion,
                                           String alertRoute) {
        if (metadata == null) {
            throw new IllegalArgumentException("metadata is required");
        }
        metadata.put(KEY_FAILURE_CODE, sanitizeValue(failureCode));
        metadata.put(KEY_ERROR_CATEGORY, sanitizeValue(errorCategory));
        metadata.put(KEY_ALERT_ROUTING_VERSION, sanitizeValue(alertRoutingVersion));
        metadata.put(KEY_ALERT_ROUTE, sanitizeValue(alertRoute));
    }

    private static String sanitizeValue(String value) {
        if (!StringUtils.hasText(value)) {
            return FALLBACK_VALUE;
        }
        return value.trim();
    }
}
