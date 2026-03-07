# PR #90 auth review follow-up

- Feature: `review-fix-auth-regressions-from-pr90`
- Frontend impact: none

## Notes

- `POST /api/v1/auth/password/forgot` keeps the same request body and the same generic success payload even when reset-token persistence fails for a known account, so known-user and unknown-user flows remain externally indistinguishable.
- Fresh-session usability after logout/password-change/reset revocation markers is fixed entirely inside token timestamp alignment; no login, refresh, logout, or `/auth/me` request/response shape changed.
- Frontend and support consumers should continue treating forgot-password as a masked success flow while relying on backend logs/metrics, not user-visible responses, for persistence failure diagnostics.
