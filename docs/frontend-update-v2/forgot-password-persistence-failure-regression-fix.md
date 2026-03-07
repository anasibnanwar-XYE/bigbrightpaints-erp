# Forgot-password persistence failure regression fix

- Feature: `forgot-password-persistence-failure-regression-fix`
- Frontend impact: historical only

## Notes

- This packet note is superseded by `review-fix-auth-regressions-from-pr90`.
- The live `POST /api/v1/auth/password/forgot` contract once again keeps the same generic success payload for known-user and unknown-user requests even when reset-token persistence fails before a reset link can be stored.
- No frontend migration is required; clients should continue treating forgot-password as externally non-enumerating while operators use backend diagnostics for persistence failures.
