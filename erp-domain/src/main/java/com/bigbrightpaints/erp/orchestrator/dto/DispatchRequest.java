package com.bigbrightpaints.erp.orchestrator.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DispatchRequest(
    @NotBlank String batchId,
    @NotBlank String requestedBy,
    @NotNull @DecimalMin(value = "0.01") BigDecimal postingAmount) {}
