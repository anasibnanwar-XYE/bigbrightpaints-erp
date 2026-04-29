package com.bigbrightpaints.erp.modules.admin.service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.bigbrightpaints.erp.core.audit.AuditEvent;
import com.bigbrightpaints.erp.core.audit.AuditLog;
import com.bigbrightpaints.erp.core.audit.AuditService;
import com.bigbrightpaints.erp.core.exception.ApplicationException;
import com.bigbrightpaints.erp.core.exception.ErrorCode;
import com.bigbrightpaints.erp.core.util.CompanyTime;
import com.bigbrightpaints.erp.modules.admin.domain.SupportTicket;
import com.bigbrightpaints.erp.modules.admin.domain.SupportTicketCategory;
import com.bigbrightpaints.erp.modules.admin.domain.SupportTicketRepository;
import com.bigbrightpaints.erp.modules.admin.dto.SuperAdminSupportTicketDtos;

@Service
public class SupportTicketSentryLinkService {

  private static final Pattern SAFE_ISSUE_ID = Pattern.compile("[A-Za-z0-9_-]{1,128}");

  private final SupportTicketRepository supportTicketRepository;
  private final SupportTicketAccessSupport supportTicketAccessSupport;
  private final SentryIssueClient sentryIssueClient;
  private final BugReportMetadataSanitizer bugReportMetadataSanitizer;
  private final AuditService auditService;

  public SupportTicketSentryLinkService(
      SupportTicketRepository supportTicketRepository,
      SupportTicketAccessSupport supportTicketAccessSupport,
      SentryIssueClient sentryIssueClient,
      BugReportMetadataSanitizer bugReportMetadataSanitizer,
      AuditService auditService) {
    this.supportTicketRepository = supportTicketRepository;
    this.supportTicketAccessSupport = supportTicketAccessSupport;
    this.sentryIssueClient = sentryIssueClient;
    this.bugReportMetadataSanitizer = bugReportMetadataSanitizer;
    this.auditService = auditService;
  }

  @Transactional
  public SuperAdminSupportTicketDtos.SentryLinkResponse link(
      Long ticketId, SuperAdminSupportTicketDtos.SentryLinkRequest request) {
    SupportTicket ticket = requireBugTicket(ticketId);
    String issueId = validateIssueId(request == null ? null : request.issueId());
    ticket.setSentryIssueId(issueId);
    ticket.setSentryIssueUrl(sentryIssueClient.localIssueUrl(issueId));
    ticket.setSentryIssueStatus("LINKED");
    ticket.setSentryLinkedAt(CompanyTime.now(ticket.getCompany()));
    ticket.setSentryLastError(null);
    SupportTicket saved = supportTicketRepository.saveAndFlush(ticket);
    Long auditEventId = audit(saved, "sentry-issue-linked", Map.of("sentryStatus", "LINKED"));
    return response(saved, auditEventId);
  }

  @Transactional
  public SuperAdminSupportTicketDtos.SentryLinkResponse sync(Long ticketId) {
    SupportTicket ticket = requireBugTicket(ticketId);
    if (!StringUtils.hasText(ticket.getSentryIssueId())) {
      throw new ApplicationException(
          ErrorCode.VALIDATION_MISSING_REQUIRED_FIELD, "A Sentry issue must be linked first");
    }
    Long auditEventId;
    try {
      SentryIssueClient.SentryIssueResult result =
          sentryIssueClient.fetchIssue(ticket.getSentryIssueId());
      ticket.setSentryIssueUrl(result.issueUrl());
      ticket.setSentryIssueStatus(result.status());
      ticket.setSentrySyncedAt(result.syncedAt());
      ticket.setSentryLastSyncAt(result.syncedAt());
      ticket.setSentryLastError(null);
      SupportTicket saved = supportTicketRepository.saveAndFlush(ticket);
      auditEventId = audit(saved, "sentry-issue-synced", Map.of("sentryStatus", result.status()));
      return response(saved, auditEventId);
    } catch (ApplicationException ex) {
      ticket.setSentryLastSyncAt(CompanyTime.now(ticket.getCompany()));
      ticket.setSentryLastError(sanitizeError(ex.getUserMessage()));
      SupportTicket saved = supportTicketRepository.saveAndFlush(ticket);
      auditEventId =
          audit(saved, "sentry-issue-sync-failed", Map.of("sentryStatus", "SYNC_FAILED"));
      return response(saved, auditEventId);
    } catch (RuntimeException ex) {
      ticket.setSentryLastSyncAt(CompanyTime.now(ticket.getCompany()));
      ticket.setSentryLastError("Sentry sync failed");
      SupportTicket saved = supportTicketRepository.saveAndFlush(ticket);
      auditEventId =
          audit(saved, "sentry-issue-sync-failed", Map.of("sentryStatus", "SYNC_FAILED"));
      return response(saved, auditEventId);
    }
  }

  public SuperAdminSupportTicketDtos.SentryLinkResponse response(
      SupportTicket ticket, Long auditEventId) {
    return new SuperAdminSupportTicketDtos.SentryLinkResponse(
        ticket.getSentryIssueId(),
        ticket.getSentryIssueUrl(),
        ticket.getSentryIssueStatus(),
        ticket.getSentryLinkedAt(),
        ticket.getSentrySyncedAt(),
        ticket.getSentryLastSyncAt(),
        ticket.getSentryLastError(),
        bugReportMetadataSanitizer.safeSentryMetadata(ticket),
        auditEventId);
  }

  private SupportTicket requireBugTicket(Long ticketId) {
    Long resolvedTicketId = supportTicketAccessSupport.requireTicketId(ticketId);
    SupportTicket ticket =
        supportTicketRepository
            .findById(resolvedTicketId)
            .orElseThrow(() -> supportTicketAccessSupport.notFound(resolvedTicketId));
    if (ticket.getCategory() != SupportTicketCategory.BUG) {
      throw new ApplicationException(
          ErrorCode.VALIDATION_INVALID_INPUT, "Sentry links are only allowed for bug reports");
    }
    return ticket;
  }

  private String validateIssueId(String rawIssueId) {
    if (!StringUtils.hasText(rawIssueId)) {
      throw new ApplicationException(
          ErrorCode.VALIDATION_MISSING_REQUIRED_FIELD, "issueId is required");
    }
    String issueId = rawIssueId.trim();
    if (!SAFE_ISSUE_ID.matcher(issueId).matches()) {
      throw new ApplicationException(
          ErrorCode.VALIDATION_INVALID_INPUT,
          "issueId must be a configured Sentry issue ID or slug, not a URL or credential");
    }
    return issueId;
  }

  private String sanitizeError(String message) {
    if (!StringUtils.hasText(message)) {
      return "Sentry sync failed";
    }
    String sanitized = message.replaceAll("(?i)bearer\\s+[A-Za-z0-9._:-]+", "Bearer [redacted]");
    return sanitized.length() > 256 ? sanitized.substring(0, 256) : sanitized;
  }

  private Long audit(SupportTicket ticket, String reason, Map<String, String> extraMetadata) {
    Map<String, String> metadata = new LinkedHashMap<>();
    metadata.put("reason", reason);
    metadata.put("ticketId", String.valueOf(ticket.getId()));
    metadata.put(
        "ticketPublicId",
        ticket.getPublicId() == null ? "unassigned" : ticket.getPublicId().toString());
    metadata.put("targetCompanyId", String.valueOf(ticket.getCompany().getId()));
    metadata.put(
        "targetTenantHash",
        bugReportMetadataSanitizer.safeSentryMetadata(ticket).get("tenantHash"));
    metadata.put("sentryIssueId", ticket.getSentryIssueId());
    metadata.putAll(extraMetadata);
    AuditLog auditLog =
        auditService.logAuthSuccessRequired(
            AuditEvent.DATA_UPDATE, currentActor(), ticket.getCompany().getCode(), metadata);
    if (auditLog == null || auditLog.getId() == null) {
      throw new ApplicationException(
          ErrorCode.BUSINESS_INVALID_STATE, "Support ticket Sentry audit event was not persisted");
    }
    return auditLog.getId();
  }

  private String currentActor() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    return authentication == null || !StringUtils.hasText(authentication.getName())
        ? "system"
        : authentication.getName();
  }
}
