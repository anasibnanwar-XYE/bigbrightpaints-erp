# Admin Controllers

## Overview

| Controller | Endpoints | Purpose |
|------------|-----------|---------|
| AdminSettingsController | 5 | System settings, approvals |
| AdminUserController | 8 | User management |
| ChangelogController | 2 | Changelog listing |
| SuperAdminChangelogController | 3 | Changelog management |
| SupportTicketController | 3 | Support tickets |

---

## AdminSettingsController

**File**: `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/admin/controller/AdminSettingsController.java`

**Package**: `com.bigbrightpaints.erp.modules.admin.controller`

**Base Path**: `/api/v1/admin`

**Dependencies**:
- SystemSettingsService
- EmailService
- CompanyContextService
- TenantRuntimePolicyService
- ExportApprovalService
- CreditRequestRepository
- CreditLimitOverrideRequestRepository
- PeriodCloseRequestRepository
- PayrollRunRepository
- AuditService
- ModuleGatingService

### Endpoints

| Method | Path | Signature | Description |
|--------|------|-----------|-------------|
| GET | `/settings` | `ApiResponse<SystemSettingsDto> getSettings()` | Get system settings |
| PUT | `/settings` | `ApiResponse<SystemSettingsDto> updateSettings(@Valid @RequestBody SystemSettingsUpdateRequest request)` | Update settings (SUPER_ADMIN only) |
| PUT | `/exports/{requestId}/approve` | `ApiResponse<ExportRequestDto> approveExportRequest(@PathVariable Long requestId)` | Approve export request |
| PUT | `/exports/{requestId}/reject` | `ApiResponse<ExportRequestDto> rejectExportRequest(@PathVariable Long requestId, @RequestBody(required=false) ExportRequestDecisionRequest request)` | Reject export request |
| POST | `/notify` | `ApiResponse<String> notifyUser(@Valid @RequestBody AdminNotifyRequest request)` | Send notification email |
| GET | `/approvals` | `ApiResponse<AdminApprovalsResponse> approvals()` | Get pending approvals |

---

## AdminUserController

**File**: `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/admin/controller/AdminUserController.java`

**Package**: `com.bigbrightpaints.erp.modules.admin.controller`

**Base Path**: `/api/v1/admin/users`

**Dependencies**:
- AdminUserService

### Endpoints

| Method | Path | Signature | Description |
|--------|------|-----------|-------------|
| GET | `/` | `ResponseEntity<ApiResponse<List<UserDto>>> list()` | List all users |
| POST | `/` | `ResponseEntity<ApiResponse<UserDto>> create(@Valid @RequestBody CreateUserRequest request)` | Create user |
| PUT | `/{id}` | `ResponseEntity<ApiResponse<UserDto>> update(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest request)` | Update user |
| POST | `/{userId}/force-reset-password` | `ResponseEntity<ApiResponse<String>> forceResetPassword(@PathVariable Long userId)` | Force password reset |
| PUT | `/{userId}/status` | `ResponseEntity<ApiResponse<UserDto>> updateStatus(@PathVariable Long userId, @Valid @RequestBody UpdateUserStatusRequest request)` | Update user status |
| PATCH | `/{id}/suspend` | `ResponseEntity<Void> suspend(@PathVariable Long id)` | Suspend user |
| PATCH | `/{id}/unsuspend` | `ResponseEntity<Void> unsuspend(@PathVariable Long id)` | Unsuspend user |
| PATCH | `/{id}/mfa/disable` | `ResponseEntity<Void> disableMfa(@PathVariable Long id)` | Disable MFA |
| DELETE | `/{id}` | `ResponseEntity<Void> delete(@PathVariable Long id)` | Delete user |

---

## ChangelogController

**File**: `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/admin/controller/ChangelogController.java`

**Package**: `com.bigbrightpaints.erp.modules.admin.controller`

**Base Path**: `/api/v1/changelog`

**Dependencies**:
- ChangelogService

### Endpoints

| Method | Path | Signature | Description |
|--------|------|-----------|-------------|
| GET | `/` | `ResponseEntity<ApiResponse<PageResponse<ChangelogEntryResponse>>> list(@RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="20") int size)` | List changelog entries |
| GET | `/latest-highlighted` | `ResponseEntity<ApiResponse<ChangelogEntryResponse>> latestHighlighted()` | Get latest highlighted entry |

---

## SuperAdminChangelogController

**File**: `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/admin/controller/SuperAdminChangelogController.java`

**Package**: `com.bigbrightpaints.erp.modules.admin.controller`

**Base Path**: `/api/v1/superadmin/changelog`

**Dependencies**:
- ChangelogService

### Endpoints

| Method | Path | Signature | Description |
|--------|------|-----------|-------------|
| POST | `/` | `ResponseEntity<ApiResponse<ChangelogEntryResponse>> create(@Valid @RequestBody ChangelogEntryRequest request)` | Create changelog entry |
| PUT | `/{id}` | `ResponseEntity<ApiResponse<ChangelogEntryResponse>> update(@PathVariable Long id, @Valid @RequestBody ChangelogEntryRequest request)` | Update changelog entry |
| DELETE | `/{id}` | `ResponseEntity<Void> delete(@PathVariable Long id)` | Soft delete entry |

---

## SupportTicketController

**File**: `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/admin/controller/SupportTicketController.java`

**Package**: `com.bigbrightpaints.erp.modules.admin.controller`

**Base Path**: `/api/v1/support/tickets`

**Dependencies**:
- SupportTicketService

### Endpoints

| Method | Path | Signature | Description |
|--------|------|-----------|-------------|
| POST | `/` | `ResponseEntity<ApiResponse<SupportTicketResponse>> create(@Valid @RequestBody SupportTicketCreateRequest request)` | Create support ticket |
| GET | `/` | `ResponseEntity<ApiResponse<SupportTicketListResponse>> list()` | List support tickets |
| GET | `/{ticketId}` | `ResponseEntity<ApiResponse<SupportTicketResponse>> getById(@PathVariable Long ticketId)` | Get ticket by ID |
