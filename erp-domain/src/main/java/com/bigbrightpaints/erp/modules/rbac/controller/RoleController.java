package com.bigbrightpaints.erp.modules.rbac.controller;

import com.bigbrightpaints.erp.modules.rbac.dto.CreateRoleRequest;
import com.bigbrightpaints.erp.modules.rbac.dto.RoleDto;
import com.bigbrightpaints.erp.modules.rbac.service.RoleService;
import com.bigbrightpaints.erp.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/roles")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_FACTORY_MANAGER','ROLE_SALES_MANAGER')")
    public ResponseEntity<ApiResponse<List<RoleDto>>> listRoles() {
        return ResponseEntity.ok(ApiResponse.success(roleService.listRoles()));
    }

    @GetMapping("/factory")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_FACTORY_MANAGER')")
    public ResponseEntity<ApiResponse<List<RoleDto>>> factoryRoles() {
        return ResponseEntity.ok(ApiResponse.success("Factory roles", roleService.listRolesByPrefix("ROLE_FACTORY")));
    }

    @GetMapping("/sales")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_SALES_MANAGER')")
    public ResponseEntity<ApiResponse<List<RoleDto>>> salesRoles() {
        return ResponseEntity.ok(ApiResponse.success("Sales roles", roleService.listRolesByPrefix("ROLE_SALES")));
    }

    @GetMapping("/dealer")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_SALES_MANAGER')")
    public ResponseEntity<ApiResponse<List<RoleDto>>> dealerRoles() {
        return ResponseEntity.ok(ApiResponse.success("Dealer roles", roleService.listRolesByPrefix("ROLE_DEALER")));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<RoleDto>> createRole(@Valid @RequestBody CreateRoleRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Role created", roleService.createRole(request)));
    }

    @PostMapping("/dealer")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_SALES_MANAGER')")
    public ResponseEntity<ApiResponse<RoleDto>> createDealerRole(@Valid @RequestBody CreateRoleRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Dealer role created", roleService.createDealerRole(request)));
    }
}
