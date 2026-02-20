package com.bigbrightpaints.erp.modules.admin.dto;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class TenantRuntimePolicyUpdateRequestTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void tearDownValidator() {
        validatorFactory.close();
    }

    @Test
    void accessorsExposeUpdatePayload() {
        TenantRuntimePolicyUpdateRequest request = new TenantRuntimePolicyUpdateRequest(
                300,
                900,
                35,
                "HOLD",
                "Awaiting review",
                "Temporary guardrail"
        );

        assertThat(request.maxActiveUsers()).isEqualTo(300);
        assertThat(request.maxRequestsPerMinute()).isEqualTo(900);
        assertThat(request.maxConcurrentRequests()).isEqualTo(35);
        assertThat(request.holdState()).isEqualTo("HOLD");
        assertThat(request.holdReason()).isEqualTo("Awaiting review");
        assertThat(request.changeReason()).isEqualTo("Temporary guardrail");
    }

    @Test
    void validationRejectsNonPositiveLimitsAndOversizedReasons() {
        String tooLong = "x".repeat(301);
        TenantRuntimePolicyUpdateRequest request = new TenantRuntimePolicyUpdateRequest(
                0,
                -1,
                0,
                "ACTIVE",
                tooLong,
                tooLong
        );

        Set<ConstraintViolation<TenantRuntimePolicyUpdateRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(5);
        assertThat(violations)
                .extracting(v -> v.getPropertyPath().toString())
                .containsExactlyInAnyOrder(
                        "maxActiveUsers",
                        "maxRequestsPerMinute",
                        "maxConcurrentRequests",
                        "holdReason",
                        "changeReason"
                );
        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .containsExactlyInAnyOrder(
                        "maxActiveUsers must be at least 1",
                        "maxRequestsPerMinute must be at least 1",
                        "maxConcurrentRequests must be at least 1",
                        "holdReason must be at most 300 characters",
                        "changeReason must be at most 300 characters"
                );
    }

    @Test
    void validationAcceptsBoundaryValues() {
        TenantRuntimePolicyUpdateRequest request = new TenantRuntimePolicyUpdateRequest(
                1,
                1,
                1,
                "HOLD",
                "x".repeat(300),
                "y".repeat(300)
        );

        assertThat(validator.validate(request)).isEmpty();
    }
}
