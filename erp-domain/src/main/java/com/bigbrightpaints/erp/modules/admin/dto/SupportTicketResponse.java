package com.bigbrightpaints.erp.modules.admin.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;
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
    BugReport bugReport,
    SentryLink sentry,
    Instant resolvedAt,
    Instant resolvedNotificationSentAt,
    Instant createdAt,
    Instant updatedAt,
    List<SupportTicketMessageResponse> messages) {

  public record BugReport(
      String reproductionSteps,
      String environment,
      String release,
      String traceId,
      Map<String, String> metadata,
      Map<String, String> safeSentryMetadata) {}

  public record SentryLink(
      String issueId,
      String issueUrl,
      String status,
      Instant linkedAt,
      Instant syncedAt,
      Instant lastSyncAt,
      String lastError,
      Map<String, String> safeMetadata,
      Long auditEventId) {}
}
