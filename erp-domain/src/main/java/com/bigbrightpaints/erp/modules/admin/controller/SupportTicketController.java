package com.bigbrightpaints.erp.modules.admin.controller;

import com.bigbrightpaints.erp.modules.admin.dto.SupportTicketCreateRequest;
import com.bigbrightpaints.erp.modules.admin.dto.SupportTicketListResponse;
import com.bigbrightpaints.erp.modules.admin.dto.SupportTicketResponse;
import com.bigbrightpaints.erp.modules.admin.service.SupportTicketService;
import com.bigbrightpaints.erp.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/support/tickets")
public class SupportTicketController {

    private final SupportTicketService supportTicketService;

    public SupportTicketController(SupportTicketService supportTicketService) {
        this.supportTicketService = supportTicketService;
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<SupportTicketResponse>> create(
            @Valid @RequestBody SupportTicketCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Support ticket created",
                supportTicketService.create(request)));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<SupportTicketListResponse>> list() {
        return ResponseEntity.ok(ApiResponse.success(
                "Support tickets fetched",
                new SupportTicketListResponse(supportTicketService.list())));
    }

    @GetMapping("/{ticketId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<SupportTicketResponse>> getById(@PathVariable Long ticketId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Support ticket fetched",
                supportTicketService.getById(ticketId)));
    }
}
