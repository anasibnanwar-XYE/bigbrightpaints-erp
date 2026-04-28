package com.bigbrightpaints.erp.modules.admin.dto;

import java.time.Instant;
import java.util.List;

public record SuperAdminProfileDto(
    String displayName,
    String email,
    String phone,
    String avatarUrl,
    String timezone,
    String language,
    Instant lastLoginAt,
    List<SuperAdminProfileSessionDto> sessions) {}
