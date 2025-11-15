package com.bigbrightpaints.erp.modules.rbac.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record CreateRoleRequest(
        @NotBlank(message = "Role name is required")
        String name,
        @NotBlank(message = "Description is required")
        String description,
        @NotEmpty(message = "At least one permission is required")
        List<@NotBlank(message = "Permission code must not be blank") String> permissions
) {}
