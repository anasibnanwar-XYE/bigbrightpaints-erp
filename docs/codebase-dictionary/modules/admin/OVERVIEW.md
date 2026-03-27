# Admin Module Overview

**Module Path:** `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/admin/`  
**Package Root:** `com.bigbrightpaints.erp.modules.admin`

## Purpose

The Admin module provides system administration capabilities including:
- User management (CRUD, status control, MFA management)
- Support ticket system with GitHub issue synchronization
- Changelog management for system updates
- Export approval workflow for sensitive data
- Tenant runtime policy enforcement

## Module Boundaries

### Inbound Dependencies
- **Company Context** (`modules/company`) - Tenant isolation via `CompanyContextHolder`
- **Auth** (`modules/auth`) - User account entities
- **RBAC** (`modules/rbac`) - Role management
- **Sales** (`modules/sales`) - Credit limit requests, credit override requests
- **Accounting** (`modules/accounting`) - Period close requests
- **HR** (`modules/hr`) - Payroll run approvals

### Outbound Dependencies
- **Email Service** (`core/notification`) - User notifications
- **Audit Service** (`core/audit`) - Action logging
- **GitHub API** - External issue sync

### Domain Events Published
- User lifecycle events via `AuditService`
- Configuration change events
- Access denied/allowed events

## Architecture Layers

```
modules/admin/
├── controller/     # REST endpoints (5 controllers)
├── service/        # Business logic (7 services)
├── domain/         # Entities & repositories (8 classes)
└── dto/            # Data transfer objects (19 classes)
```

## Key Design Patterns

### 1. Scoped Access Control
- Non-SUPER_ADMIN users can only manage users within their company scope
- Out-of-scope access attempts are audited and masked as "not found" for security

### 2. GitHub Issue Synchronization
- Support tickets can be synced to GitHub Issues for external tracking
- Status polling reconciles ticket state with GitHub issue state
- Async processing prevents request blocking

### 3. Export Approval Workflow
- System settings can require approval for data exports
- Maker-checker pattern for sensitive data access
- Audit trail for all export requests

### 4. Tenant Runtime Policy
- Enforces tenant-level quotas (users, requests)
- Policy decisions audited with full context
- Integration with request interceptors

## Anti-Patterns to Avoid

### 1. Cross-Tenant User Access
❌ **Wrong:** Accessing users from other companies without SUPER_ADMIN
✅ **Correct:** Use `resolveScopedUserForAdminAction()` which validates scope

### 2. Unaudited Administrative Actions
❌ **Wrong:** Modifying user status without logging
✅ **Correct:** All admin actions go through `auditUserAccountAction()`

### 3. Synchronous GitHub API Calls
❌ **Wrong:** Calling GitHub API in request thread
✅ **Correct:** Use `submitGitHubIssueAsync()` for async processing

## Canonicality Status

| Component | Status | Notes |
|-----------|--------|-------|
| `AdminUserService` | ✅ Canonical | Single entry point for user management |
| `SupportTicketService` | ✅ Canonical | Support ticket lifecycle |
| `ChangelogService` | ✅ Canonical | Version announcements |
| `ExportApprovalService` | ✅ Canonical | Export request approval |
| `TenantRuntimePolicyService` | ✅ Canonical | Tenant quota enforcement |
| `GitHubIssueClient` | ✅ Canonical | GitHub API integration |

## Security Requirements

- **Roles:** `ROLE_ADMIN`, `ROLE_SUPER_ADMIN` for most operations
- **SUPER_ADMIN Only:** Period lock enforcement changes, SUPER_ADMIN role assignment
- **Tenant Isolation:** All queries filtered by `Company` from `CompanyContextHolder`
- **Audit:** All administrative actions logged with actor, target, and metadata

## Configuration

| Property | Description |
|----------|-------------|
| `github.enabled` | Enable GitHub issue sync |
| `github.token` | GitHub personal access token |
| `github.repo-owner` | Repository owner |
| `github.repo-name` | Repository name |
