package com.bigbrightpaints.erp.modules.admin.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
    @NotBlank String displayName, @Size(min = 1) List<@NotBlank String> roles) {}
