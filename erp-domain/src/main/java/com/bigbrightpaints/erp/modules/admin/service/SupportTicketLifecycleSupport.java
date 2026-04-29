package com.bigbrightpaints.erp.modules.admin.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

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
import com.bigbrightpaints.erp.modules.admin.domain.SupportTicketPriority;
import com.bigbrightpaints.erp.modules.admin.domain.SupportTicketRepository;
import com.bigbrightpaints.erp.modules.admin.domain.SupportTicketSlaStatus;
import com.bigbrightpaints.erp.modules.admin.domain.SupportTicketStatus;
import com.bigbrightpaints.erp.modules.admin.domain.SupportTicketTimelineEntry;
import com.bigbrightpaints.erp.modules.admin.domain.SupportTicketTimelineRepository;
import com.bigbrightpaints.erp.modules.admin.dto.SuperAdminSupportTicketDtos;
import com.bigbrightpaints.erp.modules.company.domain.Company;

@Service
public class SupportTicketLifecycleSupport {

  private static final String EVENT_CREATED = "TICKET_CREATED";
  private static final String EVENT_FIRST_RESPONSE = "FIRST_RESPONSE";
  private static final String EVENT_STATUS_CHANGED = "STATUS_CHANGED";
  private static final String EVENT_SLA_BREACHED = "SLA_BREACHED";
  private static final String EVENT_FEATURE_CONVERTED = "FEATURE_CONVERTED_TO_INCIDENT";
  private static final String EVENT_SLA_POLICY_RECALCULATED = "SLA_POLICY_RECALCULATED";
  private static final Set<SupportTicketStatus> ACTIVE_STATUSES =
      Set.of(SupportTicketStatus.OPEN, SupportTicketStatus.IN_PROGRESS);
  private static final Set<SupportTicketCategory> SLA_CATEGORIES =
      Set.of(SupportTicketCategory.SUPPORT, SupportTicketCategory.BUG);

  private final SupportTicketRepository supportTicketRepository;
  private final SupportTicketTimelineRepository timelineRepository;
  private final AuditService auditService;

  public SupportTicketLifecycleSupport(
      SupportTicketRepository supportTicketRepository,
      SupportTicketTimelineRepository timelineRepository,
      AuditService auditService) {
    this.supportTicketRepository = supportTicketRepository;
    this.timelineRepository = timelineRepository;
    this.auditService = auditService;
  }

  public void initializeSla(SupportTicket ticket, Instant baseTime) {
    if (ticket.getCategory() == SupportTicketCategory.FEATURE_REQUEST) {
      ticket.setSlaPolicyId("FEATURE_REQUEST-NONE");
      ticket.setSlaSupportTier(normalizeTier(ticket));
      ticket.setFirstResponseDueAt(null);
      ticket.setResolutionDueAt(null);
      ticket.setSlaStatus(SupportTicketSlaStatus.NOT_APPLICABLE);
      ticket.setBreachedAt(null);
      return;
    }

    SupportTicketPriority priority =
        ticket.getPriority() == null ? SupportTicketPriority.NORMAL : ticket.getPriority();
    String tier = normalizeTier(ticket);
    Instant effectiveBase =
        baseTime != null
            ? baseTime
            : ticket.getCreatedAt() != null ? ticket.getCreatedAt() : CompanyTime.now();
    ticket.setSlaPolicyId(tier + "-" + priority.name());
    ticket.setSlaSupportTier(tier);
    ticket.setFirstResponseDueAt(effectiveBase.plus(firstResponseWindow(tier, priority)));
    ticket.setResolutionDueAt(effectiveBase.plus(resolutionWindow(tier, priority)));
    ticket.setSlaStatus(SupportTicketSlaStatus.PENDING);
    ticket.setBreachedAt(null);
  }

  @Transactional
  public int recalculateActiveTenantTicketsForSupportTierChange(
      Company company, String oldSupportTier, String newSupportTier, Long planAuditEventId) {
    if (company == null) {
      throw new ApplicationException(
          ErrorCode.VALIDATION_MISSING_REQUIRED_FIELD, "company is required");
    }
    String normalizedOldTier = normalizeTier(oldSupportTier);
    String normalizedNewTier = normalizeTier(newSupportTier);
    if (normalizedOldTier.equals(normalizedNewTier)) {
      return 0;
    }
    List<SupportTicket> tickets =
        supportTicketRepository.findByCompanyAndStatusInAndCategoryInOrderByIdAsc(
            company, ACTIVE_STATUSES, SLA_CATEGORIES);
    int recalculated = 0;
    for (SupportTicket ticket : tickets) {
      if (recalculateTicketSla(ticket, normalizedOldTier, normalizedNewTier, planAuditEventId)) {
        recalculated++;
      }
    }
    return recalculated;
  }

  @Transactional
  public void recordCreation(SupportTicket ticket, Long auditEventId) {
    appendTimeline(
        ticket,
        EVENT_CREATED,
        null,
        statusName(ticket.getStatus()),
        null,
        categoryName(ticket.getCategory()),
        "support-ticket-created",
        auditEventId);
  }

  @Transactional
  public void recordFirstResponseIfNeeded(SupportTicket ticket, Long auditEventId) {
    if (ticket.getCategory() == SupportTicketCategory.FEATURE_REQUEST
        || ticket.getFirstRespondedAt() != null) {
      return;
    }
    Instant now = CompanyTime.now(ticket.getCompany());
    ticket.setFirstRespondedAt(now);
    if (ticket.getSlaStatus() == SupportTicketSlaStatus.PENDING) {
      ticket.setSlaStatus(SupportTicketSlaStatus.RESPONDED);
    }
    supportTicketRepository.saveAndFlush(ticket);
    appendTimeline(
        ticket,
        EVENT_FIRST_RESPONSE,
        statusName(ticket.getStatus()),
        statusName(ticket.getStatus()),
        categoryName(ticket.getCategory()),
        categoryName(ticket.getCategory()),
        "first-platform-visible-reply",
        auditEventId);
  }

  @Transactional
  public void changeStatus(
      SupportTicket ticket, SupportTicketStatus newStatus, String reason) {
    SupportTicketStatus previousStatus = ticket.getStatus();
    if (previousStatus == newStatus) {
      return;
    }
    if (!isAllowedTransition(previousStatus, newStatus)) {
      throw new ApplicationException(
          ErrorCode.VALIDATION_INVALID_INPUT,
          "Invalid support ticket status transition: " + previousStatus + " -> " + newStatus);
    }
    Long auditEventId =
        audit(
            ticket,
            "support-ticket-status-changed",
            Map.of(
                "oldStatus", previousStatus.name(),
                "newStatus", newStatus.name(),
                "reason", normalizeOptional(reason, "reason", 256)));
    ticket.setStatus(newStatus);
    if (newStatus == SupportTicketStatus.RESOLVED || newStatus == SupportTicketStatus.CLOSED) {
      ticket.setResolvedAt(CompanyTime.now(ticket.getCompany()));
      if (ticket.getSlaStatus() != SupportTicketSlaStatus.NOT_APPLICABLE) {
        ticket.setSlaStatus(SupportTicketSlaStatus.RESOLVED);
      }
    } else if (ticket.getCategory() != SupportTicketCategory.FEATURE_REQUEST
        && ticket.getSlaStatus() == SupportTicketSlaStatus.RESOLVED) {
      ticket.setSlaStatus(
          ticket.getFirstRespondedAt() == null
              ? SupportTicketSlaStatus.PENDING
              : SupportTicketSlaStatus.RESPONDED);
    }
    supportTicketRepository.saveAndFlush(ticket);
    appendTimeline(
        ticket,
        EVENT_STATUS_CHANGED,
        previousStatus.name(),
        newStatus.name(),
        categoryName(ticket.getCategory()),
        categoryName(ticket.getCategory()),
        normalizeOptional(reason, "reason", 256),
        auditEventId);
  }

  @Transactional
  public void convertFeatureRequestToIncident(SupportTicket ticket, String reason) {
    if (ticket.getCategory() != SupportTicketCategory.FEATURE_REQUEST) {
      throw new ApplicationException(
          ErrorCode.BUSINESS_INVALID_STATE, "Only feature requests can be converted to incidents");
    }
    Long auditEventId =
        audit(
            ticket,
            "feature-request-converted-to-incident",
            Map.of(
                "oldCategory", SupportTicketCategory.FEATURE_REQUEST.name(),
                "newCategory", SupportTicketCategory.BUG.name(),
                "reason", normalizeOptional(reason, "reason", 256)));
    ticket.setCategory(SupportTicketCategory.BUG);
    ticket.setConvertedToIncidentAt(CompanyTime.now(ticket.getCompany()));
    initializeSla(ticket, ticket.getConvertedToIncidentAt());
    supportTicketRepository.saveAndFlush(ticket);
    appendTimeline(
        ticket,
        EVENT_FEATURE_CONVERTED,
        statusName(ticket.getStatus()),
        statusName(ticket.getStatus()),
        SupportTicketCategory.FEATURE_REQUEST.name(),
        SupportTicketCategory.BUG.name(),
        normalizeOptional(reason, "reason", 256),
        auditEventId);
  }

  @Transactional
  public SuperAdminSupportTicketDtos.SlaRefreshResponse refreshBreaches(Instant asOf) {
    Instant serverNow = CompanyTime.now();
    if (asOf != null && asOf.isAfter(serverNow)) {
      throw new ApplicationException(
          ErrorCode.VALIDATION_INVALID_DATE, "SLA breach refresh asOf cannot be in the future");
    }
    Instant effectiveAsOf = asOf == null ? serverNow : asOf;
    List<SupportTicket> candidates =
        supportTicketRepository.findTop200ByStatusInAndSlaStatusNotOrderByResolutionDueAtAscIdAsc(
            ACTIVE_STATUSES, SupportTicketSlaStatus.BREACHED);
    int processed = 0;
    int breached = 0;
    List<Long> auditEventIds = new java.util.ArrayList<>();
    for (SupportTicket ticket : candidates) {
      if (ticket.getSlaStatus() == SupportTicketSlaStatus.NOT_APPLICABLE
          || ticket.getResolutionDueAt() == null) {
        continue;
      }
      processed++;
      boolean firstResponseBreached =
          ticket.getFirstRespondedAt() == null
              && ticket.getFirstResponseDueAt() != null
              && !effectiveAsOf.isBefore(ticket.getFirstResponseDueAt());
      boolean resolutionBreached = !effectiveAsOf.isBefore(ticket.getResolutionDueAt());
      if (!firstResponseBreached && !resolutionBreached) {
        continue;
      }
      Long auditEventId =
          audit(
              ticket,
              "support-ticket-sla-breached",
              Map.of(
                  "policyId", safe(ticket.getSlaPolicyId()),
                  "slaStatus", SupportTicketSlaStatus.BREACHED.name(),
                  "breachKind", resolutionBreached ? "RESOLUTION" : "FIRST_RESPONSE"));
      ticket.setSlaStatus(SupportTicketSlaStatus.BREACHED);
      ticket.setBreachedAt(effectiveAsOf);
      supportTicketRepository.saveAndFlush(ticket);
      appendTimeline(
          ticket,
          EVENT_SLA_BREACHED,
          statusName(ticket.getStatus()),
          statusName(ticket.getStatus()),
          categoryName(ticket.getCategory()),
          categoryName(ticket.getCategory()),
          resolutionBreached ? "resolution-overdue" : "first-response-overdue",
          auditEventId);
      auditEventIds.add(auditEventId);
      breached++;
    }
    return new SuperAdminSupportTicketDtos.SlaRefreshResponse(
        processed,
        breached,
        supportTicketRepository.countBySlaStatus(SupportTicketSlaStatus.BREACHED),
        auditEventIds);
  }

  @Transactional(readOnly = true)
  public List<SuperAdminSupportTicketDtos.TimelineItem> timeline(SupportTicket ticket) {
    return timelineRepository.findByTicketOrderByCreatedAtAscIdAsc(ticket).stream()
        .map(
            entry ->
                new SuperAdminSupportTicketDtos.TimelineItem(
                    entry.getId(),
                    entry.getPublicId(),
                    entry.getEventType(),
                    entry.getFromStatus(),
                    entry.getToStatus(),
                    entry.getFromCategory(),
                    entry.getToCategory(),
                    entry.getNote(),
                    entry.getAuditEventId(),
                    entry.getCreatedAt()))
        .toList();
  }

  public SuperAdminSupportTicketDtos.SlaSummary summary(SupportTicket ticket) {
    return new SuperAdminSupportTicketDtos.SlaSummary(
        ticket.getSlaPolicyId(),
        ticket.getSlaSupportTier(),
        ticket.getFirstResponseDueAt(),
        ticket.getResolutionDueAt(),
        ticket.getFirstRespondedAt(),
        ticket.getBreachedAt(),
        ticket.getSlaStatus());
  }

  public SupportTicketStatus parseStatus(String status) {
    if (!StringUtils.hasText(status)) {
      throw new ApplicationException(
          ErrorCode.VALIDATION_MISSING_REQUIRED_FIELD, "status is required");
    }
    try {
      return SupportTicketStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException ex) {
      throw new ApplicationException(
          ErrorCode.VALIDATION_INVALID_INPUT, "Invalid support ticket status: " + status);
    }
  }

  private void appendTimeline(
      SupportTicket ticket,
      String eventType,
      String fromStatus,
      String toStatus,
      String fromCategory,
      String toCategory,
      String note,
      Long auditEventId) {
    SupportTicketTimelineEntry entry = new SupportTicketTimelineEntry();
    entry.setTicket(ticket);
    entry.setCompany(ticket.getCompany());
    entry.setEventType(eventType);
    entry.setFromStatus(fromStatus);
    entry.setToStatus(toStatus);
    entry.setFromCategory(fromCategory);
    entry.setToCategory(toCategory);
    entry.setNote(note);
    entry.setAuditEventId(auditEventId);
    timelineRepository.saveAndFlush(entry);
  }

  private boolean recalculateTicketSla(
      SupportTicket ticket, String oldSupportTier, String newSupportTier, Long planAuditEventId) {
    if (ticket.getCategory() == SupportTicketCategory.FEATURE_REQUEST
        || !ACTIVE_STATUSES.contains(ticket.getStatus())) {
      return false;
    }
    String oldPolicyId = safe(ticket.getSlaPolicyId());
    String oldTier = safe(ticket.getSlaSupportTier());
    Instant oldFirstResponseDueAt = ticket.getFirstResponseDueAt();
    Instant oldResolutionDueAt = ticket.getResolutionDueAt();

    initializeSla(ticket, slaBaseTime(ticket));
    if (ticket.getFirstRespondedAt() != null) {
      ticket.setSlaStatus(SupportTicketSlaStatus.RESPONDED);
    }
    boolean changed =
        !oldPolicyId.equals(safe(ticket.getSlaPolicyId()))
            || !oldTier.equals(safe(ticket.getSlaSupportTier()))
            || !java.util.Objects.equals(oldFirstResponseDueAt, ticket.getFirstResponseDueAt())
            || !java.util.Objects.equals(oldResolutionDueAt, ticket.getResolutionDueAt());
    if (!changed) {
      return false;
    }
    Long auditEventId =
        audit(
            ticket,
            "support-ticket-sla-policy-recalculated",
            Map.of(
                "oldPolicyId",
                oldPolicyId,
                "newPolicyId",
                safe(ticket.getSlaPolicyId()),
                "oldSupportTier",
                oldTier,
                "newSupportTier",
                safe(ticket.getSlaSupportTier()),
                "oldFirstResponseDueAt",
                instantString(oldFirstResponseDueAt),
                "newFirstResponseDueAt",
                instantString(ticket.getFirstResponseDueAt()),
                "oldResolutionDueAt",
                instantString(oldResolutionDueAt),
                "newResolutionDueAt",
                instantString(ticket.getResolutionDueAt()),
                "planAuditEventId",
                planAuditEventId == null ? "" : String.valueOf(planAuditEventId)));
    supportTicketRepository.saveAndFlush(ticket);
    appendTimeline(
        ticket,
        EVENT_SLA_POLICY_RECALCULATED,
        statusName(ticket.getStatus()),
        statusName(ticket.getStatus()),
        categoryName(ticket.getCategory()),
        categoryName(ticket.getCategory()),
        "policy "
            + oldPolicyId
            + " -> "
            + safe(ticket.getSlaPolicyId())
            + "; tier "
            + oldSupportTier
            + " -> "
            + newSupportTier
            + "; firstResponseDueAt "
            + instantString(oldFirstResponseDueAt)
            + " -> "
            + instantString(ticket.getFirstResponseDueAt())
            + "; resolutionDueAt "
            + instantString(oldResolutionDueAt)
            + " -> "
            + instantString(ticket.getResolutionDueAt()),
        auditEventId);
    return true;
  }

  private Instant slaBaseTime(SupportTicket ticket) {
    if (ticket.getConvertedToIncidentAt() != null) {
      return ticket.getConvertedToIncidentAt();
    }
    return ticket.getCreatedAt() != null
        ? ticket.getCreatedAt()
        : CompanyTime.now(ticket.getCompany());
  }

  private Long audit(SupportTicket ticket, String reason, Map<String, String> metadata) {
    java.util.Map<String, String> auditMetadata = new java.util.LinkedHashMap<>();
    auditMetadata.put("reason", reason);
    auditMetadata.put("ticketId", String.valueOf(ticket.getId()));
    auditMetadata.put(
        "ticketPublicId",
        ticket.getPublicId() == null ? "unassigned" : ticket.getPublicId().toString());
    auditMetadata.put("targetCompanyCode", ticket.getCompany().getCode());
    auditMetadata.put("targetCompanyId", String.valueOf(ticket.getCompany().getId()));
    auditMetadata.putAll(metadata);
    AuditLog auditLog =
        auditService.logAuthSuccessRequired(
            AuditEvent.DATA_UPDATE, currentActor(), ticket.getCompany().getCode(), auditMetadata);
    if (auditLog == null || auditLog.getId() == null) {
      throw new ApplicationException(
          ErrorCode.BUSINESS_INVALID_STATE, "Support ticket audit event was not persisted");
    }
    return auditLog.getId();
  }

  private boolean isAllowedTransition(
      SupportTicketStatus previousStatus, SupportTicketStatus newStatus) {
    if (previousStatus == null || newStatus == null) {
      return false;
    }
    return switch (previousStatus) {
      case OPEN ->
          Set.of(
                  SupportTicketStatus.IN_PROGRESS,
                  SupportTicketStatus.RESOLVED,
                  SupportTicketStatus.CLOSED)
              .contains(newStatus);
      case IN_PROGRESS ->
          Set.of(SupportTicketStatus.OPEN, SupportTicketStatus.RESOLVED, SupportTicketStatus.CLOSED)
              .contains(newStatus);
      case RESOLVED ->
          Set.of(SupportTicketStatus.OPEN, SupportTicketStatus.CLOSED).contains(newStatus);
      case CLOSED -> newStatus == SupportTicketStatus.OPEN;
    };
  }

  private String normalizeTier(SupportTicket ticket) {
    String tier =
        ticket.getCompany() == null ? null : ticket.getCompany().getCommercialSupportTier();
    return normalizeTier(tier);
  }

  private String normalizeTier(String tier) {
    return StringUtils.hasText(tier) ? tier.trim().toUpperCase(Locale.ROOT) : "STANDARD";
  }

  private Duration firstResponseWindow(String supportTier, SupportTicketPriority priority) {
    long hours =
        switch (supportTier) {
          case "DEDICATED", "ENTERPRISE" -> 2;
          case "PRIORITY" -> 4;
          default -> 8;
        };
    return Duration.ofHours(Math.max(1, hours - priorityUrgency(priority)));
  }

  private Duration resolutionWindow(String supportTier, SupportTicketPriority priority) {
    long hours =
        switch (supportTier) {
          case "DEDICATED", "ENTERPRISE" -> 24;
          case "PRIORITY" -> 48;
          default -> 72;
        };
    return Duration.ofHours(Math.max(4, hours - (priorityUrgency(priority) * 4L)));
  }

  private long priorityUrgency(SupportTicketPriority priority) {
    return switch (priority) {
      case URGENT -> 3;
      case HIGH -> 2;
      case NORMAL -> 1;
      case LOW -> 0;
    };
  }

  private String normalizeOptional(String value, String fieldName, int maxLength) {
    if (!StringUtils.hasText(value)) {
      return "";
    }
    String trimmed = value.trim();
    if (trimmed.length() > maxLength) {
      throw new ApplicationException(
          ErrorCode.VALIDATION_OUT_OF_RANGE, fieldName + " exceeds max length " + maxLength);
    }
    return trimmed;
  }

  private String statusName(SupportTicketStatus status) {
    return status == null ? null : status.name();
  }

  private String categoryName(SupportTicketCategory category) {
    return category == null ? null : category.name();
  }

  private String instantString(Instant instant) {
    return instant == null ? "" : instant.toString();
  }

  private String safe(String value) {
    return StringUtils.hasText(value) ? value : "UNKNOWN";
  }

  private String currentActor() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    return authentication == null || !StringUtils.hasText(authentication.getName())
        ? "system"
        : authentication.getName();
  }

  public Collection<SupportTicketStatus> activeStatuses() {
    return ACTIVE_STATUSES;
  }
}
