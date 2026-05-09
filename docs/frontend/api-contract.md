# Frontend API Contract

Last reviewed: 2026-05-09

This file gives frontend developers the compact current-state contract. For
route-by-route detail, use `docs/frontend-api/**` and the matching
`docs/frontend-portals/<portal>/api-contracts.md`.

## Transport Rules

- API base is `/api/v1`.
- Most responses use `ApiResponse<T>` with `success`, `message`, `data`, and
  error metadata when applicable.
- Tenant-scoped calls must include `X-Company-Code`.
- Do not send `X-Company-Id`.
- Do not send requester identity fields such as `requestedBy`; backend derives
  actors from the authenticated principal.
- Capture `traceId` or correlation metadata from error responses when present
  and show it in support/debug copy.

## Auth Bootstrap

| Action | Endpoint | Frontend behavior |
| --- | --- | --- |
| Login | `POST /api/v1/auth/login` | Store current access/refresh credentials according to shell storage policy |
| Bootstrap | `GET /api/v1/auth/me` | Decide portal, role, tenant scope, and password-change corridor |
| Refresh | `POST /api/v1/auth/refresh-token` | Refresh access token without changing tenant scope |
| Logout | `POST /api/v1/auth/logout` | Clear local auth state and leave protected shells |
| Change password | `POST /api/v1/auth/password/change` | Required before normal shell when `mustChangePassword=true` |

## Endpoint Ownership

| Area | Current owner | Canonical endpoints |
| --- | --- | --- |
| Tenant control plane | Superadmin | `/api/v1/superadmin/**` |
| Tenant user/admin operations | Tenant Admin | `/api/v1/admin/**` |
| Accounting execution | Accounting | `/api/v1/accounting/**` |
| Shared finance reports | Accounting | `/api/v1/reports/**` reads documented in accounting portal docs |
| Sales orders and dealer master | Sales | `/api/v1/sales/**`, `/api/v1/dealers/**` |
| Commercial credit | Sales | `/api/v1/credit/**` |
| Production and packing | Factory | `/api/v1/factory/**` |
| Dispatch execution | Factory | `POST /api/v1/dispatch/confirm` |
| Dealer self-service | Dealer Client | `/api/v1/dealer-portal/**` |

## Request Examples

Tenant-scoped read:

```http
GET /api/v1/accounting/accounts/tree
Authorization: Bearer <access-token>
X-Company-Code: ACME
```

Idempotent mutation:

```http
POST /api/v1/accounting/journal-entries
Authorization: Bearer <access-token>
X-Company-Code: ACME
Idempotency-Key: journal-2026-05-09-001
Content-Type: application/json
```

## Error And Validation UX

- `400`: malformed request or invalid enum/filter input. Fix form/query state.
- `401`: unauthenticated or expired session. Refresh or return to login.
- `403`: authenticated but not allowed for this role or tenant scope.
- `404`: missing resource or out-of-scope resource.
- `409`: state conflict, duplicate idempotency key with different payload, or
  concurrent workflow conflict.
- `422`: business validation failed, such as credit limit exceeded or blocked
  accounting readiness.
- `429`: quota/rate/concurrency limit. Respect retry metadata when present.

Frontend should render backend `details` entries as field-level or workflow
blockers when the response provides them.

## Idempotency

- Use `Idempotency-Key` for user-triggered mutations that may be retried.
- Reusing a key with the same payload may return the original result.
- Reusing a key with a different payload is a conflict and should not be
  auto-retried with a new key unless the user explicitly starts a new intent.
- Do not implement legacy idempotency signature fallbacks in the frontend.

## Pagination And Filters

- List routes use zero-based `page` and bounded `size` where pagination is
  supported.
- Unsupported filters should be treated as frontend bugs, not silently ignored
  UI options.
- Some legacy-shaped list routes still return plain arrays; check the matching
  portal `api-contracts.md` before assuming `PageResponse<T>`.

## Canonical References

- Shared contract docs: `docs/frontend-api/README.md`
- Portal contracts: `docs/frontend-portals/README.md`
- Error/idempotency detail: `docs/frontend-api/idempotency-and-errors.md`
- DTO examples: `docs/frontend-api/dto-examples.md`
