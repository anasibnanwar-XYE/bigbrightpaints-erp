# Lane 02 Exec Spec

## Covers
- Backlog row 2
- `TEN-03`, `AUTH-01`, `AUTH-02`, `AUTH-03`, `AUTH-04`, `AUTH-05`, `AUTH-06`, `AUTH-07`, `AUTH-09`, `ADMIN-08`, `ADMIN-14`, `OPS-02`

## Why This Lane Is P0
It carries the clearest direct exposure: leaked onboarding credentials, plaintext bearer secrets, weak must-change-password enforcement, and inconsistent cross-tenant incident-response controls.

## Primary Review Sources
- `flows/company-tenant-control-plane.md`
- `flows/auth-identity.md`
- `flows/admin-governance.md`
- `ops-deployment-runtime.md`

## Primary Code Hotspots
- `AuthService`
- `PasswordResetService`
- `RefreshTokenService`
- `TenantAdminProvisioningService`
- `AdminUserService`
- `SecurityConfig`

## Entry Criteria
- Lane 01 ownership is clear for any company-context or tenant-policy paths touched by auth work
- `AUTH-09` and `ADMIN-14` are reproducible on the current branch before code changes begin
- the token-storage migration plan names the dual-compatibility window and cleanup owner
- support and operator owners agree on the required reset, lockout, and incident-response contract

## Produces For Other Lanes
- a secure credential and reset baseline for Lanes 05 and 06
- a real forced-password-change corridor that frontend can rely on
- durable incident-response semantics for tenant-admin and super-admin controls

## Packet Sequence

### Packet 0 - close the current auth-branch regressions
- split token-persistence failures from delivery masking so `AUTH-09` is closed without user enumeration
- remove foreign-user write locks before scope checks so `ADMIN-14` is closed without weakening masked responses
- confirm whether the deprecated super-admin forgot-password alias is being repaired as contract cleanup or retired
- output: regression proof for the current auth PR gate

### Packet 1 - fix temporary-credential exposure and corridor enforcement
- stop returning onboarding temporary passwords in normal API responses
- make required delivery success explicit on hard-reset flows
- enforce a limited session corridor until password change is complete
- output: temp-credential contract note and forced-change regression suite

### Packet 2 - migrate bearer secrets to digest storage safely
- move password-reset and refresh-token storage to digest or equivalent non-replayable persistence
- use dual-read or dual-write compatibility until old rows can be retired safely
- output: migration plan, compatibility tests, and rollback-first write-path strategy

### Packet 3 - align incident-response, lockout, session, and MFA behavior
- make lockout semantics observable and consistent
- align password-change, reset, disable, and support-reset session revocation posture
- decide the live MFA recovery-code persistence model and remove silent schema drift
- output: support runbook note and auth incident-response proof

## Frontend And Operator Handoff
- frontend gets exact forced-password-change, logout, and lockout contracts before UI changes are requested
- support runbooks call out what is masked, what is observable, and what a reset or lockout actually revokes
- security and release notes must name the token-storage compatibility window explicitly

## Stop-The-Line Triggers
- response-contract changes and token-schema migration are mixed without dual compatibility
- masking or anti-enumeration behavior is weakened to make error handling simpler
- cross-tenant admin incident work expands into company-lifecycle or global-settings redesign
- a token-digest migration has no cleanup or rollback story

## Must Not Mix With
- tenant lifecycle model rewrite
- global settings governance
- accounting-boundary redesign

## Must-Pass Evidence
- login, refresh, logout, reset, and MFA regression coverage
- migration proof for token storage if schema changes
- explicit tests for forced-password-change corridor
- masked foreign-tenant lookup tests proving no lock is acquired before scope checks

## Rollback
- preserve read compatibility for old token rows until the digest migration is fully proven and revert the write path first if rollback is needed

## Exit Gate
- no plaintext bearer secret remains required for live auth flows
- temporary credentials cannot keep operating as ordinary bearer sessions
- delivery failures stay masked where intended, but token persistence failures are observable and actionable
- foreign-tenant admin masking no longer amplifies lock contention
