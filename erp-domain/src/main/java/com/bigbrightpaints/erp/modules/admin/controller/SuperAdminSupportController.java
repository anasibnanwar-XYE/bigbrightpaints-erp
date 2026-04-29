package com.bigbrightpaints.erp.modules.admin.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bigbrightpaints.erp.core.security.PortalRoleActionMatrix;
import com.bigbrightpaints.erp.modules.admin.dto.SuperAdminSupportTicketDtos;
import com.bigbrightpaints.erp.modules.admin.dto.SupportTicketMessageRequest;
import com.bigbrightpaints.erp.modules.admin.dto.SupportTicketMessageResponse;
import com.bigbrightpaints.erp.modules.admin.service.SuperAdminSupportService;
import com.bigbrightpaints.erp.shared.dto.ApiResponse;
import com.bigbrightpaints.erp.shared.dto.PageResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/superadmin/support/tickets")
@PreAuthorize(PortalRoleActionMatrix.SUPER_ADMIN_ONLY)
public class SuperAdminSupportController {

  private final SuperAdminSupportService superAdminSupportService;

  public SuperAdminSupportController(SuperAdminSupportService superAdminSupportService) {
    this.superAdminSupportService = superAdminSupportService;
  }

  @GetMapping
  public ResponseEntity<ApiResponse<PageResponse<SuperAdminSupportTicketDtos.QueueItem>>> listQueue(
      @RequestParam(required = false) String status,
      @RequestParam(required = false) String category,
      @RequestParam(required = false) String slaStatus,
      @RequestParam(required = false, name = "q") String query,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(defaultValue = "createdAt,desc") String sort) {
    return ResponseEntity.ok(
        ApiResponse.success(
            "Super Admin support queue fetched",
            superAdminSupportService.listQueue(
                status, category, slaStatus, query, page, size, sort)));
  }

  @PostMapping("/sla/refresh")
  public ResponseEntity<ApiResponse<SuperAdminSupportTicketDtos.SlaRefreshResponse>>
      refreshSlaBreaches(
          @RequestBody(required = false) SuperAdminSupportTicketDtos.SlaRefreshRequest request) {
    return ResponseEntity.ok(
        ApiResponse.success(
            "Super Admin support SLA breaches refreshed",
            superAdminSupportService.refreshSlaBreaches(request)));
  }

  @GetMapping("/{ticketId}")
  public ResponseEntity<ApiResponse<SuperAdminSupportTicketDtos.Detail>> getDetail(
      @PathVariable Long ticketId) {
    return ResponseEntity.ok(
        ApiResponse.success(
            "Super Admin support ticket fetched", superAdminSupportService.getDetail(ticketId)));
  }

  @PostMapping("/{ticketId}/messages")
  public ResponseEntity<ApiResponse<SupportTicketMessageResponse>> addMessage(
      @PathVariable Long ticketId, @Valid @RequestBody SupportTicketMessageRequest request) {
    return ResponseEntity.ok(
        ApiResponse.success(
            "Super Admin support ticket message created",
            superAdminSupportService.addMessage(ticketId, request)));
  }

  @GetMapping("/{ticketId}/messages")
  public ResponseEntity<ApiResponse<PageResponse<SupportTicketMessageResponse>>> listMessages(
      @PathVariable Long ticketId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(defaultValue = "false") boolean includeInternal) {
    return ResponseEntity.ok(
        ApiResponse.success(
            "Super Admin support ticket messages fetched",
            superAdminSupportService.listMessages(ticketId, page, size, includeInternal)));
  }

  @PostMapping("/{ticketId}/status")
  public ResponseEntity<ApiResponse<SuperAdminSupportTicketDtos.Detail>> updateStatus(
      @PathVariable Long ticketId,
      @Valid @RequestBody SuperAdminSupportTicketDtos.StatusUpdateRequest request) {
    return ResponseEntity.ok(
        ApiResponse.success(
            "Super Admin support ticket status updated",
            superAdminSupportService.updateStatus(ticketId, request)));
  }

  @PostMapping("/{ticketId}/convert-to-incident")
  public ResponseEntity<ApiResponse<SuperAdminSupportTicketDtos.Detail>> convertToIncident(
      @PathVariable Long ticketId,
      @Valid @RequestBody(required = false)
          SuperAdminSupportTicketDtos.ConvertToIncidentRequest request) {
    return ResponseEntity.ok(
        ApiResponse.success(
            "Super Admin feature request converted to incident",
            superAdminSupportService.convertFeatureRequestToIncident(ticketId, request)));
  }

  @GetMapping("/{ticketId}/timeline")
  public ResponseEntity<ApiResponse<java.util.List<SuperAdminSupportTicketDtos.TimelineItem>>>
      timeline(@PathVariable Long ticketId) {
    return ResponseEntity.ok(
        ApiResponse.success(
            "Super Admin support ticket timeline fetched",
            superAdminSupportService.timeline(ticketId)));
  }

  @PostMapping("/{ticketId}/internal-notes")
  public ResponseEntity<ApiResponse<SupportTicketMessageResponse>> addInternalNote(
      @PathVariable Long ticketId, @Valid @RequestBody SupportTicketMessageRequest request) {
    return ResponseEntity.ok(
        ApiResponse.success(
            "Super Admin support internal note created",
            superAdminSupportService.addInternalNote(ticketId, request)));
  }
}
