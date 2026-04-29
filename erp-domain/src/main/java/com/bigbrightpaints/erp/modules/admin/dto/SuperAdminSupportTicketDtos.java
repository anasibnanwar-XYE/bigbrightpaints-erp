package com.bigbrightpaints.erp.modules.admin.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.bigbrightpaints.erp.modules.admin.domain.SupportTicketCategory;
import com.bigbrightpaints.erp.modules.admin.domain.SupportTicketPriority;
import com.bigbrightpaints.erp.modules.admin.domain.SupportTicketSlaStatus;
import com.bigbrightpaints.erp.modules.admin.domain.SupportTicketStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class SuperAdminSupportTicketDtos {

  private SuperAdminSupportTicketDtos() {}

  public record SlaSummary(
      String policyId,
      String supportTier,
      Instant firstResponseDueAt,
      Instant resolutionDueAt,
      Instant firstRespondedAt,
      Instant breachedAt,
      SupportTicketSlaStatus status) {}

  public record TimelineItem(
      Long id,
      UUID publicId,
      String eventType,
      String fromStatus,
      String toStatus,
      String fromCategory,
      String toCategory,
      String note,
      Long auditEventId,
      Instant createdAt) {}

  public record StatusUpdateRequest(
      @NotBlank @Size(max = 32) String status, @Size(max = 256) String reason) {}

  public record ConvertToIncidentRequest(@Size(max = 256) String reason) {}

  public record SlaRefreshRequest(Instant asOf) {}

  public record SlaRefreshResponse(
      int processedTickets,
      int breachedTickets,
      long totalBreachedTickets,
      List<Long> auditEventIds) {}

  public record QueueItem(
      Long ticketId,
      UUID publicId,
      Long companyId,
      String companyCode,
      String companyName,
      SupportTicketCategory category,
      SupportTicketPriority priority,
      SupportTicketStatus status,
      String requesterRole,
      String requesterEmail,
      Instant createdAt,
      Instant updatedAt,
      SlaSummary sla) {}

  public record Detail(
      Long ticketId,
      UUID publicId,
      Long companyId,
      String companyCode,
      String companyName,
      SupportTicketCategory category,
      SupportTicketPriority priority,
      SupportTicketStatus status,
      String subject,
      String description,
      String requesterRole,
      String requesterEmail,
      Instant createdAt,
      Instant updatedAt,
      SlaSummary sla,
      List<SupportTicketMessageResponse> messages,
      List<SupportTicketMessageResponse> internalNotes,
      Instant convertedToIncidentAt,
      List<TimelineItem> timeline) {}
}
