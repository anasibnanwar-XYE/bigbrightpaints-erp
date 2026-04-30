# Auth reset recovery contract hardening

- Feature: `auth-reset-recovery-contract-hardening`
- Frontend impact: medium, conditional path migration

## Notes

- `POST /api/v1/auth/password/forgot` now requires `{ email, companyCode }`.
- The retired `POST /api/v1/auth/password/forgot/superadmin` alias is removed from the canonical contract and must not be called.
- Public forgot-password stays anti-enumeration-safe for unknown or disabled identities, but known scoped accounts now fail closed when reset-token storage or email delivery fails.
- `POST /api/v1/auth/password/reset` remains token-based and scoped by the issued reset token, not by request headers.
- Historical note: the Super Admin support admin-password-reset route (`POST /api/v1/superadmin/tenants/{id}/support/admin-password-reset`) is retired and must not be documented or called as a current support recovery surface.
- Current recovery UX should use the standard scoped forgot/reset flow above, with reset links/tokens handled only through the normal redacted delivery boundary.
- Superadmin warning/context operations live only on the same tenant-scoped namespace:
  - `POST /api/v1/superadmin/tenants/{id}/support/warnings`
  - `PUT /api/v1/superadmin/tenants/{id}/support/context`
- Retired company-scoped aliases such as `/api/v1/companies/{id}/support/*` are not part of the published contract.
