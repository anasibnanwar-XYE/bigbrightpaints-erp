# Privileged user boundary hardening

- Feature: `privileged-user-boundary-hardening`
- Frontend impact: low, no code change required

## Notes

- Admin user-management request and success-response payload shapes stay the same.
- The current contract normalizes tenant-boundary authorization and audit behavior across force-reset, status enable/disable, lock/unlock, session revoke, and MFA disable. Retired `suspend`, `unsuspend`, and `DELETE /api/v1/admin/users/{userId}` aliases are absent/non-mutating.
- Current foreign-target masking semantics were refined later by `masked-admin-target-lookup-hardening`; use that note for the current missing-vs-foreign target behavior on auth-sensitive tenant-admin actions.
- No frontend code change required.
