package com.bigbrightpaints.erp.modules.admin.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.bigbrightpaints.erp.modules.admin.domain.SupportTicketCategory;
import com.bigbrightpaints.erp.modules.admin.domain.SupportTicketPriority;
import com.bigbrightpaints.erp.modules.admin.domain.SupportTicketStatus;

public record SupportTicketResponse(
    Long id,
    UUID publicId,
    String companyCode,
    Long userId,
    String requesterEmail,
    SupportTicketCategory category,
    SupportTicketPriority priority,
    String subject,
    String description,
    SupportTicketStatus status,
    Long githubIssueNumber,
    String githubIssueUrl,
    String githubIssueState,
    Instant githubSyncedAt,
    String githubLastError,
    Instant resolvedAt,
    Instant resolvedNotificationSentAt,
    Instant createdAt,
    Instant updatedAt,
    List<SupportTicketMessageResponse> messages) {}
