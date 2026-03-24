package com.bigbrightpaints.erp.modules.factory.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;

public record FactoryTaskRequest(
    @NotBlank String title,
    String description,
    String assignee,
    String status,
    LocalDate dueDate,
    Long salesOrderId,
    Long packagingSlipId) {}
