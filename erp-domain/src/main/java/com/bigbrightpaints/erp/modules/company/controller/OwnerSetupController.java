package com.bigbrightpaints.erp.modules.company.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bigbrightpaints.erp.core.security.PortalRoleActionMatrix;
import com.bigbrightpaints.erp.modules.company.dto.OwnerSetupAccountingRequest;
import com.bigbrightpaints.erp.modules.company.dto.OwnerSetupCompanyDetailsRequest;
import com.bigbrightpaints.erp.modules.company.dto.OwnerSetupGstRequest;
import com.bigbrightpaints.erp.modules.company.dto.OwnerSetupInviteTeamRequest;
import com.bigbrightpaints.erp.modules.company.dto.OwnerSetupStatusResponse;
import com.bigbrightpaints.erp.modules.company.service.OwnerSetupService;
import com.bigbrightpaints.erp.shared.dto.ApiResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/setup")
@PreAuthorize(PortalRoleActionMatrix.TENANT_ADMIN_ONLY)
public class OwnerSetupController {

  private final OwnerSetupService ownerSetupService;

  public OwnerSetupController(OwnerSetupService ownerSetupService) {
    this.ownerSetupService = ownerSetupService;
  }

  @GetMapping("/status")
  public ResponseEntity<ApiResponse<OwnerSetupStatusResponse>> status() {
    return ResponseEntity.ok(ApiResponse.success(ownerSetupService.getStatus()));
  }

  @PutMapping("/company-details")
  public ResponseEntity<ApiResponse<OwnerSetupStatusResponse>> companyDetails(
      @Valid @RequestBody OwnerSetupCompanyDetailsRequest request) {
    return ResponseEntity.ok(
        ApiResponse.success(
            "Company details saved", ownerSetupService.completeCompanyDetails(request)));
  }

  @PutMapping("/gst")
  public ResponseEntity<ApiResponse<OwnerSetupStatusResponse>> gst(
      @Valid @RequestBody OwnerSetupGstRequest request) {
    return ResponseEntity.ok(
        ApiResponse.success("GST setup saved", ownerSetupService.completeGst(request)));
  }

  @PutMapping("/accounting")
  public ResponseEntity<ApiResponse<OwnerSetupStatusResponse>> accounting(
      @Valid @RequestBody OwnerSetupAccountingRequest request) {
    return ResponseEntity.ok(
        ApiResponse.success(
            "Accounting setup saved", ownerSetupService.completeAccounting(request)));
  }

  @PostMapping("/invite-team")
  public ResponseEntity<ApiResponse<OwnerSetupStatusResponse>> inviteTeam(
      @Valid @RequestBody OwnerSetupInviteTeamRequest request) {
    return ResponseEntity.ok(
        ApiResponse.success("Invite team step saved", ownerSetupService.inviteTeam(request)));
  }

  @PostMapping("/finish")
  public ResponseEntity<ApiResponse<OwnerSetupStatusResponse>> finish() {
    return ResponseEntity.ok(ApiResponse.success("Setup finished", ownerSetupService.finish()));
  }
}
