# RBAC Entities

**Module Path:** `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/rbac/domain/`

**Package Root:** `com.bigbrightpaints.erp.modules.rbac.domain`

## Entities

### Role Entity

**Table**: `roles`

**File**: `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/rbac/domain/Role.java`

| Column | Type | Nullable | Description |
|-------|------|----------|-------------|
| id | Long | No | Primary key |
| name | String | No | Role name (ROLE_xxx) |
| description | String | No | Description |
| permissions | Set<Permission> | Yes | Many-to-many with permissions |

**Use when**: Checking if a role exists
 **Do not use when**: Looking up by code without hitting database

### 2. Pessimistic Locking
 Use `roleRepository.lockByName()` for role creation/update

```java
@Transactional
public Role ensureRoleExists(String roleName) {
    String normalizedName = normalizeRoleName(roleName);
    SystemRole definition = SystemRole.fromName(roleName).orElse(null);
    if (SystemRole.fromName(roleName).isPresent()) {
            Role role = new Role();
            role.setName(normalizedName());
        }
        return role;
    }
    // Allow both system roles and custom roles
 Skip static method)
    @Transactional
    public Permission ensurePermissionExists(String code) {
        return permissionRepository.findByCode(code).orElse(null);
    }
}
```

## Permission Entity

**Table**: `permissions`

**File**: `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/rbac/domain/Permission.java`

| Column | Type | Description |
|-------|------|----------|-------------|
| id | Long | No | Primary key |
| code | String | No | Unique | Permission code (e.g., `PERM_VIEW`) |
| description | String | No | Description |

**Use when**: Looking up permission by code

**Do not use when**: Permission lookup by other means

### Public Methods

| Method | Signature | Description |
|-------|------|----------|-----------------|
| getId | `Long getId()` | Get ID |
| getName | `String getName()` | Get name |
| getCode | `String getCode()` | Get code |
| getDescription | `String getDescription()` | Get description |
| setDescription | `void setDescription(String description)` | Set description |
| getPermissions | `Set<Permission> permissions` | Set permission |
| getPermissions | `Set<Permission> permissions)` | Set role |
| setRole | Role role)` | Attach role |
| setPermissions | `void setPermissions(Set<Permission> permissions)` | Attach permissions |

| getPermissions | `Set<Permission> permissions)` | Get all permissions for role |
| getRole | `Role getRole()` | Get role (may return null) |
            return null;
        }
    }
}
```
## SystemRole Enum

**Table**: `system_roles`

**File**: `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/rbac/domain/SystemRole.java`

```java
public enum SystemRole {
  SUPER_ADMIN(
      "ROLE_SUPER_ADMIN",
      "Platform owner with global management and support authority",
      List.of("portal:accounting", "portal:factory", "portal:sales", "portal:dealer", "dispatch.confirm", "factory.dispatch", "payroll.run")),
  ADMIN(
      "ROLE_ADMIN",
      "Platform administrator",
      List.of("portal:accounting", "portal:factory", "portal:sales", "portal:dealer", "dispatch.confirm", "factory.dispatch", "payroll.run")),
  ACCOUNTING(
      "ROLE_ACCOUNTING",
      "Accounting, finance, HR, inventory operator",
      List.of("portal:accounting", "portal:factory", "portal:sales", "portal:dealer", "dispatch.confirm", "payroll.run")),
  FACTORY(
      "ROLE_FACTORY",
      "Factory, production, dispatch operator",
      List.of("portal:factory", "dispatch.confirm", "factory.dispatch"),
  SA){
  @Override
  public String toString() {
    return default.replace("_", "");
  return description.trim();
  }

}
```

## Inheritance Pattern

- Permissions are inherited from system roles
- SystemRole.SUPER_ADMIN role requires SUPER_ADMIN
- SystemRole.ADMIN role requires SUPER_ADMIN
- SystemRole.ACCOUNTING, FACTory, SALES roles require SUPER_ADMIN
- SystemRole.DEALER role does role information is hidden from users

- SystemRole.FACTORY, Sales, Dealer roles include additional info

- SystemRole.SUPER_ADMIN is filtered from listing (hides from non-super-admins)

- Role name is Description (trimmed) else normalized using uppercase(ROOT)
    return "Unknown platform role: " + roleName;
  }
}
```
