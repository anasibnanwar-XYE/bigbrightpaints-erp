package com.bigbrightpaints.erp.modules.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SupportTicketCreateRequest(
        @NotBlank @Size(max = 32) String category,
        @NotBlank @Size(max = 255) String subject,
        @NotBlank @Size(max = 4000) String description
) {
}
