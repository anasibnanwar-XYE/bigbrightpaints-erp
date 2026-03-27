# RBAC Module Overview

**Module Path:** `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/rbac/`  
**Package Root:** `com.bigbrightpaints.erp.modules.rbac`

## Purpose

The RBAC (Role-Based Access Control) module provides:
- Role and permission management
- System role definitions with default permissions
- Role synchronization for consistency
- Permission validation for role assignments

## Module Boundaries

### Inbound Dependencies
- **Auth** (`modules/auth`) - User role assignments
- **Company** (`core/security`) - Company context for authorization

### Outbound Dependencies
- **Audit Service** (`core/audit`) - Authorization decisions logged

## Architecture Layers

```
modules/rbac/
├── config/       # Configuration (1 class)
├── controller/   # REST endpoints (1 controller)
├── domain/       # Entities & enums (4 classes)
├── dto/          # Data transfer objects (3 classes)
└── service/      # Business logic (1 service)
```

## System Roles

| Role | Description | Permissions |
|------|-------------|-------------|
| ROLE_SUPER_ADMIN | Platform owner with global management | portal:accounting, portal:factory, portal:sales, portal:dealer, dispatch.confirm, factory.dispatch, payroll.run |
| ROLE_ADMIN | Platform administrator | portal:accounting, portal:factory, portal:sales, portal:dealer, dispatch.confirm, factory.dispatch, payroll.run |
| ROLE_ACCOUNTING | Accounting, finance, HR, inventory | portal:accounting, dispatch.confirm, payroll.run |
| ROLE_FACTORY | Factory, production, dispatch | portal:factory, dispatch.confirm, factory.dispatch |
| ROLE_SALES | Sales operations and dealer management | portal:sales |
| ROLE_DEALER | Dealer workspace user | portal:dealer |

## Key Design Patterns

### 1. System Role Synchronization
- `SystemRole` enum defines canonical roles and permissions
- `RoleService.synchronizeSystemRoles()` syncs database with definitions
- Retired permissions automatically removed from roles

### 2. Permission Inheritance
- Permissions defined at role level
- Users inherit all permissions from assigned roles
- SUPER_ADMIN required for ADMIN/SUPER_ADMIN role assignment

### 3. Scoped Role Management
- Non-SUPER_ADMIN users cannot assign privileged roles
- All role assignment attempts are audited

## Anti-Patterns to Avoid

### 1. Direct Role Assignment
❌ **Wrong:** Creating roles outside of `RoleService`
✅ **Correct:** Use `RoleService.ensureRoleExists()` for consistent role management

### 2. Privileged Role Assignment Without Audit
❌ **Wrong:** Direct database role assignment
✅ **Correct:** Go through `RoleService` which audits all assignments

## Canonicality Status

| Component | Status | Notes |
|-----------|--------|-------|
| `RoleService` | ✅ Canonical | Single entry point for role management |
| `SystemRole` | ✅ Canonical | System role definitions |
| `Role` entity | ✅ Canonical | Role persistence |
| `Permission` entity | ✅ Canonical | Permission persistence |

## Security Requirements

- **SUPER_ADMIN Only:** Assigning ROLE_ADMIN or ROLE_SUPER_ADMIN
- **Audit:** All privilege escalation decisions logged
- **Tenant Scope:** Authorization decisions include tenant context
