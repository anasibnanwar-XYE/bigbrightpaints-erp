package com.bigbrightpaints.erp.modules.admin.dto;

import java.time.Instant;
import java.util.UUID;

import com.bigbrightpaints.erp.modules.admin.domain.SupportTicketMessageAuthorRole;
import com.bigbrightpaints.erp.modules.admin.domain.SupportTicketMessageVisibility;

public record SupportTicketMessageResponse(
    Long id,
    UUID publicId,
    Long ticketId,
    Long authorUserId,
    String authorEmail,
    SupportTicketMessageAuthorRole authorRole,
    SupportTicketMessageVisibility visibility,
    String content,
    Instant createdAt,
    Long auditEventId) {}
