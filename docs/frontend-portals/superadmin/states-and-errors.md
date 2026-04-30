# States And Errors

Last reviewed: 2026-04-30

## Screen states

### Add Client and activation

- `idle`
- `loading-options`
- `ready`
- `submitting-draft`
- `submitting-send-activation`
- `pending-activation`
- `activation-action-running`
- `seed-repair-running`
- `failed`

Use `pending-activation` only after the response status is `PENDING_ACTIVATION` and activation metadata says an email/link action happened. For `DRAFT`, show activation as not sent.

### Client detail

- `loading`
- `ready`
- `mutating-lifecycle`
- `mutating-plan-limits`
- `mutating-billing`
- `mutating-support`
- `mutating-seed-status`
- `mutating-observability`
- `reload-failed`

### Support, bug, and audit

- `loading-list`
- `empty-filtered`
- `ready`
- `posting-message`
- `posting-internal-note`
- `changing-status`
- `refreshing-sla`
- `linking-sentry`
- `remediating-event`
- `rate-limited`

## Standard envelope

All Super Admin routes return the standard `ApiResponse` envelope. Success includes a `data` payload plus timestamp/trace metadata. Failure includes safe fields such as `data.code`, `data.message`, `data.reason`, `data.path`, `data.traceId`, and optional field details. Never render stack traces, SQL, Java class names, token values, or provider credentials.

Example failure shape:

```json
{
  "success": false,
  "message": "Unsupported sort field",
  "data": {
    "code": "VAL_001",
    "message": "Unsupported sort field",
    "reason": "sort must be one of the documented fields",
    "path": "/api/v1/superadmin/tenants",
    "traceId": "TRACE_ID_FROM_RESPONSE",
    "details": {
      "sort": "allowed: companyName,status,trialEndsAt,lastActivityAt"
    }
  },
  "timestamp": "2026-04-30T00:00:00Z"
}
```

## Unknown input matrix

| Probe | Expected status | UI treatment |
| --- | --- | --- |
| unsupported filter query | `400` | remove stale filter chip and show inline filter error |
| invalid enum such as status or cadence | `400` | keep form open and show enum options from docs/options response |
| invalid sort field | `400` | reset sort to documented default |
| page below zero or size above max | `400` | reset to `page=0` and a documented bounded size |
| forbidden body field such as role/authority/tenant override in profile | `400` or `403` | block submission and highlight forbidden field |
| retired route call | `404` or `410` | show stale-client upgrade message; do not retry alternate legacy routes |
| duplicate tenant, plan, or billing idempotency key conflict | `409` | show already-exists or replay state without duplicating side effects |
| burst/platform rate limit | `429` | read retry/reset metadata when present and disable retry button until allowed |

## Important failures to surface clearly

| Condition | Likely cause | UI treatment |
| --- | --- | --- |
| duplicate tenant code or owner email | normalized duplicate | inline field error and no retry loop |
| seed state blocks activation | required seed artifact not usable | show seed categories and repair action if available |
| activation expired, used, malformed, or superseded | invalid token state | ask operator/owner to resend; never display token values |
| invalid plan or override | stale plan/version or unsupported key | reload plan templates and entitlements |
| quota/rate limit block | durable quota, burst, or concurrent limit exceeded | show dimension, reset/retry metadata, and safe reads state |
| billing duplicate active subscription | one-active-subscription invariant | show current subscription and reprice/snapshot action |
| Sentry/Datadog unavailable | integration disabled or degraded | show degraded status without credentials; keep core flow available |
| privacy canary rejected/redacted | free-text ingress guard | show sanitized summary only |

## Empty states

- Client list empty: show Add Client CTA.
- Support queue empty: show active filters and SLA status summary.
- Messages empty: show “No customer-visible messages yet.” Internal notes remain platform-only.
- Audit/security/suspicious lists empty: show filter summary, not a normal success toast.
- Cost snapshots empty: show create-snapshot CTA.
