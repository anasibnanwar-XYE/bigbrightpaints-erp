package com.bigbrightpaints.erp.modules.admin.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.bigbrightpaints.erp.core.exception.ApplicationException;
import com.bigbrightpaints.erp.core.exception.ErrorCode;
import com.bigbrightpaints.erp.modules.admin.domain.SupportTicket;
import com.bigbrightpaints.erp.modules.admin.domain.SupportTicketMessageAuthorRole;
import com.bigbrightpaints.erp.modules.admin.domain.SupportTicketMessageVisibility;
import com.bigbrightpaints.erp.modules.admin.domain.SupportTicketPriority;
import com.bigbrightpaints.erp.modules.admin.domain.SupportTicketRepository;
import com.bigbrightpaints.erp.modules.admin.domain.SupportTicketStatus;
import com.bigbrightpaints.erp.modules.admin.dto.SuperAdminSupportTicketDtos;
import com.bigbrightpaints.erp.modules.admin.dto.SupportTicketMessageRequest;
import com.bigbrightpaints.erp.modules.admin.dto.SupportTicketMessageResponse;
import com.bigbrightpaints.erp.modules.auth.domain.UserAccount;
import com.bigbrightpaints.erp.modules.rbac.domain.Role;
import com.bigbrightpaints.erp.shared.dto.PageResponse;

@Service
public class SuperAdminSupportService {

  private static final Set<String> SORT_FIELDS = Set.of("createdAt", "updatedAt", "priority");

  private final SupportTicketRepository supportTicketRepository;
  private final SupportTicketAccessSupport supportTicketAccessSupport;

  public SuperAdminSupportService(
      SupportTicketRepository supportTicketRepository,
      SupportTicketAccessSupport supportTicketAccessSupport) {
    this.supportTicketRepository = supportTicketRepository;
    this.supportTicketAccessSupport = supportTicketAccessSupport;
  }

  @Transactional(readOnly = true)
  public PageResponse<SuperAdminSupportTicketDtos.QueueItem> listQueue(
      String status, String query, int page, int size, String sort) {
    Pageable pageable = PageRequest.of(requirePage(page), requireSize(size), parseSort(sort));
    SupportTicketStatus parsedStatus = parseStatus(status);
    String normalizedQuery = normalizeOptional(query);
    Page<SupportTicket> tickets =
        isPrioritySort(sort)
            ? supportTicketRepository.findSuperAdminQueueOrderByPriorityRank(
                parsedStatus,
                normalizedQuery,
                PageRequest.of(pageable.getPageNumber(), pageable.getPageSize()))
            : supportTicketRepository.findSuperAdminQueue(parsedStatus, normalizedQuery, pageable);
    Map<Long, UserAccount> requesters = requesters(tickets.getContent());
    List<SuperAdminSupportTicketDtos.QueueItem> content =
        tickets.getContent().stream().map(ticket -> queueItem(ticket, requesters)).toList();
    return PageResponse.of(
        content, tickets.getTotalElements(), tickets.getNumber(), tickets.getSize());
  }

  @Transactional(readOnly = true)
  public SuperAdminSupportTicketDtos.Detail getDetail(Long ticketId) {
    SupportTicket ticket = requireTicket(ticketId);
    Map<Long, UserAccount> requesters = requesters(List.of(ticket));
    UserAccount requester = requesters.get(ticket.getUserId());
    return new SuperAdminSupportTicketDtos.Detail(
        ticket.getId(),
        ticket.getPublicId(),
        ticket.getCompany().getId(),
        ticket.getCompany().getCode(),
        ticket.getCompany().getName(),
        ticket.getCategory(),
        ticket.getPriority(),
        ticket.getStatus(),
        ticket.getSubject(),
        ticket.getDescription(),
        requesterRole(requester),
        requester == null ? null : requester.getEmail(),
        ticket.getCreatedAt(),
        ticket.getUpdatedAt(),
        sla(ticket),
        supportTicketAccessSupport.customerMessagePreview(ticket, true),
        supportTicketAccessSupport.internalNotePreview(ticket));
  }

  @Transactional
  public SupportTicketMessageResponse addMessage(
      Long ticketId, SupportTicketMessageRequest request) {
    return supportTicketAccessSupport.addMessage(
        requireTicket(ticketId),
        request,
        SupportTicketMessageAuthorRole.SUPER_ADMIN,
        SupportTicketMessageVisibility.CUSTOMER);
  }

  @Transactional
  public SupportTicketMessageResponse addInternalNote(
      Long ticketId, SupportTicketMessageRequest request) {
    return supportTicketAccessSupport.addMessage(
        requireTicket(ticketId),
        request,
        SupportTicketMessageAuthorRole.SUPER_ADMIN,
        SupportTicketMessageVisibility.INTERNAL);
  }

  @Transactional(readOnly = true)
  public PageResponse<SupportTicketMessageResponse> listMessages(
      Long ticketId, int page, int size, boolean includeInternal) {
    SupportTicket ticket = requireTicket(ticketId);
    if (!includeInternal) {
      return supportTicketAccessSupport.listCustomerMessages(ticket, page, size, true);
    }
    return supportTicketAccessSupport.listMessagesByVisibility(
        ticket,
        List.of(SupportTicketMessageVisibility.CUSTOMER, SupportTicketMessageVisibility.INTERNAL),
        page,
        size,
        true);
  }

  private SupportTicket requireTicket(Long ticketId) {
    Long resolvedTicketId = supportTicketAccessSupport.requireTicketId(ticketId);
    return supportTicketRepository
        .findById(resolvedTicketId)
        .orElseThrow(() -> supportTicketAccessSupport.notFound(resolvedTicketId));
  }

  private SuperAdminSupportTicketDtos.QueueItem queueItem(
      SupportTicket ticket, Map<Long, UserAccount> requesters) {
    UserAccount requester = requesters.get(ticket.getUserId());
    return new SuperAdminSupportTicketDtos.QueueItem(
        ticket.getId(),
        ticket.getPublicId(),
        ticket.getCompany().getId(),
        ticket.getCompany().getCode(),
        ticket.getCompany().getName(),
        ticket.getCategory(),
        ticket.getPriority(),
        ticket.getStatus(),
        requesterRole(requester),
        requester == null ? null : requester.getEmail(),
        ticket.getCreatedAt(),
        ticket.getUpdatedAt(),
        sla(ticket));
  }

  private Map<Long, UserAccount> requesters(List<SupportTicket> tickets) {
    Set<Long> ids =
        tickets.stream()
            .map(SupportTicket::getUserId)
            .filter(id -> id != null && id > 0)
            .collect(Collectors.toSet());
    if (ids.isEmpty()) {
      return Map.of();
    }
    return supportTicketRepository.findUsersByIdIn(ids).stream()
        .collect(Collectors.toMap(UserAccount::getId, user -> user));
  }

  private String requesterRole(UserAccount user) {
    if (user == null || user.getRoles() == null) {
      return "UNKNOWN";
    }
    Set<String> roles = user.getRoles().stream().map(Role::getName).collect(Collectors.toSet());
    if (roles.contains("ROLE_DEALER")) {
      return "DEALER";
    }
    if (roles.contains("ROLE_ADMIN")) {
      return "TENANT_ADMIN";
    }
    if (roles.contains("ROLE_ACCOUNTING")) {
      return "TENANT_ACCOUNTING";
    }
    return "TENANT_USER";
  }

  private SuperAdminSupportTicketDtos.SlaSummary sla(SupportTicket ticket) {
    String tier = ticket.getCompany().getCommercialSupportTier();
    String normalizedTier =
        StringUtils.hasText(tier) ? tier.trim().toUpperCase(Locale.ROOT) : "STANDARD";
    SupportTicketPriority priority =
        ticket.getPriority() == null ? SupportTicketPriority.NORMAL : ticket.getPriority();
    Duration firstResponse = firstResponseWindow(normalizedTier, priority);
    Duration resolution = resolutionWindow(normalizedTier, priority);
    Instant createdAt = ticket.getCreatedAt();
    Instant firstDue = createdAt == null ? null : createdAt.plus(firstResponse);
    Instant resolutionDue = createdAt == null ? null : createdAt.plus(resolution);
    String status =
        resolutionDue != null && Instant.now().isAfter(resolutionDue) ? "BREACHED" : "PENDING";
    return new SuperAdminSupportTicketDtos.SlaSummary(
        normalizedTier + "-" + priority.name(), normalizedTier, firstDue, resolutionDue, status);
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

  private SupportTicketStatus parseStatus(String status) {
    if (!StringUtils.hasText(status)) {
      return null;
    }
    try {
      return SupportTicketStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException ex) {
      throw new ApplicationException(
          ErrorCode.VALIDATION_INVALID_INPUT, "Invalid support ticket status: " + status);
    }
  }

  private String normalizeOptional(String value) {
    return StringUtils.hasText(value) ? value.trim() : null;
  }

  private Sort parseSort(String sort) {
    String raw = StringUtils.hasText(sort) ? sort.trim() : "createdAt,desc";
    String[] parts = raw.split(",");
    String field = parts[0].trim();
    if (!SORT_FIELDS.contains(field)) {
      throw new ApplicationException(ErrorCode.VALIDATION_INVALID_INPUT, "Invalid sort: " + sort);
    }
    Sort.Direction direction =
        parts.length > 1 && "asc".equalsIgnoreCase(parts[1].trim())
            ? Sort.Direction.ASC
            : Sort.Direction.DESC;
    return Sort.by(direction, field).and(Sort.by(direction, "id"));
  }

  private boolean isPrioritySort(String sort) {
    String raw = StringUtils.hasText(sort) ? sort.trim() : "createdAt,desc";
    String[] parts = raw.split(",");
    return "priority".equals(parts[0].trim());
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
}
