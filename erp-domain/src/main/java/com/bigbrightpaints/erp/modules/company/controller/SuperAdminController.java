package com.bigbrightpaints.erp.modules.company.controller;

import com.bigbrightpaints.erp.modules.company.dto.SuperAdminDashboardDto;
import com.bigbrightpaints.erp.modules.company.dto.SuperAdminTenantDto;
import com.bigbrightpaints.erp.modules.company.dto.SuperAdminTenantUsageDto;
import com.bigbrightpaints.erp.modules.company.service.SuperAdminService;
import com.bigbrightpaints.erp.shared.dto.ApiResponse;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/superadmin")
@PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
public class SuperAdminController {

    private final SuperAdminService superAdminService;

    public SuperAdminController(SuperAdminService superAdminService) {
        this.superAdminService = superAdminService;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<SuperAdminDashboardDto>> dashboard() {
        return ResponseEntity.ok(ApiResponse.success(
                "Superadmin dashboard fetched",
                superAdminService.getDashboard()));
    }

    @GetMapping("/tenants")
    public ResponseEntity<ApiResponse<List<SuperAdminTenantDto>>> listTenants(
            @RequestParam(value = "status", required = false) String status) {
        return ResponseEntity.ok(ApiResponse.success(
                "Superadmin tenant list fetched",
                superAdminService.listTenants(status)));
    }

    @PostMapping("/tenants/{id}/suspend")
    public ResponseEntity<ApiResponse<SuperAdminTenantDto>> suspendTenant(@PathVariable("id") Long tenantId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Tenant suspended",
                superAdminService.suspendTenant(tenantId)));
    }

    @PostMapping("/tenants/{id}/activate")
    public ResponseEntity<ApiResponse<SuperAdminTenantDto>> activateTenant(@PathVariable("id") Long tenantId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Tenant activated",
                superAdminService.activateTenant(tenantId)));
    }

    @GetMapping("/tenants/{id}/usage")
    public ResponseEntity<ApiResponse<SuperAdminTenantUsageDto>> tenantUsage(@PathVariable("id") Long tenantId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Tenant usage fetched",
                superAdminService.getTenantUsage(tenantId)));
    }
}
