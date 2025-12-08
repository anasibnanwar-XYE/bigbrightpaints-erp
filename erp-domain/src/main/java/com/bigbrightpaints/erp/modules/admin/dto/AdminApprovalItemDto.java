package com.bigbrightpaints.erp.modules.admin.dto;

import java.time.Instant;
import java.util.UUID;

public record AdminApprovalItemDto(
        String type,
        Long id,
        UUID publicId,
        String reference,
        String status,
        String summary,
        Instant createdAt
) {}
