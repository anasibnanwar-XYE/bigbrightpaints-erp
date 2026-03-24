package com.bigbrightpaints.erp.orchestrator.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ApproveOrderRequest(
    @NotBlank String orderId, @NotBlank String approvedBy, @NotNull BigDecimal totalAmount) {}
