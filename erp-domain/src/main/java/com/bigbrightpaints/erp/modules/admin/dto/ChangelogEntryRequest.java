package com.bigbrightpaints.erp.modules.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ChangelogEntryRequest(
    @NotBlank
        @Pattern(
            regexp =
                "^(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)(?:-[0-9A-Za-z-]+(?:\\.[0-9A-Za-z-]+)*)?(?:\\+[0-9A-Za-z-]+(?:\\.[0-9A-Za-z-]+)*)?$",
            message = "version must be a valid semver string")
        @Size(max = 32)
        String version,
    @NotBlank @Size(max = 255) String title,
    @NotBlank String body,
    Boolean isHighlighted) {}
