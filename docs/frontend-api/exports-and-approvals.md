# Exports and Approvals

Last reviewed: 2026-03-31

## Export Workflows

Exports are asynchronous operations that return a file (CSV, Excel, PDF) after processing.

### Export Request Flow

1. **Request export** — Submit an export request with parameters.
2. **Get status** — Poll for export completion status.
3. **Download** — When ready, download the generated file.

### Create Export Request

```
POST /api/v1/exports/request
```

**Request:**

```json
{
  "exportType": "JOURNAL_ENTRY",
  "format": "CSV",
  "parameters": {
    "startDate": "2026-01-01",
    "endDate": "2026-03-31",
    "accountCodes": ["1000", "2000"]
  },
  "idempotencyKey": "550e8400-e29b-41d4-a716-446655440000"
}
```

**Response:**

```json
{
  "success": true,
  "data": {
    "requestId": "export-req-001",
    "status": "PROCESSING",
    "createdAt": "2026-03-31T10:00:00Z"
  },
  "message": "Export request created",
  "timestamp": "2026-03-31T10:00:00Z"
}
```

### Check Export Status

```
GET /api/v1/exports/status/{requestId}
```

**Response:**

```json
{
  "success": true,
  "data": {
    "requestId": "export-req-001",
    "status": "COMPLETED",
    "fileUrl": "/api/v1/exports/download/export-req-001",
    "expiresAt": "2026-04-01T10:00:00Z"
  },
  "timestamp": "2026-03-31T10:05:00Z"
}
```

### Download Export

```
GET /api/v1/exports/download/{requestId}
```

**Status Values:**

| Status | Description |
|---|---|
| `PROCESSING` | Export is being generated — poll for completion |
| `COMPLETED` | File is ready for download |
| `FAILED` | Export failed — check error details |

### Export Types

| Export Type | Module | Description |
|---|---|---|
| `JOURNAL_ENTRY` | accounting | Journal entries for a date range |
| `LEDGER` | accounting | General ledger report |
| `DEALER_AGING` | accounting | Aged receivables by dealer |
| `INVOICE` | invoice | Invoice export |
| `GST_RETURN` | accounting | GST return data |
| `PRODUCT_CATALOG` | catalog | Product/brand catalog |

## Approval Workflows

Approvals are requests that require an authorized user (typically tenant-admin) to approve or reject before the operation proceeds.

### Approval Request Flow

1. **Request approval** — A user triggers an operation that requires approval.
2. **Admin reviews** — Tenant-admin reviews the request and approves or rejects.
3. **Execution** — If approved, the operation executes; if rejected, the requester is notified.

### Submit Approval Request

Some operations automatically create approval requests:

```
POST /api/v1/sales/orders/{orderId}/request-approval
```

**Response:**

```json
{
  "success": true,
  "data": {
    "approvalId": "approval-001",
    "status": "PENDING",
    "requestedBy": "user@example.com",
    "requestedAt": "2026-03-31T10:00:00Z"
  },
  "message": "Approval request created",
  "timestamp": "2026-03-31T10:00:00Z"
}
```

### List Pending Approvals

```
GET /api/v1/admin/approvals?filter.status=PENDING
```

**Response:**

```json
{
  "success": true,
  "data": {
    "content": [
      {
        "approvalId": "approval-001",
        "originType": "CREDIT_REQUEST",
        "ownerType": "SALES",
        "requesterEmail": "user@example.com",
        "summary": "Credit limit increase request for Acme Corp",
        "status": "PENDING",
        "createdAt": "2026-03-31T10:00:00Z"
      }
    ]
  }
}
```

### Approve Request

```
POST /api/v1/admin/approvals/{approvalId}/approve
```

**Request:**

```json
{
  "note": "Approved based on credit history review"
}
```

### Reject Request

```
POST /api/v1/admin/approvals/{approvalId}/reject
```

**Request:**

```json
{
  "note": "Insufficient credit history - please provide more details"
}
```

## Approval Ownership

**Export approvals** belong to **tenant-admin**, not accounting.

| Approval Type | Owner Portal | Endpoint |
|---|---|---|
| Export requests | tenant-admin | `/api/v1/admin/approvals` |
| Credit requests | tenant-admin | `/api/v1/admin/approvals` |
| Period close | tenant-admin | `/api/v1/admin/approvals` |
| Payroll runs | tenant-admin | `/api/v1/admin/approvals` |

See [`docs/frontend-portals/tenant-admin/README.md`](../frontend-portals/tenant-admin/README.md) for detailed approval ownership.

## Approval Origin Types

| Origin Type | Description |
|---|---|
| `CREDIT_REQUEST` | Dealer credit limit increase request |
| `CREDIT_LIMIT_OVERRIDE_REQUEST` | Override existing credit limit |
| `PAYROLL_RUN` | Payroll execution request |
| `PERIOD_CLOSE_REQUEST` | Accounting period close request |
| `EXPORT_REQUEST` | Large export request requiring approval |

## Links

- See [`docs/frontend-portals/tenant-admin/workflows.md`](../frontend-portals/tenant-admin/workflows.md) for approval workflows specific to tenant-admin.
- See [`idempotency-and-errors.md`](./idempotency-and-errors.md) for idempotency handling on export requests.
