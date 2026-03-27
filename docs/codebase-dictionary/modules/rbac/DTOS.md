# RBAC DTOs

**Module Path:** `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/rbac/dto`

**Package Root:** `com.bigbrightpaints.erp.modules.rbac.dto`

## DTOs

### CreateRoleRequest
| Field | Type | Nullable | Description |
|-------|------|--------|---------------|-----------------|
| name | String | name | No | Role name (with ROLE_ prefix) |
| permissions | List<String> | permissions | No | List of permission codes to assign |

| description | String | description | No | Role description |
| label | String | label | No | Label for categorizing roles |

---

## RoleDto

**File**: `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/rbac/dto/RoleDto.java`

**Package**: `com.bigbrightpaints.erp.modules.rbac.dto`

```java
public record RoleDto(Long id, String name, String description, List<PermissionDto> permissions) {
    public RoleDto(Long id, String name, String name, String description, List<PermissionDto> permissions) {}
}
```
