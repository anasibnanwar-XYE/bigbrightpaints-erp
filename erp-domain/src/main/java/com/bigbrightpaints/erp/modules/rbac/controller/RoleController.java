package com.bigbrightpaints.erp.modules.rbac.controller;

import com.bigbrightpaints.erp.core.security.PortalRoleActionMatrix;
import com.bigbrightpaints.erp.modules.rbac.dto.RoleDto;
import com.bigbrightpaints.erp.modules.rbac.service.RoleService;
import com.bigbrightpaints.erp.shared.dto.ApiResponse;
import java.util.List;
import java.util.Locale;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/roles")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @GetMapping
    @PreAuthorize(PortalRoleActionMatrix.ADMIN_ONLY)
    public ResponseEntity<ApiResponse<List<RoleDto>>> listRoles() {
        return ResponseEntity.ok(ApiResponse.success("Platform roles", roleService.listRolesForCurrentActor()));
    }

    @GetMapping("/{roleKey}")
    @PreAuthorize(PortalRoleActionMatrix.ADMIN_ONLY)
    public ResponseEntity<ApiResponse<RoleDto>> getRoleByKey(@org.springframework.web.bind.annotation.PathVariable String roleKey) {
        String normalized = roleKey == null ? "" : roleKey.trim().toUpperCase(Locale.ROOT);
        if (!normalized.startsWith("ROLE_")) {
            normalized = "ROLE_" + normalized;
        }
        String target = normalized;
        RoleDto match = roleService.listRolesForCurrentActor().stream()
                .filter(r -> r.name() != null && r.name().equalsIgnoreCase(target))
                .findFirst()
                .orElseThrow(() -> com.bigbrightpaints.erp.core.validation.ValidationUtils.invalidInput("Role not found: " + target));
        return ResponseEntity.ok(ApiResponse.success("Role " + target, match));
    }
}
