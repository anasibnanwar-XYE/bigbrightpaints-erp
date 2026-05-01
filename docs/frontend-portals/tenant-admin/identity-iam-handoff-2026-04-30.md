# Tenant-Admin Identity/IAM Frontend Handoff

Last reviewed: 2026-04-30

This is the current frontend handoff for PR #197 identity/account hard-cut behavior. The tenant-admin UI should treat `/api/v1/auth/**` as the My Account/IAM owner and `/api/v1/admin/users/**` as tenant-admin target-user control.

## Bootstrap and scope

- Bootstrap every tenant-admin shell with `GET /api/v1/auth/me`.
- Send `X-Company-Code` on tenant-scoped requests.
- Do not send or persist numeric company IDs for tenant-admin shell behavior.
- If `mustChangePassword=true`, route to password change before normal tenant-admin pages.

## My Account APIs

| UX area | Endpoint |
| --- | --- |
| Current identity | `GET /api/v1/auth/me` |
| Profile display fields | `PATCH /api/v1/auth/me/profile` |
| Secondary contact fields | `PATCH /api/v1/auth/me/contact` |
| Security summary | `GET /api/v1/auth/me/security` |
| Security history | `GET /api/v1/auth/me/security-events?type=&page=0&size=50` |
| Password change | `POST /api/v1/auth/password/change` |

Security history returns a paged response with privacy-safe rows only: `type`, `eventType`, `companyCode`, `authScopeCode`, `outcome`, `reason`, `createdAt`, and allowlisted `metadata`. Do not expect actor IDs, target user IDs, session IDs, token material, MFA secrets, recovery codes, password hashes, or raw user-agent fingerprints.

## MFA APIs

| UX action | Endpoint | Notes |
| --- | --- | --- |
| Read status | `GET /api/v1/auth/mfa` | Returns `{ "enabled": boolean }` inside `ApiResponse` |
| Start setup | `POST /api/v1/auth/mfa/setup` | Returns secret, QR URI, and one-time recovery codes |
| Activate | `POST /api/v1/auth/mfa/activate` | Requires TOTP code |
| Disable | `POST /api/v1/auth/mfa/disable` | Requires TOTP or recovery code |
| Regenerate recovery codes | `POST /api/v1/auth/mfa/recovery-codes/regenerate` | Requires fresh MFA proof and returns new codes once |

## Session APIs

| UX action | Endpoint |
| --- | --- |
| List active sessions/devices | `GET /api/v1/auth/sessions` |
| Revoke another own session | `DELETE /api/v1/auth/sessions/{sessionId}` |
| Revoke current session | `DELETE /api/v1/auth/sessions/current` |
| Revoke all own sessions | `DELETE /api/v1/auth/sessions` |

Session rows are current-principal-only and must not expose access tokens, refresh tokens, token digests, password hashes, MFA secrets, recovery codes, or raw user-agent fingerprints.

## Tenant-admin target-user controls

| UX action | Endpoint |
| --- | --- |
| List users | `GET /api/v1/admin/users` |
| Create user | `POST /api/v1/admin/users` |
| Read user | `GET /api/v1/admin/users/{id}` |
| Update display/roles | `PUT /api/v1/admin/users/{id}` |
| Enable/disable | `PUT /api/v1/admin/users/{userId}/status` |
| Force password reset | `POST /api/v1/admin/users/{userId}/force-reset-password` |
| Lock | `POST /api/v1/admin/users/{userId}/lock` |
| Unlock | `POST /api/v1/admin/users/{userId}/unlock` |
| Disable target MFA | `PATCH /api/v1/admin/users/{id}/mfa/disable` |
| Revoke target sessions | `DELETE /api/v1/admin/users/{userId}/sessions` |
| Read target security events | `GET /api/v1/admin/users/{userId}/security-events?type=` |
| Assignable roles | `GET /api/v1/admin/users/assignable-roles` |

Tenant-admin controls cannot operate on `ROLE_ADMIN` or `ROLE_SUPER_ADMIN` targets.

## Retired routes

Do not call these retired aliases:

- `/api/v1/auth/profile`
- `/api/v1/auth/password/forgot/superadmin`
- `PATCH /api/v1/admin/users/{id}/suspend`
- `PATCH /api/v1/admin/users/{id}/unsuspend`
- `DELETE /api/v1/admin/users/{id}`
