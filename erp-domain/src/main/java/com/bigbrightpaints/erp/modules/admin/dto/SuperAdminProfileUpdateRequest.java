package com.bigbrightpaints.erp.modules.admin.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SuperAdminProfileUpdateRequest(
    @Size(max = 120, message = "displayName must be at most 120 characters") String displayName,
    @Size(max = 64, message = "phone must be at most 64 characters") String phone,
    @Size(max = 500, message = "avatarUrl must be at most 500 characters") String avatarUrl,
    @Size(max = 64, message = "timezone must be at most 64 characters") String timezone,
    @Size(max = 16, message = "language must be at most 16 characters")
        @Pattern(
            regexp = "^[A-Za-z]{2,3}([_-][A-Za-z]{2})?$",
            message = "language must be a short locale code")
        String language) {}
