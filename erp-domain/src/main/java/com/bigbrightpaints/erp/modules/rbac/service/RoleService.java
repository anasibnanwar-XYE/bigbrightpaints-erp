package com.bigbrightpaints.erp.modules.rbac.service;

import com.bigbrightpaints.erp.modules.rbac.domain.Permission;
import com.bigbrightpaints.erp.modules.rbac.domain.PermissionRepository;
import com.bigbrightpaints.erp.modules.rbac.domain.Role;
import com.bigbrightpaints.erp.modules.rbac.domain.RoleRepository;
import com.bigbrightpaints.erp.modules.rbac.dto.CreateRoleRequest;
import com.bigbrightpaints.erp.modules.rbac.dto.PermissionDto;
import com.bigbrightpaints.erp.modules.rbac.dto.RoleDto;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;

@Service
public class RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    public RoleService(RoleRepository roleRepository, PermissionRepository permissionRepository) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
    }

    public List<RoleDto> listRoles() {
        return roleRepository.findAll().stream().map(this::toDto).toList();
    }

    public List<RoleDto> listRolesByPrefix(String prefix) {
        return roleRepository.findByNameStartingWithIgnoreCase(prefix)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public RoleDto createRole(CreateRoleRequest request) {
        return persistRole(request, false);
    }

    @Transactional
    public RoleDto createDealerRole(CreateRoleRequest request) {
        return persistRole(request, true);
    }

    public Role ensureRoleExists(String roleName) {
        return roleRepository.findByName(roleName).orElseGet(() -> {
            Role role = new Role();
            role.setName(roleName);
            role.setDescription(roleName);
            return roleRepository.save(role);
        });
    }

    public Permission ensurePermissionExists(String code) {
        return permissionRepository.findByCode(code).orElseGet(() -> {
            Permission permission = new Permission();
            permission.setCode(code);
            permission.setDescription(code);
            return permissionRepository.save(permission);
        });
    }

    private RoleDto persistRole(CreateRoleRequest request, boolean enforceDealerPrefix) {
        String normalizedName = request.name().trim().toUpperCase(Locale.ROOT);
        if (enforceDealerPrefix && !normalizedName.startsWith("ROLE_DEALER_")) {
            throw new IllegalArgumentException("Dealer roles must start with ROLE_DEALER_");
        }

        Role role = roleRepository.findByName(normalizedName).orElseGet(Role::new);
        role.setName(normalizedName);
        role.setDescription(request.description().trim());

        var permissionCodes = request.permissions().stream()
                .map(code -> code.trim())
                .toList();
        var permissions = permissionRepository.findByCodeIn(permissionCodes);
        if (permissions.size() != permissionCodes.size()) {
            throw new IllegalArgumentException("One or more permission codes are invalid.");
        }

        role.getPermissions().clear();
        role.getPermissions().addAll(new HashSet<>(permissions));

        Role saved = roleRepository.save(role);
        return toDto(saved);
    }

    private RoleDto toDto(Role role) {
        List<PermissionDto> permissions = role.getPermissions().stream()
                .map(p -> new PermissionDto(p.getId(), p.getCode(), p.getDescription()))
                .toList();
        return new RoleDto(role.getId(), role.getName(), role.getDescription(), permissions);
    }
}
