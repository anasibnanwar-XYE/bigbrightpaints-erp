# Auth and Company Scope

Last reviewed: 2026-04-28

## Canonical Auth Routes

- `POST /api/v1/auth/login`
- `GET /api/v1/auth/me`
- `POST /api/v1/auth/refresh-token`
- `POST /api/v1/auth/logout`
- `PATCH /api/v1/auth/me/profile`
- `PATCH /api/v1/auth/me/contact`
- `GET /api/v1/auth/me/security`
- `GET /api/v1/auth/me/security-events`
- `POST /api/v1/auth/password/change`
- `POST /api/v1/auth/password/forgot`
- `POST /api/v1/auth/password/reset`
- `GET /api/v1/auth/mfa`
- `POST /api/v1/auth/mfa/setup`
- `POST /api/v1/auth/mfa/activate`
- `POST /api/v1/auth/mfa/disable`
- `POST /api/v1/auth/mfa/recovery-codes/regenerate`
- `GET /api/v1/auth/sessions`
- `DELETE /api/v1/auth/sessions/{sessionId}`
- `DELETE /api/v1/auth/sessions/current`
- `DELETE /api/v1/auth/sessions`

## Scope Rules

- Persist `companyCode`, never numeric `companyId`, for tenant shells.
- Tenant-scoped requests must send `X-Company-Code`.
- `X-Company-Id` is retired and must not be sent.
- Do not build tenant switching around alternate company identifiers.
- Superadmin shell is separate and must not be mounted inside tenant-admin routes.
- Platform-only superadmin hosts (`settings`, `roles`, `notify`) require the
  platform auth scope code; tenant-scoped superadmin sessions are denied on
  those hosts.

## Bootstrap Contract

Use this as the sole frontend identity bootstrap endpoint:

```text
GET /api/v1/auth/me
```

Important fields:

- `email`
- `displayName`
- `companyCode`
- `mfaEnabled`
- `mustChangePassword`
- `roles`
- `permissions`

### Must-change-password corridor

If `/auth/me` returns `mustChangePassword=true`:

1. Route to password-change immediately.
2. Block normal tenant shell routes until password change succeeds.
3. Re-hydrate session state with `GET /api/v1/auth/me` after change.

### Session refresh

```text
POST /api/v1/auth/refresh-token
```

Use refreshed token pair, then re-fetch `/auth/me`.

### Logout

```text
POST /api/v1/auth/logout
```

May include refresh token for explicit revocation.

### My Account security history

```text
GET /api/v1/auth/me/security-events?type=&page=0&size=50
```

Returns `ApiResponse<PageResponse<SelfSecurityEvent>>` with `content`,
`page`, `size`, `totalElements`, and `totalPages`. The backend clamps
`size` to `1..100`. Type filtering is applied before ordering and paging;
ordering is stable by `occurredAt desc, id desc`. Self-history rows are scoped to the
authenticated stable account/company scope and expose only privacy-safe fields:
`type`, `eventType`, `companyCode`, `authScopeCode`, `outcome`, `reason`,
`createdAt`, and allowlisted `metadata` (`operation`, `reason`, `outcome`,
`action`, `changedFields`). Do not expect actor IDs, target user IDs, session
IDs, token material, MFA secrets, recovery codes, hashes, or raw user-agent
fingerprints in this feed.

## Tenant-admin self/settings integration

Tenant-admin self/settings UX uses:

- `GET /api/v1/admin/self/settings` for self settings read model
- auth APIs for password and MFA mutations

Do not use `/api/v1/auth/password/forgot/superadmin`; password recovery is always the scoped
`POST /api/v1/auth/password/forgot` route with `{ email, companyCode }`.

## My Account profile, contact, security, and sessions

- `PATCH /api/v1/auth/me/profile` updates only self-owned display profile fields such as
  `preferredName` and `profilePictureUrl`; it does not accept role, tenant, email, or security
  mutations.
- `PATCH /api/v1/auth/me/contact` updates self contact fields such as secondary email or phone
  without changing login identity.
- `GET /api/v1/auth/me/security` returns a privacy-safe security summary for the current principal.
- `GET /api/v1/auth/sessions` lists only the caller's active sessions/devices and omits access
  tokens, refresh tokens, token digests, password hashes, MFA secrets, recovery codes, and raw
  user-agent fingerprints.
- `DELETE /api/v1/auth/sessions/{sessionId}` revokes another own session, `DELETE
  /api/v1/auth/sessions/current` revokes the current session, and `DELETE /api/v1/auth/sessions`
  revokes all sessions for the current user.
- `POST /api/v1/auth/mfa/recovery-codes/regenerate` requires fresh MFA proof and returns the new
  recovery-code set only once; old unused codes are invalidated and affected sessions are revoked.

## Role Boundaries

| Role | Portal shell | Canonical route ownership |
| --- | --- | --- |
| `ROLE_SUPER_ADMIN` | superadmin | `/api/v1/superadmin/**` control-plane routes |
| `ROLE_ADMIN` | tenant-admin | tenant-scoped `/api/v1/admin/**` workflows |
| `ROLE_ACCOUNTING` | accounting | `/api/v1/accounting/**`, accounting portal workflows |
| `ROLE_SALES` | sales | `/api/v1/sales/**`, sales portal workflows |
| `ROLE_FACTORY` | factory | `/api/v1/factory/**`, dispatch/production workflows |
| `ROLE_DEALER` | dealer-client | `/api/v1/dealer-portal/**` |

Boundary notes:

- A `ROLE_SUPER_ADMIN` user must not be routed into tenant-admin shell routes.
- Tenant-admin workflows are tenant-scoped; control-plane actions remain superadmin-only.
- Tenant-scoped superadmin sessions are explicitly denied on `/api/v1/superadmin/settings`,
  `/api/v1/superadmin/roles`, and `/api/v1/superadmin/notify`.
