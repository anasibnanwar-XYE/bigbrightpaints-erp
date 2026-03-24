package com.bigbrightpaints.erp.modules.admin.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record AdminNotifyRequest(
    @Email @NotBlank String to, @NotBlank String subject, @NotBlank String body) {}
