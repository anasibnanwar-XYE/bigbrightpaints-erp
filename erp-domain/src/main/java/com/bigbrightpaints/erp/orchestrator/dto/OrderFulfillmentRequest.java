package com.bigbrightpaints.erp.orchestrator.dto;

import jakarta.validation.constraints.NotBlank;

public record OrderFulfillmentRequest(
        @NotBlank String status,
        String notes
) {}
