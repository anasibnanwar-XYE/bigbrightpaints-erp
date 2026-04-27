# Masked admin lock-scope regression fix

- Feature: `masked-admin-lock-scope-regression-fix`
- Frontend impact: none

## Notes

- This historical packet has been superseded by the identity route hard cut: retired admin suspend, unsuspend, and hard-delete aliases are no longer product APIs.
- Current no-content admin user controls are `POST /api/v1/admin/users/{userId}/lock`, `POST /api/v1/admin/users/{userId}/unlock`, `PATCH /api/v1/admin/users/{id}/mfa/disable`, and `DELETE /api/v1/admin/users/{userId}/sessions`.
- Tenant-admin foreign-target attempts on current canonical mutation paths return the same masked `400 User not found` validation envelope as truly missing ids.
- The backend now acquires pessimistic locks only through company-scoped lookup before it falls back to a non-locking existence check for internal denial auditing, so the cross-tenant contention regression is fixed without changing frontend-visible behavior.
- No frontend code change is required.
