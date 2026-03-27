# RBAC Services

## Overview

| Service | Purpose |
|---------|---------|
| RoleService | Role and permission management |

---

## RoleService

**File**: `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/rbac/service/RoleService.java`

**Package**: `com.bigbrightpaints.erp.modules.rbac.service`

**Responsibility**: Centralized role management with permission synchronization and access control

**Use when**: Managing roles, checking permissions, synchronizing system roles

**Do not use when**: Direct database access is needed for bulk operations

### Dependencies
- RoleRepository
- PermissionRepository
- AuditService

### Public Methods

| Method | Signature | Description |
|--------|-----------|-------------|
| listRoles | `List<RoleDto> listRoles()` | List all system roles |
| listRolesForCurrentActor | `List<RoleDto> listRolesForCurrentActor()` | List roles for current user (filters SUPER_ADMIN for non-super-admins) |
| synchronizeSystemRolePermissions | `int synchronizeSystemRolePermissions()` | Sync permissions for existing roles |
| createRole | `RoleDto createRole(CreateRoleRequest request)` | Create/update role with permissions |
| canManageSharedRoleMutation | `boolean canManageSharedRoleMutation(String roleName)` | Check if user can mutate role |
| ensureRoleExists | `Role ensureRoleExists(String roleName)` | Ensure role exists (with lock) |
| ensurePermissionExists | `Permission ensurePermissionExists(String code)` | Ensure permission exists |
| isSystemRole | `boolean isSystemRole(String roleName)` | Check if role is a system role |
| synchronizeSystemRoles | `int synchronizeSystemRoles()` | Sync all system roles |

### Side Effects
- DB writes: Role and permission creation/updates
- Events: Audit events for authorization decisions

### Status
✅ **Canonical** - Single entry point for role management

### Key Behaviors

1. **Role Normalization**: All role names are normalized to uppercase with ROLE_ prefix
2. **Pessimistic Locking**: `lockByName()` prevents concurrent role creation
3. **Permission Sync**: System roles automatically sync their default permissions
4. **Access Control**: SUPER_ADMIN authority required for ADMIN/SUPER_ADMIN role management
