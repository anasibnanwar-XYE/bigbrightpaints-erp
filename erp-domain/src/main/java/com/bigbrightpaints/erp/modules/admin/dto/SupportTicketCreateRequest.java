package com.bigbrightpaints.erp.modules.admin.dto;

import java.util.Map;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SupportTicketCreateRequest(
    @NotBlank @Size(max = 32) String category,
    @Size(max = 32) String priority,
    @NotBlank @Size(max = 255) String subject,
    @NotBlank @Size(max = 4000) String description,
    @Size(max = 2000) String reproductionSteps,
    @Size(max = 64) String environment,
    @Size(max = 128) String release,
    @Size(max = 128) String traceId,
    Map<String, String> metadata) {}
