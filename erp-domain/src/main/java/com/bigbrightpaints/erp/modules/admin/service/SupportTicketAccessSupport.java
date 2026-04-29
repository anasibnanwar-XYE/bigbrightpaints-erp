package com.bigbrightpaints.erp.modules.admin.service;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.springframework.web.util.HtmlUtils;

import com.bigbrightpaints.erp.core.audit.AuditEvent;
import com.bigbrightpaints.erp.core.audit.AuditLog;
import com.bigbrightpaints.erp.core.audit.AuditService;
import com.bigbrightpaints.erp.core.exception.ApplicationException;
import com.bigbrightpaints.erp.core.exception.ErrorCode;
import com.bigbrightpaints.erp.core.security.SecurityActorResolver;
import com.bigbrightpaints.erp.modules.admin.domain.SupportTicket;
import com.bigbrightpaints.erp.modules.admin.domain.SupportTicketCategory;
import com.bigbrightpaints.erp.modules.admin.domain.SupportTicketMessage;
import com.bigbrightpaints.erp.modules.admin.domain.SupportTicketMessageAuthorRole;
import com.bigbrightpaints.erp.modules.admin.domain.SupportTicketMessageRepository;
import com.bigbrightpaints.erp.modules.admin.domain.SupportTicketMessageVisibility;
import com.bigbrightpaints.erp.modules.admin.domain.SupportTicketPriority;
import com.bigbrightpaints.erp.modules.admin.domain.SupportTicketRepository;
import com.bigbrightpaints.erp.modules.admin.dto.SupportTicketCreateRequest;
import com.bigbrightpaints.erp.modules.admin.dto.SupportTicketMessageRequest;
import com.bigbrightpaints.erp.modules.admin.dto.SupportTicketMessageResponse;
import com.bigbrightpaints.erp.modules.admin.dto.SupportTicketResponse;
import com.bigbrightpaints.erp.modules.auth.domain.UserAccount;
import com.bigbrightpaints.erp.modules.auth.domain.UserPrincipal;
import com.bigbrightpaints.erp.modules.company.domain.Company;
import com.bigbrightpaints.erp.shared.dto.PageResponse;

import jakarta.annotation.Nullable;

@Service
public class SupportTicketAccessSupport {

  private static final int EMBEDDED_MESSAGE_PREVIEW_SIZE = 5;

  private final SupportTicketRepository supportTicketRepository;
  private final SupportTicketMessageRepository supportTicketMessageRepository;
  private final SupportTicketGitHubSyncService supportTicketGitHubSyncService;
  private final AuditService auditService;

  public SupportTicketAccessSupport(
      SupportTicketRepository supportTicketRepository,
      SupportTicketMessageRepository supportTicketMessageRepository,
      SupportTicketGitHubSyncService supportTicketGitHubSyncService,
      AuditService auditService) {
    this.supportTicketRepository = supportTicketRepository;
    this.supportTicketMessageRepository = supportTicketMessageRepository;
    this.supportTicketGitHubSyncService = supportTicketGitHubSyncService;
    this.auditService = auditService;
  }

  @Transactional
  public SupportTicketResponse createTicket(Company company, SupportTicketCreateRequest request) {
    UserAccount actor = requireCurrentUser();

    SupportTicket ticket = new SupportTicket();
    ticket.setCompany(company);
    ticket.setUserId(actor.getId());
    ticket.setCategory(parseCategory(request.category()));
    ticket.setPriority(parsePriority(request.priority()));
    ticket.setSubject(normalizeRequired(request.subject(), "subject", 255));
    ticket.setDescription(normalizeRequired(request.description(), "description", 4000));

    SupportTicket saved = supportTicketRepository.save(ticket);
    auditRequired(
        saved,
        "support-ticket-created",
        SupportTicketMessageAuthorRole.TENANT,
        SupportTicketMessageVisibility.CUSTOMER);
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.registerSynchronization(
          new TransactionSynchronization() {
            @Override
            public void afterCommit() {
              supportTicketGitHubSyncService.submitGitHubIssueAsync(saved.getId());
            }
          });
    } else {
      supportTicketGitHubSyncService.submitGitHubIssueAsync(saved.getId());
    }
    return toResponses(List.of(saved), actor.getId()).getFirst();
  }

  @Transactional
  public SupportTicketMessageResponse addMessage(
      SupportTicket ticket,
      SupportTicketMessageRequest request,
      SupportTicketMessageAuthorRole authorRole,
      SupportTicketMessageVisibility visibility) {
    UserAccount actor = requireCurrentUser();
    SupportTicketMessage message = new SupportTicketMessage();
    message.setTicket(ticket);
    message.setCompany(ticket.getCompany());
    message.setAuthorUserId(actor.getId());
    message.setAuthorRole(authorRole);
    message.setVisibility(visibility);
    message.setContent(sanitizeContent(request == null ? null : request.content()));
    SupportTicketMessage saved = supportTicketMessageRepository.saveAndFlush(message);
    Long auditEventId =
        auditRequired(ticket, supportAuditReason(visibility), authorRole, visibility);
    saved.setAuditEventId(auditEventId);
    supportTicketMessageRepository.saveAndFlush(saved);
    return toMessageResponses(List.of(saved)).getFirst();
  }

  @Transactional(readOnly = true)
  public PageResponse<SupportTicketMessageResponse> listCustomerMessages(
      SupportTicket ticket, int page, int size, boolean platformAudience) {
    Pageable pageable = PageRequest.of(requirePage(page), requireSize(size));
    Page<SupportTicketMessage> messages =
        supportTicketMessageRepository.findByTicketAndVisibilityOrderByCreatedAtAscIdAsc(
            ticket, SupportTicketMessageVisibility.CUSTOMER, pageable);
    return PageResponse.of(
        toMessageResponses(messages.getContent(), platformAudience),
        messages.getTotalElements(),
        messages.getNumber(),
        messages.getSize());
  }

  public UserAccount requireCurrentUser() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !authentication.isAuthenticated()) {
      throw new ApplicationException(
          ErrorCode.AUTH_INSUFFICIENT_PERMISSIONS, "Authentication is required");
    }
    Object principal = authentication.getPrincipal();
    if (principal instanceof UserPrincipal userPrincipal && userPrincipal.getUser() != null) {
      return userPrincipal.getUser();
    }
    String actor = SecurityActorResolver.resolveActorOrUnknown();
    if (!StringUtils.hasText(actor)
        || SecurityActorResolver.UNKNOWN_AUTH_ACTOR.equals(actor)
        || SecurityActorResolver.SYSTEM_PROCESS_ACTOR.equals(actor)) {
      throw new ApplicationException(
          ErrorCode.AUTH_INSUFFICIENT_PERMISSIONS, "Authenticated user account is required");
    }
    throw new ApplicationException(
        ErrorCode.AUTH_INSUFFICIENT_PERMISSIONS,
        "Authenticated principal is not a user account: " + actor);
  }

  public Long requireTicketId(@Nullable Long ticketId) {
    if (ticketId == null) {
      throw new ApplicationException(
          ErrorCode.VALIDATION_MISSING_REQUIRED_FIELD, "ticketId is required");
    }
    return ticketId;
  }

  public ApplicationException notFound(Long ticketId) {
    return new ApplicationException(
        ErrorCode.BUSINESS_ENTITY_NOT_FOUND, "Support ticket not found: " + ticketId);
  }

  public List<SupportTicketResponse> toResponses(
      List<SupportTicket> tickets, @Nullable Long actorUserId) {
    if (tickets == null || tickets.isEmpty()) {
      return List.of();
    }

    Map<Long, String> requesterEmails = resolveRequesterEmails(tickets, actorUserId);
    return tickets.stream()
        .map(
            ticket -> {
              String companyCode =
                  ticket.getCompany() != null ? ticket.getCompany().getCode() : null;
              return new SupportTicketResponse(
                  ticket.getId(),
                  ticket.getPublicId(),
                  companyCode,
                  ticket.getUserId(),
                  requesterEmails.get(ticket.getUserId()),
                  ticket.getCategory(),
                  ticket.getPriority(),
                  ticket.getSubject(),
                  ticket.getDescription(),
                  ticket.getStatus(),
                  ticket.getGithubIssueNumber(),
                  ticket.getGithubIssueUrl(),
                  ticket.getGithubIssueState(),
                  ticket.getGithubSyncedAt(),
                  ticket.getGithubLastError(),
                  ticket.getResolvedAt(),
                  ticket.getResolvedNotificationSentAt(),
                  ticket.getCreatedAt(),
                  ticket.getUpdatedAt(),
                  customerMessagePreview(ticket, false));
            })
        .toList();
  }

  public List<SupportTicketMessageResponse> customerMessagePreview(
      SupportTicket ticket, boolean platformAudience) {
    return toMessageResponses(
        supportTicketMessageRepository
            .findByTicketAndVisibilityOrderByCreatedAtAscIdAsc(
                ticket,
                SupportTicketMessageVisibility.CUSTOMER,
                PageRequest.of(0, EMBEDDED_MESSAGE_PREVIEW_SIZE))
            .getContent(),
        platformAudience);
  }

  public List<SupportTicketMessageResponse> internalNotePreview(SupportTicket ticket) {
    return toMessageResponses(
        supportTicketMessageRepository
            .findByTicketAndVisibilityOrderByCreatedAtAscIdAsc(
                ticket,
                SupportTicketMessageVisibility.INTERNAL,
                PageRequest.of(0, EMBEDDED_MESSAGE_PREVIEW_SIZE))
            .getContent());
  }

  @Transactional(readOnly = true)
  public PageResponse<SupportTicketMessageResponse> listMessagesByVisibility(
      SupportTicket ticket,
      List<SupportTicketMessageVisibility> visibilities,
      int page,
      int size,
      boolean platformAudience) {
    Pageable pageable = PageRequest.of(requirePage(page), requireSize(size));
    Page<SupportTicketMessage> messages =
        supportTicketMessageRepository.findByTicketAndVisibilityInOrderByCreatedAtAscIdAsc(
            ticket, visibilities, pageable);
    return PageResponse.of(
        toMessageResponses(messages.getContent(), platformAudience),
        messages.getTotalElements(),
        messages.getNumber(),
        messages.getSize());
  }

  public List<SupportTicketMessageResponse> toMessageResponses(
      List<SupportTicketMessage> messages) {
    return toMessageResponses(messages, true);
  }

  private List<SupportTicketMessageResponse> toMessageResponses(
      List<SupportTicketMessage> messages, boolean platformAudience) {
    if (messages == null || messages.isEmpty()) {
      return List.of();
    }
    Set<Long> authorIds =
        messages.stream()
            .map(SupportTicketMessage::getAuthorUserId)
            .filter(id -> id != null && id > 0)
            .collect(Collectors.toSet());
    Map<Long, String> emails =
        authorIds.isEmpty()
            ? Map.of()
            : supportTicketMessageRepository.findUsersByIdIn(authorIds).stream()
                .collect(
                    Collectors.toMap(
                        UserAccount::getId,
                        UserAccount::getEmail,
                        (existing, replacement) -> existing,
                        java.util.LinkedHashMap::new));
    return messages.stream()
        .map(
            message -> {
              boolean redactPlatformAuthor =
                  !platformAudience
                      && message.getAuthorRole() == SupportTicketMessageAuthorRole.SUPER_ADMIN;
              return new SupportTicketMessageResponse(
                  message.getId(),
                  message.getPublicId(),
                  message.getTicket().getId(),
                  redactPlatformAuthor ? null : message.getAuthorUserId(),
                  redactPlatformAuthor ? null : emails.get(message.getAuthorUserId()),
                  message.getAuthorRole(),
                  message.getVisibility(),
                  message.getContent(),
                  message.getCreatedAt(),
                  redactPlatformAuthor ? null : message.getAuditEventId());
            })
        .toList();
  }

  private SupportTicketCategory parseCategory(String rawCategory) {
    String normalized = normalizeRequired(rawCategory, "category", 32).toUpperCase(Locale.ROOT);
    try {
      return SupportTicketCategory.valueOf(normalized);
    } catch (IllegalArgumentException ex) {
      throw new ApplicationException(
          ErrorCode.VALIDATION_INVALID_INPUT, "Invalid category: " + rawCategory);
    }
  }

  private SupportTicketPriority parsePriority(String rawPriority) {
    if (!StringUtils.hasText(rawPriority)) {
      return SupportTicketPriority.NORMAL;
    }
    String normalized = normalizeRequired(rawPriority, "priority", 32).toUpperCase(Locale.ROOT);
    try {
      return SupportTicketPriority.valueOf(normalized);
    } catch (IllegalArgumentException ex) {
      throw new ApplicationException(
          ErrorCode.VALIDATION_INVALID_INPUT, "Invalid priority: " + rawPriority);
    }
  }

  private int requirePage(int page) {
    if (page < 0) {
      throw new ApplicationException(
          ErrorCode.VALIDATION_OUT_OF_RANGE, "page must be greater than or equal to 0");
    }
    return page;
  }

  private int requireSize(int size) {
    if (size < 1 || size > 100) {
      throw new ApplicationException(
          ErrorCode.VALIDATION_OUT_OF_RANGE, "size must be between 1 and 100");
    }
    return size;
  }

  private String sanitizeContent(String value) {
    String normalized = normalizeRequired(value, "content", 4000);
    return HtmlUtils.htmlEscape(normalized);
  }

  private String supportAuditReason(SupportTicketMessageVisibility visibility) {
    return visibility == SupportTicketMessageVisibility.INTERNAL
        ? "support-internal-note-created"
        : "support-message-created";
  }

  private Long auditRequired(
      SupportTicket ticket,
      String reason,
      SupportTicketMessageAuthorRole authorRole,
      SupportTicketMessageVisibility visibility) {
    AuditLog auditLog =
        auditService.logAuthSuccessRequired(
            AuditEvent.DATA_CREATE,
            currentActor(),
            ticket.getCompany().getCode(),
            Map.of(
                "reason",
                reason,
                "ticketId",
                String.valueOf(ticket.getId()),
                "ticketPublicId",
                ticket.getPublicId() == null ? "unassigned" : ticket.getPublicId().toString(),
                "targetCompanyCode",
                ticket.getCompany().getCode(),
                "targetCompanyId",
                String.valueOf(ticket.getCompany().getId()),
                "authorRole",
                authorRole.name(),
                "visibility",
                visibility.name()));
    if (auditLog == null || auditLog.getId() == null) {
      throw new ApplicationException(
          ErrorCode.BUSINESS_INVALID_STATE, "Support ticket audit event was not persisted");
    }
    return auditLog.getId();
  }

  private String currentActor() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    return authentication == null || !StringUtils.hasText(authentication.getName())
        ? "system"
        : authentication.getName();
  }

  private String normalizeRequired(String value, String fieldName, int maxLength) {
    if (!StringUtils.hasText(value)) {
      throw new ApplicationException(
          ErrorCode.VALIDATION_MISSING_REQUIRED_FIELD, fieldName + " is required");
    }
    String trimmed = value.trim();
    if (trimmed.length() > maxLength) {
      throw new ApplicationException(
          ErrorCode.VALIDATION_OUT_OF_RANGE, fieldName + " exceeds max length " + maxLength);
    }
    return trimmed;
  }

  private Map<Long, String> resolveRequesterEmails(
      List<SupportTicket> tickets, @Nullable Long actorUserId) {
    Set<Long> requesterIds =
        tickets.stream()
            .map(SupportTicket::getUserId)
            .filter(id -> id != null && id > 0)
            .collect(Collectors.toSet());
    if (requesterIds.isEmpty()) {
      return Map.of();
    }

    if (actorUserId != null && requesterIds.size() == 1 && requesterIds.contains(actorUserId)) {
      return Map.of(actorUserId, resolveActorEmail());
    }

    return supportTicketRepository.findUsersByIdIn(requesterIds).stream()
        .collect(
            Collectors.toMap(
                UserAccount::getId,
                UserAccount::getEmail,
                (existing, replacement) -> existing,
                java.util.LinkedHashMap::new));
  }

  private String resolveActorEmail() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null
        && authentication.getPrincipal() instanceof UserPrincipal userPrincipal
        && userPrincipal.getUser() != null) {
      return userPrincipal.getUser().getEmail();
    }
    String actor = SecurityActorResolver.resolveActorOrUnknown();
    if (StringUtils.hasText(actor)
        && !SecurityActorResolver.UNKNOWN_AUTH_ACTOR.equals(actor)
        && !SecurityActorResolver.SYSTEM_PROCESS_ACTOR.equals(actor)) {
      return actor;
    }
    return null;
  }
}
