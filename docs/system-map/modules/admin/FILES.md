# ADMIN Module Map

`erp-domain/src/main/java/com/bigbrightpaints/erp/modules/admin` + related tests + SQL.

## Files
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/admin/controller/AdminSettingsController.java | REST API entrypoint for module endpoints
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/admin/controller/AdminUserController.java | REST API entrypoint for module endpoints
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/admin/dto/AdminApprovalItemDto.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/admin/dto/AdminApprovalsResponse.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/admin/dto/AdminNotifyRequest.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/admin/dto/CreateUserRequest.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/admin/dto/SystemSettingsDto.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/admin/dto/SystemSettingsUpdateRequest.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/admin/dto/UpdateUserRequest.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/admin/dto/UserDto.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/admin/service/AdminUserService.java | business service logic for module workflows
erp-domain/src/test/java/com/bigbrightpaints/erp/modules/admin/AdminApprovalRbacIT.java | module test coverage
erp-domain/src/test/java/com/bigbrightpaints/erp/modules/admin/AdminUserSecurityIT.java | module test coverage
erp-domain/src/test/java/com/bigbrightpaints/erp/modules/admin/controller/AdminSettingsControllerApprovalsContractTest.java | module test coverage

## Entrypoint files
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/admin/controller/AdminSettingsController.java | Admin settings and governance API
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/admin/controller/AdminUserController.java | User/approval control API

## High-risk files
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/admin/controller/AdminSettingsController.java | Sensitive operations for portal/runtime settings
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/admin/service/AdminUserService.java | User lifecycle and approval state
