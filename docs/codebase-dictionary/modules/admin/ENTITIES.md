# Admin Entities

## Overview

| Entity | Table | Purpose |
|--------|-------|---------|
| ChangelogEntry | changelog_entries | System version announcements |
| ExportRequest | export_requests | Export approval tracking |
| SupportTicket | support_tickets | User support tickets |

---

## ChangelogEntry

**File**: `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/admin/domain/ChangelogEntry.java`

**Package**: `com.bigbrightpaints.erp.modules.admin.domain`

**Table**: `changelog_entries`

**Responsibility**: Stores system changelog entries for version announcements

### Fields

| Field | Type | Column | Nullable | Description |
|-------|------|--------|----------|-------------|
| id | Long | id | No | Primary key |
| versionLabel | String | version_label | No | Version number (max 32 chars) |
| title | String | title | No | Entry title (max 255 chars) |
| body | String | body | No | Entry content (TEXT) |
| publishedAt | Instant | published_at | No | Publication timestamp |
| createdBy | String | created_by | No | Creator identifier |
| highlighted | boolean | highlighted | No | Featured entry flag |
| deleted | boolean | deleted | No | Soft delete flag |
| createdAt | Instant | created_at | No | Creation timestamp |
| updatedAt | Instant | updated_at | No | Last update timestamp |
| deletedAt | Instant | deleted_at | Yes | Deletion timestamp |

### Inheritance
- Extends `VersionedEntity`

---

## ExportRequest

**File**: `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/admin/domain/ExportRequest.java`

**Package**: `com.bigbrightpaints.erp.modules.admin.domain`

**Table**: `export_requests`

**Responsibility**: Tracks data export requests requiring approval

### Fields

| Field | Type | Column | Nullable | Description |
|-------|------|--------|----------|-------------|
| id | Long | id | No | Primary key |
| company | Company | company_id | No | Owning company (FK) |
| userId | Long | user_id | No | Requesting user ID |
| reportType | String | report_type | No | Report type identifier |
| parameters | String | parameters | Yes | Export parameters (TEXT) |
| status | ExportApprovalStatus | status | No | Approval status |
| createdAt | Instant | created_at | No | Request timestamp |
| approvedBy | String | approved_by | Yes | Approver identifier |
| approvedAt | Instant | approved_at | Yes | Approval timestamp |
| rejectionReason | String | rejection_reason | Yes | Rejection reason (TEXT) |

### Relationships

| Relationship | Type | Target | Description |
|--------------|------|--------|-------------|
| company | ManyToOne | Company | Owning company |

### Inheritance
- Extends `VersionedEntity`

---

## SupportTicket

**File**: `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/admin/domain/SupportTicket.java`

**Package**: `com.bigbrightpaints.erp.modules.admin.domain`

**Table**: `support_tickets`

**Responsibility**: User support tickets with GitHub sync

### Fields

| Field | Type | Column | Nullable | Description |
|-------|------|--------|----------|-------------|
| id | Long | id | No | Primary key |
| publicId | UUID | public_id | No | Public identifier |
| company | Company | company_id | No | Owning company (FK) |
| userId | Long | user_id | No | Requester user ID |
| category | SupportTicketCategory | category | No | Ticket category |
| subject | String | subject | No | Ticket subject (max 255 chars) |
| description | String | description | No | Ticket description (TEXT) |
| status | SupportTicketStatus | status | No | Ticket status |
| githubIssueNumber | Long | github_issue_number | Yes | Linked GitHub issue |
| githubIssueUrl | String | github_issue_url | Yes | GitHub issue URL (max 512 chars) |
| githubIssueState | String | github_issue_state | Yes | GitHub issue state |
| githubSyncedAt | Instant | github_synced_at | Yes | Last GitHub sync |
| githubLastError | String | github_last_error | Yes | Last sync error (TEXT) |
| githubLastSyncAt | Instant | github_last_sync_at | Yes | Last sync attempt |
| resolvedAt | Instant | resolved_at | Yes | Resolution timestamp |
| resolvedNotificationSentAt | Instant | resolved_notification_sent_at | Yes | Resolution notification |
| createdAt | Instant | created_at | No | Creation timestamp |
| updatedAt | Instant | updated_at | No | Last update timestamp |

### Relationships

| Relationship | Type | Target | Description |
|--------------|------|--------|-------------|
| company | ManyToOne | Company | Owning company |

### Inheritance
- Extends `VersionedEntity`

---

## Enums

### SupportTicketCategory

**File**: `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/admin/domain/SupportTicketCategory.java`

| Value | Description |
|-------|-------------|
| BUG | Bug report |
| FEATURE_REQUEST | Feature request |
| SUPPORT | General support |

### SupportTicketStatus

**File**: `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/admin/domain/SupportTicketStatus.java`

| Value | Description |
|-------|-------------|
| OPEN | New ticket |
| IN_PROGRESS | Being worked on |
| RESOLVED | Resolved |
| CLOSED | Closed |

### ExportApprovalStatus (DTO)

**File**: `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/admin/dto/ExportApprovalStatus.java`

| Value | Description |
|-------|-------------|
| PENDING | Awaiting approval |
| APPROVED | Approved |
| REJECTED | Rejected |

---

## Repositories

| Repository | Entity | Key Methods |
|------------|--------|-------------|
| ChangelogEntryRepository | ChangelogEntry | `findByIdAndDeletedFalse()`, `findFirstByHighlightedTrueAndDeletedFalseOrderByPublishedAtDescIdDesc()` |
| ExportRequestRepository | ExportRequest | `findByCompanyAndStatusOrderByCreatedAtAsc()`, `findByCompanyAndId()` |
| SupportTicketRepository | SupportTicket | `findByCompanyOrderByCreatedAtDesc()`, `findByCompanyAndUserIdOrderByCreatedAtDesc()`, `findTop200ByGithubIssueNumberIsNotNullAndStatusInOrderByCreatedAtAsc()` |
