package com.bigbrightpaints.erp.modules.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SupportTicketMessageRequest(@NotBlank @Size(max = 4000) String content) {}
