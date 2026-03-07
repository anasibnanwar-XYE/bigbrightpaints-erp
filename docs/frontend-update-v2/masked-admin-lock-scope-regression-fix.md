# Masked admin lock-scope regression fix

- Feature: `masked-admin-lock-scope-regression-fix`
- Frontend impact: none

## Notes

- No request-body or success-response payload shapes changed for `PATCH /api/v1/admin/users/{id}/suspend`, `PATCH /api/v1/admin/users/{id}/unsuspend`, `PATCH /api/v1/admin/users/{id}/mfa/disable`, or `DELETE /api/v1/admin/users/{id}`.
- Tenant-admin foreign-target attempts on those paths still return the same masked `400 User not found` validation envelope as truly missing ids.
- The backend now acquires pessimistic locks only through company-scoped lookup before it falls back to a non-locking existence check for internal denial auditing, so the cross-tenant contention regression is fixed without changing frontend-visible behavior.
- No frontend code change is required.
