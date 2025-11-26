package com.bigbrightpaints.erp.orchestrator.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;

public record DispatchRequest(
    @NotBlank String batchId,
    @NotBlank String requestedBy,
    @NotNull @DecimalMin(value = "0.01") BigDecimal postingAmount
) {}
