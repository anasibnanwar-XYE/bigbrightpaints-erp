package com.bigbrightpaints.erp.modules.sales.controller;

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
import com.bigbrightpaints.erp.modules.admin.dto.SupportTicketCreateRequest;
import com.bigbrightpaints.erp.modules.admin.dto.SupportTicketListResponse;
import com.bigbrightpaints.erp.modules.admin.dto.SupportTicketMessageRequest;
import com.bigbrightpaints.erp.modules.admin.dto.SupportTicketMessageResponse;
import com.bigbrightpaints.erp.modules.admin.dto.SupportTicketResponse;
import com.bigbrightpaints.erp.modules.admin.service.DealerPortalSupportTicketService;
import com.bigbrightpaints.erp.shared.dto.ApiResponse;
import com.bigbrightpaints.erp.shared.dto.PageResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/dealer-portal/support/tickets")
@PreAuthorize(PortalRoleActionMatrix.DEALER_ONLY)
public class DealerPortalSupportTicketController {

  private final DealerPortalSupportTicketService dealerPortalSupportTicketService;

  public DealerPortalSupportTicketController(
      DealerPortalSupportTicketService dealerPortalSupportTicketService) {
    this.dealerPortalSupportTicketService = dealerPortalSupportTicketService;
  }

  @PostMapping
  public ResponseEntity<ApiResponse<SupportTicketResponse>> create(
      @Valid @RequestBody SupportTicketCreateRequest request) {
    return ResponseEntity.ok(
        ApiResponse.success(
            "Support ticket created", dealerPortalSupportTicketService.create(request)));
  }

  @GetMapping
  public ResponseEntity<ApiResponse<SupportTicketListResponse>> list() {
    return ResponseEntity.ok(
        ApiResponse.success(
            "Support tickets fetched",
            new SupportTicketListResponse(dealerPortalSupportTicketService.list())));
  }

  @GetMapping("/{ticketId}")
  public ResponseEntity<ApiResponse<SupportTicketResponse>> getById(@PathVariable Long ticketId) {
    return ResponseEntity.ok(
        ApiResponse.success(
            "Support ticket fetched", dealerPortalSupportTicketService.getById(ticketId)));
  }

  @PostMapping("/{ticketId}/messages")
  public ResponseEntity<ApiResponse<SupportTicketMessageResponse>> addMessage(
      @PathVariable Long ticketId, @Valid @RequestBody SupportTicketMessageRequest request) {
    return ResponseEntity.ok(
        ApiResponse.success(
            "Support ticket message created",
            dealerPortalSupportTicketService.addMessage(ticketId, request)));
  }

  @GetMapping("/{ticketId}/messages")
  public ResponseEntity<ApiResponse<PageResponse<SupportTicketMessageResponse>>> listMessages(
      @PathVariable Long ticketId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return ResponseEntity.ok(
        ApiResponse.success(
            "Support ticket messages fetched",
            dealerPortalSupportTicketService.listMessages(ticketId, page, size)));
  }
}
