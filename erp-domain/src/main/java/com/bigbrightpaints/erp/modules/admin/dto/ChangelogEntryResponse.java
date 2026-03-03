package com.bigbrightpaints.erp.modules.admin.dto;

import java.time.Instant;

public record ChangelogEntryResponse(
        Long id,
        String version,
        String title,
        String body,
        Instant publishedAt,
        String createdBy,
        boolean isHighlighted
) {
}
