# Current Auth Branch Merge Gate Packet

Use this packet before broader remediation starts. The broader program should not begin while these regressions remain open on the current auth-hardening branch.

## 1. Header
- lane: merge gate across Lane 01 and Lane 02
- slice name: current auth branch regression closure
- finding IDs: `TEN-10`, `AUTH-09`, `ADMIN-14`
- implementer: `TBD before code changes start`
- reviewer: `TBD before merge`
- QA owner: `TBD before regression run`
- release approver: `TBD before promotion`
- branch: current auth-hardening branch
- target environment: local plus PostgreSQL-backed regression environment

## 2. Lane Start Gate
- `TEN-10` is reproducible on the canonical `PUT /api/v1/companies/{id}/tenant-runtime/policy` path
- `AUTH-09` is reproducible on the public forgot-password flow or proven by targeted service regression
- `ADMIN-14` is reproducible on masked tenant-admin foreign-user actions
- no unrelated token-digest migration, lifecycle redesign, or global settings governance work is mixed into this slice

## 3. Why This Slice Exists
- broader remediation cannot start cleanly while the current auth branch still carries control-plane and auth regressions
- the slice closes one cache invalidation regression, one auth error-handling regression, and one cross-tenant lock-amplification regression
- finishing this packet unlocks the normal entry into Lane 01 and Lane 02

## 4. Scope
- fix immediate policy-cache invalidation for canonical company runtime-policy updates
- keep public forgot-password delivery masking but stop swallowing token-persistence failures
- remove foreign-user write-lock acquisition before masked scope checks
- do not widen into token digest storage migration, admin-user redesign, or broader runtime-policy consolidation

## 5. Caller Map
- `CompanyContextFilter`
- `TenantRuntimeEnforcementService`
- `PasswordResetService`
- `AdminUserService`
- supporting repository lock path for masked admin actions
- company runtime-policy controller path
- public forgot-password controller path
- masked admin user-control endpoints

## 6. Invariant Pack
- canonical runtime-policy updates must invalidate policy cache immediately
- public forgot-password must not leak whether a user exists
- public forgot-password must surface token-persistence failures to monitoring and tests
- masked tenant-admin foreign-user actions must keep the masked contract
- masked tenant-admin foreign-user actions must not acquire global write locks before scope checks

## 7. Implementation Plan
1. reproduce the three regressions with targeted tests or probes
2. repair `TEN-10` in the canonical runtime-policy request tracking and completion path
3. repair `AUTH-09` by separating persistence failure handling from delivery masking
4. repair `ADMIN-14` by moving lock acquisition behind successful scope resolution
5. extend targeted regression coverage
6. rerun the targeted regression pack and note any remaining drift before merge

## 8. Proof Pack
- company/runtime regressions: `TS_RuntimeCompanyContextFilterExecutableCoverageTest`, `TS_RuntimeTenantPolicyControlExecutableCoverageTest`, company runtime-policy controller coverage
- auth regressions: `AuthPasswordResetPublicContractIT`, `PasswordResetServiceTest`, relevant auth integration coverage
- admin regressions: `AdminUserServiceTest`, `AdminUserSecurityIT`, masked admin endpoint coverage
- probe or log evidence for the canonical runtime-policy update path if local runtime is available

## 9. Validation-First Evidence
- not a validation-first packet
- use the normal proof pack, because all three items are already confirmed backend regressions on the current branch

## 10. Rollback Pack
- revert the regression fix only; do not roll back unrelated auth-hardening work
- no data migration should be part of this packet
- keep masked response contracts stable during rollback
- rollback trigger threshold: any regression that weakens masking, leaves stale policy enforcement, or reintroduces lock amplification
- rollback rehearsal evidence: rerun the targeted regression pack on the reverted branch before promotion if rollback is considered
- expected RTO: under 1 hour for code-only rollback
- expected RPO: none, because this packet should not carry data migration

## 11. Stop Rule
- split the slice immediately if token schema changes, lifecycle model changes, or broad admin-user behavior cleanup starts to enter the same PR

## 12. Exit Gate
- canonical company runtime-policy updates invalidate cache immediately
- public forgot-password still masks delivery semantics correctly but no longer hides token-persistence failures
- masked tenant-admin foreign-user actions no longer acquire foreign write locks before scope checks
- targeted regression pack is green

## 13. Handoff
- next lane: Lane 01 and Lane 02 foundation packets
- remaining transitional paths: broader token-storage migration and runtime-policy unification stay for later lane work
- operator or frontend note: no frontend cutover is needed; this is a branch-cleanliness and correctness gate
- compatibility window and wrapper duration: not applicable for this packet
- consumer sign-off needed before cutover: security reviewer plus backend owner before merge
- deprecation or removal cutoff: none
