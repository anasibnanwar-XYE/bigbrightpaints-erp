package com.bigbrightpaints.erp.modules.auth.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
    @Size(min = 1, max = 255) String displayName,
    @Size(max = 255) String preferredName,
    @Size(max = 255) String jobTitle,
    @Size(max = 512) String profilePictureUrl,
    @Size(max = 64) String phoneSecondary,
    @Email @Size(max = 255) String secondaryEmail) {}
