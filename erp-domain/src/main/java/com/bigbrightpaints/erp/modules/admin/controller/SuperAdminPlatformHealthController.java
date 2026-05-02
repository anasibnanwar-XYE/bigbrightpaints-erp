package com.bigbrightpaints.erp.modules.admin.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bigbrightpaints.erp.core.security.PortalRoleActionMatrix;
import com.bigbrightpaints.erp.modules.admin.dto.SuperAdminPlatformHealthDto;
import com.bigbrightpaints.erp.modules.admin.service.SuperAdminPlatformHealthService;
import com.bigbrightpaints.erp.shared.dto.ApiResponse;

@RestController
@RequestMapping("/api/v1/superadmin/infra/health")
@PreAuthorize(PortalRoleActionMatrix.SUPER_ADMIN_ONLY)
public class SuperAdminPlatformHealthController {

  private final SuperAdminPlatformHealthService platformHealthService;

  public SuperAdminPlatformHealthController(SuperAdminPlatformHealthService platformHealthService) {
    this.platformHealthService = platformHealthService;
  }

  @GetMapping
  public ResponseEntity<ApiResponse<SuperAdminPlatformHealthDto>> getPlatformHealth() {
    return ResponseEntity.ok(
        ApiResponse.success(
            "Super Admin platform health fetched", platformHealthService.currentHealth()));
  }
}
