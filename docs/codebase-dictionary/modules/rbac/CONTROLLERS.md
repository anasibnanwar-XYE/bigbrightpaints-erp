# RBAC Controllers

## Overview

| Controller | Endpoints | Purpose |
|------------|-----------|---------|
| RoleController | 3 | Role management |

---

## RoleController

**File**: `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/rbac/controller/RoleController.java`

**Package**: `com.bigbrightpaints.erp.modules.rbac.controller`

**Base Path**: `/api/v1/admin/roles`

**Dependencies**:
- RoleService

### Endpoints

| Method | Path | Signature | Description |
|--------|------|-----------|-------------|
| GET | `/` | `ResponseEntity<ApiResponse<List<RoleDto>>> listRoles()` | List roles (SUPER_ADMIN sees all, others see non-SUPER_ADMIN) |
| GET | `/{roleKey}` | `ResponseEntity<ApiResponse<RoleDto>> getRoleByKey(@PathVariable String roleKey)` | Get role by key (auto-prefixes ROLE_) |
| POST | `/` | `ResponseEntity<ApiResponse<RoleDto>> createRole(@Valid @RequestBody CreateRoleRequest request)` | Create/update role |
