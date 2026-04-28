package com.bigbrightpaints.erp.modules.admin.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bigbrightpaints.erp.core.security.PortalRoleActionMatrix;
import com.bigbrightpaints.erp.modules.admin.dto.SuperAdminPasswordChangeResponseDto;
import com.bigbrightpaints.erp.modules.admin.dto.SuperAdminProfileDto;
import com.bigbrightpaints.erp.modules.admin.dto.SuperAdminProfileSessionDto;
import com.bigbrightpaints.erp.modules.admin.dto.SuperAdminProfileUpdateRequest;
import com.bigbrightpaints.erp.modules.admin.dto.SuperAdminSessionRevokeResponseDto;
import com.bigbrightpaints.erp.modules.admin.service.SuperAdminProfileService;
import com.bigbrightpaints.erp.modules.auth.domain.UserPrincipal;
import com.bigbrightpaints.erp.modules.auth.web.ChangePasswordRequest;
import com.bigbrightpaints.erp.shared.dto.ApiResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/superadmin/profile")
@PreAuthorize(PortalRoleActionMatrix.SUPER_ADMIN_ONLY)
public class SuperAdminProfileController {

  private final SuperAdminProfileService profileService;

  public SuperAdminProfileController(SuperAdminProfileService profileService) {
    this.profileService = profileService;
  }

  @GetMapping
  public ResponseEntity<ApiResponse<SuperAdminProfileDto>> getProfile(
      @AuthenticationPrincipal UserPrincipal principal) {
    return ResponseEntity.ok(
        ApiResponse.success("Super Admin profile fetched", profileService.profile(principal)));
  }

  @PutMapping
  public ResponseEntity<ApiResponse<SuperAdminProfileDto>> updateProfile(
      @AuthenticationPrincipal UserPrincipal principal,
      @Valid @RequestBody SuperAdminProfileUpdateRequest request) {
    return ResponseEntity.ok(
        ApiResponse.success(
            "Super Admin profile updated", profileService.updateProfile(principal, request)));
  }

  @PostMapping("/password")
  public ResponseEntity<ApiResponse<SuperAdminPasswordChangeResponseDto>> changePassword(
      @AuthenticationPrincipal UserPrincipal principal,
      @Valid @RequestBody ChangePasswordRequest request) {
    return ResponseEntity.ok(
        ApiResponse.success(
            "Super Admin password changed", profileService.changePassword(principal, request)));
  }

  @GetMapping("/sessions")
  public ResponseEntity<ApiResponse<List<SuperAdminProfileSessionDto>>> sessions(
      @AuthenticationPrincipal UserPrincipal principal) {
    return ResponseEntity.ok(
        ApiResponse.success("Super Admin sessions fetched", profileService.sessions(principal)));
  }

  @PostMapping("/sessions/{sessionId}/revoke")
  public ResponseEntity<ApiResponse<SuperAdminSessionRevokeResponseDto>> revokeSession(
      @AuthenticationPrincipal UserPrincipal principal, @PathVariable String sessionId) {
    return ResponseEntity.ok(
        ApiResponse.success(
            "Super Admin session revoked", profileService.revokeSession(principal, sessionId)));
  }
}
