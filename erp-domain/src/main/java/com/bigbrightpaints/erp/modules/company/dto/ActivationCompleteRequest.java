package com.bigbrightpaints.erp.modules.company.dto;

import jakarta.validation.constraints.NotBlank;

public record ActivationCompleteRequest(
    @NotBlank String token, @NotBlank String newPassword, @NotBlank String confirmPassword) {}
