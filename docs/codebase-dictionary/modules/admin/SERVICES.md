# Admin Services

## Overview

| Service | Purpose |
|---------|---------|
| AdminUserService | User management operations |
| ChangelogService | Changelog entry management |
| ExportApprovalService | Export request approval workflow |
| GitHubIssueClient | GitHub API integration |
| SupportTicketGitHubSyncService | Support ticket GitHub sync |
| SupportTicketService | Support ticket management |
| TenantRuntimePolicyService | Tenant quota enforcement |

---

## AdminUserService

**File**: `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/admin/service/AdminUserService.java`

**Package**: `com.bigbrightpaints.erp.modules.admin.service`

**Responsibility**: Centralized user management with company-scoped access control and audit logging

**Use when**: Creating, updating, or managing user accounts within tenant scope

**Do not use when**: Direct database access is needed (use repository directly for internal operations)

### Dependencies
- UserAccountRepository
- CompanyContextService
- CompanyRepository
- RoleService
- PasswordEncoder
- EmailService
- TokenBlacklistService
- RefreshTokenService
- PasswordResetService
- AuditService
- AuditLogRepository
- DealerRepository
- AccountRepository
- TenantRuntimePolicyService

### Public Methods

| Method | Signature | Description |
|--------|-----------|-------------|
| listUsers | `List<UserDto> listUsers()` | List users for current company |
| createUser | `UserDto createUser(CreateUserRequest request)` | Create new user with company assignment |
| updateUser | `UserDto updateUser(Long id, UpdateUserRequest request)` | Update user details |
| forceResetPassword | `void forceResetPassword(Long userId)` | Force password reset by admin |
| updateUserStatus | `UserDto updateUserStatus(Long userId, boolean enabled)` | Enable/disable user |
| suspend | `void suspend(Long id)` | Suspend user |
| unsuspend | `void unsuspend(Long id)` | Unsuspend user |
| deleteUser | `void deleteUser(Long id)` | Delete user |
| disableMfa | `void disableMfa(Long id)` | Disable MFA for user |

### Side Effects
- DB writes: User creation, updates, deletions
- Events: Audit events for all user operations
- External: Email notifications for credentials, suspension

### Status
✅ **Canonical** - Primary user management service

---

## ChangelogService

**File**: `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/admin/service/ChangelogService.java`

**Package**: `com.bigbrightpaints.erp.modules.admin.service`

**Responsibility**: Manage system changelog entries for version announcements

### Dependencies
- ChangelogEntryRepository
- AuditService

### Public Methods

| Method | Signature | Description |
|--------|-----------|-------------|
| create | `ChangelogEntryResponse create(ChangelogEntryRequest request)` | Create changelog entry |
| update | `ChangelogEntryResponse update(Long id, ChangelogEntryRequest request)` | Update changelog entry |
| softDelete | `void softDelete(Long id)` | Soft delete entry |
| list | `PageResponse<ChangelogEntryResponse> list(int page, int size)` | Paginated list |
| latestHighlighted | `ChangelogEntryResponse latestHighlighted()` | Get highlighted entry |

### Status
✅ **Canonical** - Single source for changelog management

---

## ExportApprovalService

**File**: `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/admin/service/ExportApprovalService.java`

**Package**: `com.bigbrightpaints.erp.modules.admin.service`

**Responsibility**: Export request approval workflow for sensitive data

### Dependencies
- CompanyContextService
- UserAccountRepository
- ExportRequestRepository
- SystemSettingsService

### Public Methods

| Method | Signature | Description |
|--------|-----------|-------------|
| isApprovalRequired | `boolean isApprovalRequired()` | Check if approval workflow enabled |
| createRequest | `ExportRequestDto createRequest(ExportRequestCreateRequest request)` | Create export request |
| listPending | `List<ExportRequestDto> listPending()` | List pending requests |
| approve | `ExportRequestDto approve(Long requestId)` | Approve request |
| reject | `ExportRequestDto reject(Long requestId, String reason)` | Reject request |
| resolveDownload | `ExportRequestDownloadResponse resolveDownload(Long requestId)` | Resolve download |

### Status
✅ **Canonical** - Export approval workflow

---

## GitHubIssueClient

**File**: `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/admin/service/GitHubIssueClient.java`

**Package**: `com.bigbrightpaints.erp.modules.admin.service`

**Responsibility**: GitHub API client for issue creation and status tracking

### Dependencies
- GitHubProperties
- RestTemplateBuilder
- ObjectMapper

### Public Methods

| Method | Signature | Description |
|--------|-----------|-------------|
| isEnabledAndConfigured | `boolean isEnabledAndConfigured()` | Check if GitHub integration enabled |
| createIssue | `GitHubIssueCreateResult createIssue(String title, String body, List<String> labels)` | Create GitHub issue |
| fetchIssueState | `GitHubIssueStateResult fetchIssueState(long issueNumber)` | Fetch issue state |

### Records

```java
record GitHubIssueCreateResult(long issueNumber, String issueUrl, String issueState, Instant syncedAt) {}
record GitHubIssueStateResult(long issueNumber, String issueUrl, String issueState, Instant syncedAt) {}
```

### Status
✅ **Canonical** - GitHub API integration

---

## SupportTicketGitHubSyncService

**File**: `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/admin/service/SupportTicketGitHubSyncService.java`

**Package**: `com.bigbrightpaints.erp.modules.admin.service`

**Responsibility**: Synchronize support tickets with GitHub issues

### Dependencies
- SupportTicketRepository
- UserAccountRepository
- GitHubIssueClient
- EmailService

### Public Methods

| Method | Signature | Description |
|--------|-----------|-------------|
| submitGitHubIssueAsync | `void submitGitHubIssueAsync(Long ticketId)` | Async GitHub issue creation |
| syncGitHubIssueStatuses | `void syncGitHubIssueStatuses()` | Scheduled sync (every 5 min) |

### Status
✅ **Canonical** - Support ticket GitHub sync

---

## SupportTicketService

**File**: `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/admin/service/SupportTicketService.java`

**Package**: `com.bigbrightpaints.erp.modules.admin.service`

**Responsibility**: Support ticket lifecycle management

### Dependencies
- SupportTicketRepository
- CompanyContextService
- SupportTicketGitHubSyncService

### Public Methods

| Method | Signature | Description |
|--------|-----------|-------------|
| create | `SupportTicketResponse create(SupportTicketCreateRequest request)` | Create ticket |
| list | `List<SupportTicketResponse> list()` | List tickets (role-filtered) |
| getById | `SupportTicketResponse getById(Long ticketId)` | Get ticket by ID |

### Status
✅ **Canonical** - Support ticket management

---

## TenantRuntimePolicyService

**File**: `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/admin/service/TenantRuntimePolicyService.java`

**Package**: `com.bigbrightpaints.erp.modules.admin.service`

**Responsibility**: Tenant runtime quota enforcement and metrics

### Dependencies
- CompanyContextService
- UserAccountRepository
- AuditService
- TenantRuntimeEnforcementService

### Public Methods

| Method | Signature | Description |
|--------|-----------|-------------|
| metrics | `TenantRuntimeMetricsDto metrics()` | Get tenant metrics |
| assertCanAddEnabledUser | `void assertCanAddEnabledUser(Company company, String operation)` | Assert user quota not exceeded |

### Status
✅ **Canonical** - Tenant quota enforcement
