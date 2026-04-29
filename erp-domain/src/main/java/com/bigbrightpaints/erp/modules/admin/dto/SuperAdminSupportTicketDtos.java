package com.bigbrightpaints.erp.modules.admin.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.bigbrightpaints.erp.modules.admin.domain.SupportTicketCategory;
import com.bigbrightpaints.erp.modules.admin.domain.SupportTicketPriority;
import com.bigbrightpaints.erp.modules.admin.domain.SupportTicketStatus;

public final class SuperAdminSupportTicketDtos {

  private SuperAdminSupportTicketDtos() {}

  public record SlaSummary(
      String policyId,
      String supportTier,
      Instant firstResponseDueAt,
      Instant resolutionDueAt,
      String status) {}

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
      List<SupportTicketMessageResponse> internalNotes) {}
}
