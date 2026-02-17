# Super Admin JSON-Rendered UI Contract (V1)

Portal: `SUPER_ADMIN_CONSOLE`  
Login: dedicated super-admin login screen  
Authority: `ROLE_SUPER_ADMIN` only

## 1) Purpose
The super-admin console must be generated from backend-provided JSON config and data contracts so tenant controls are deterministic, auditable, and easy to evolve.

This contract is for backend/frontend handoff and must remain synchronized with:
- `docs/frontend-v1-portal-handoff.yaml`
- `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/company/controller/CompanyController.java`

## 2) Required API Packet

| Method | Path | Use |
|---|---|---|
| POST | `/api/v1/auth/login` | super-admin authentication |
| GET | `/api/v1/companies` | tenant list |
| POST | `/api/v1/companies` | tenant bootstrap |
| PUT | `/api/v1/companies/{id}` | quota + tenant config update |
| POST | `/api/v1/companies/{id}/lifecycle-state` | ACTIVE/HOLD/BLOCKED transitions with reason |
| GET | `/api/v1/companies/{id}/tenant-metrics` | usage, quota, lifecycle, activity metrics |

OpenAPI parity blocker:
- Lifecycle and tenant-metrics endpoints must exist in OpenAPI before release closure.

## 3) JSON UI Schema Blocks

### 3.1 Tenant list screen contract
```json
{
  "screen": "tenant_list",
  "table": {
    "columns": ["companyCode", "name", "lifecycleState", "activeUserCount", "apiErrorRateInBasisPoints"],
    "row_actions": ["open_tenant_detail", "open_lifecycle_dialog", "open_quota_dialog"]
  },
  "filters": ["companyCode", "lifecycleState"],
  "sort": ["companyCode", "activeUserCount", "apiErrorRateInBasisPoints"]
}
```

### 3.2 Tenant detail metrics card contract
```json
{
  "screen": "tenant_detail",
  "cards": [
    "lifecycleState",
    "lifecycleReason",
    "activeUserCount",
    "apiActivityCount",
    "apiErrorCount",
    "apiErrorRateInBasisPoints",
    "distinctSessionCount",
    "auditStorageBytes"
  ],
  "quota_fields": [
    "quotaMaxActiveUsers",
    "quotaMaxApiRequests",
    "quotaMaxStorageBytes",
    "quotaMaxConcurrentSessions",
    "quotaSoftLimitEnabled",
    "quotaHardLimitEnabled"
  ]
}
```

### 3.3 Lifecycle action dialog contract
```json
{
  "action": "update_lifecycle_state",
  "request": {
    "state": "ACTIVE|HOLD|BLOCKED",
    "reason": "string (required, max 1024)"
  },
  "validation": {
    "state_required": true,
    "reason_required": true
  }
}
```

## 4) UX Rules (Fail-Closed)
- Never allow lifecycle mutation without explicit reason.
- Never hide backend denial messages for authority/scope violations.
- Always refresh metrics payload after quota or lifecycle writes.
- Never show tenant business-action controls until tenant context is explicitly selected.

## 5) Recovery and Error Rules
- `401`: force re-auth and reset stale super-admin state.
- `403`: show super-admin boundary denial and block retries.
- `409`: show conflict state and trigger fresh tenant read.
- `422`: inline field validation with request schema hints.

## 6) Frontend Engineering Checklist
- Build console from JSON blocks, not hardcoded per-tenant screens.
- Keep table/action contracts backward compatible across minor releases.
- Add audit-friendly confirmation step for hold/block and quota changes.
- Preserve immutable timeline panel for lifecycle + quota actions.
