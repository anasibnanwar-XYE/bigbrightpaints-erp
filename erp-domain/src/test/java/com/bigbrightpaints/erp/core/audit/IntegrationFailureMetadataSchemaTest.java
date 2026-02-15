package com.bigbrightpaints.erp.core.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class IntegrationFailureMetadataSchemaTest {

    @Test
    void applyRequiredFieldsPopulatesTrimmedValues() {
        Map<String, String> metadata = new HashMap<>();

        IntegrationFailureMetadataSchema.applyRequiredFields(
                metadata,
                "  SETTLEMENT_OPERATION_FAILED  ",
                "  VALIDATION  ",
                "  INTEGRATION_FAILURE_V1  ",
                "  SEV3_TICKET  ");

        assertThat(metadata)
                .containsEntry(IntegrationFailureMetadataSchema.KEY_FAILURE_CODE, "SETTLEMENT_OPERATION_FAILED")
                .containsEntry(IntegrationFailureMetadataSchema.KEY_ERROR_CATEGORY, "VALIDATION")
                .containsEntry(IntegrationFailureMetadataSchema.KEY_ALERT_ROUTING_VERSION, "INTEGRATION_FAILURE_V1")
                .containsEntry(IntegrationFailureMetadataSchema.KEY_ALERT_ROUTE, "SEV3_TICKET");
    }

    @Test
    void applyRequiredFieldsFallsBackForBlankValues() {
        Map<String, String> metadata = new HashMap<>();

        IntegrationFailureMetadataSchema.applyRequiredFields(metadata, null, " ", "", "\t");

        assertThat(metadata)
                .containsEntry(IntegrationFailureMetadataSchema.KEY_FAILURE_CODE, IntegrationFailureMetadataSchema.FALLBACK_VALUE)
                .containsEntry(IntegrationFailureMetadataSchema.KEY_ERROR_CATEGORY, IntegrationFailureMetadataSchema.FALLBACK_VALUE)
                .containsEntry(IntegrationFailureMetadataSchema.KEY_ALERT_ROUTING_VERSION, IntegrationFailureMetadataSchema.FALLBACK_VALUE)
                .containsEntry(IntegrationFailureMetadataSchema.KEY_ALERT_ROUTE, IntegrationFailureMetadataSchema.FALLBACK_VALUE);
    }

    @Test
    void applyRequiredFieldsRejectsNullMetadataMap() {
        assertThatThrownBy(() -> IntegrationFailureMetadataSchema.applyRequiredFields(
                null,
                "MALFORMED_REQUEST_PAYLOAD",
                "VALIDATION",
                "INTEGRATION_FAILURE_V1",
                "SEV3_TICKET"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("metadata is required");
    }
}
