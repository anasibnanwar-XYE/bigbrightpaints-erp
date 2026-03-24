package com.bigbrightpaints.erp.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record ResetPasswordRequest(
    @NotBlank String token, @NotBlank String newPassword, @NotBlank String confirmPassword) {}
