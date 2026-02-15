# RBAC Module Map

`erp-domain/src/main/java/com/bigbrightpaints/erp/modules/rbac` + related tests + SQL.

## Files
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/rbac/controller/RoleController.java | REST API entrypoint for module endpoints
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/rbac/domain/Permission.java | domain entity for module persistence state
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/rbac/domain/PermissionRepository.java | JPA repository for Permission persistence access
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/rbac/domain/Role.java | domain entity for module persistence state
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/rbac/domain/RoleRepository.java | JPA repository for Role persistence access
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/rbac/domain/SystemRole.java | domain entity for module persistence state
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/rbac/dto/CreateRoleRequest.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/rbac/dto/PermissionDto.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/rbac/dto/RoleDto.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/rbac/service/RoleService.java | business service logic for module workflows
erp-domain/src/main/resources/db/migration_v2/V1__core_auth_rbac.sql | Flyway schema migration file
erp-domain/src/test/java/com/bigbrightpaints/erp/modules/rbac/domain/SystemRoleTest.java | module test coverage

## Entrypoint files
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/rbac/controller/RoleController.java | Role and permission API

## High-risk files
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/rbac/service/RoleService.java | Role assignment and RBAC enforcement
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/rbac/domain/PermissionRepository.java | Permission lookup boundaries
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/rbac/domain/RoleRepository.java | Role persistence integrity
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/rbac/domain/SystemRole.java | Built-in/system role model
