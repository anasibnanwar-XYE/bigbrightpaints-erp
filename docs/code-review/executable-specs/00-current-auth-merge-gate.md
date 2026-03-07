# Current Auth Merge Gate Packet

This packet records the now-implemented merge-gate fix and the review evidence required before any base-branch merge recommendation. Lane 01 and Lane 02 should stay blocked on orchestrator review until this packet and its release gate are accepted.

## 1. Header
- lane: merge gate across Lane 01 and Lane 02
- slice name: merge-gate regression closure and release review
- finding IDs: `AUTH-09`, `VAL-MERGE-004`, guardrails `TEN-10`, `ADMIN-14`
- implementer: `Anas Ibn Anwar` (`fix(auth): surface reset persistence failures and preserve revocation ordering`, commit `4ef5f4e1`)
- reviewer: `Factory-droid orchestrator base-branch reviewer`
- QA owner: `Factory-droid merge-gate regression pack owner`
- release approver: `Factory-droid release gate approver`
- branch: `Factory-droid`
- target environment: local `MIGRATION_SET=v2` regression pack plus approved compose-backed `v2` runtime when available

## 2. Lane Start Gate
- preflight review in `00-preflight-review-merged-auth-company-admin-hardening.md` confirmed `AUTH-09` and token-revocation precision as the only remaining open merge-gate regressions on `Factory-droid`
- `TEN-10` and `ADMIN-14` were already closed on `Factory-droid` before this packet and stay in scope only as regression guardrails
- commit `4ef5f4e1` closes the remaining forgot-password persistence-failure and full-precision revocation-ordering defects without widening into token-digest migration, lifecycle redesign, or global-settings governance
- release handling is still blocked on the packet/release-gate review captured here; workers must return to the orchestrator instead of merging

## 3. Why This Slice Exists
- broader remediation cannot start cleanly while the merge gate lacks named ownership, rollback notes, and proof for the last auth regression packet
- the remaining code packet had to close one auth error-handling regression (`AUTH-09`) and one token-revocation ordering defect while re-proving the already-closed `TEN-10` and `ADMIN-14` guardrails
- finishing this governance packet is the last precondition before orchestrator-led base review for Lane 01 and Lane 02 entry

## 4. Scope
- record the exact merge-gate code packet owned by commit `4ef5f4e1`
- attach proof for the forgot-password persistence-failure fix and full-precision token-revocation ordering
- keep `TEN-10` and `ADMIN-14` visible as regression guardrails in the same proof pack
- verify contract parity: `openapi.json` no diff, `.factory/library/frontend-handoff.md` note present, and `docs/frontend-update-v2/**` carries an explicit no-new-shape-change/no-new-frontend-cutover entry
- do not widen into token digest storage migration, admin-user redesign, broader runtime-policy consolidation, or merge handling itself

## 5. Caller Map
- `PasswordResetService`
- `TokenBlacklistService`
- public forgot-password controller path (`POST /api/v1/auth/password/forgot`)
- access-token revocation check path used by auth/session enforcement
- regression guardrail suites for canonical runtime-policy enforcement and masked admin user-control endpoints
- frontend-facing review surfaces: `.factory/library/frontend-handoff.md`, `docs/frontend-update-v2/README.md`, and `docs/frontend-update-v2/merge-gate-release-governance-review.md`

## 6. Invariant Pack
- canonical runtime-policy updates must invalidate policy cache immediately (`TEN-10` stays green)
- public forgot-password must not leak whether a user exists or whether dispatch/configuration masking paths were taken
- public forgot-password must surface reset-token persistence failures as a controlled non-success instead of false-success `200 OK`
- token revocation must preserve full timestamp ordering so earlier same-millisecond tokens are still revoked
- masked tenant-admin foreign-user actions must keep the masked contract and must not reacquire foreign write locks before scope checks (`ADMIN-14` stays green)

## 7. Implemented Slice
1. `PasswordResetService` now separates reset-token persistence failure from masked delivery/configuration outcomes so `AUTH-09` returns a controlled non-success only on storage failure.
2. `TokenBlacklistService` now compares issuance/revocation timestamps at full precision so same-millisecond earlier issuance is still revoked.
3. Regression coverage was updated in `TokenBlacklistServiceTest`, `AuthPasswordResetPublicContractIT`, and `PasswordResetServiceTest`.
4. The packet keeps `TenantRuntimeEnforcementServiceTest` and `AdminUserSecurityIT` in the proof pack so the already-closed `TEN-10` and `ADMIN-14` boundaries remain explicit.
5. This governance packet adds the release note, rollback note, and parity references required before base-branch review.

## 8. Proof Pack
- targeted merge-gate regression pack:
  - `cd /home/realnigga/Desktop/Mission-control/erp-domain && MIGRATION_SET=v2 mvn test -Djacoco.skip=true -pl . '-Dtest=TokenBlacklistServiceTest,AuthPasswordResetPublicContractIT,AdminUserServiceTest,AdminUserSecurityIT,TenantRuntimeEnforcementServiceTest,TS_RuntimeTenantPolicyControlExecutableCoverageTest'`
- full gate-fast regression confidence:
  - `cd /home/realnigga/Desktop/Mission-control/erp-domain && MIGRATION_SET=v2 mvn -T8 test -Pgate-fast -Djacoco.skip=true`
- packet-shape / parity review:
  - `git diff -- openapi.json .factory/library/frontend-handoff.md docs/frontend-update-v2 docs/code-review/executable-specs`
  - `git show --stat --name-only --format=fuller 4ef5f4e1`
- runtime evidence note:
  - approved runtime probe attempted in this governance session returned `000;000` because the compose-backed runtime was not running locally; this is recorded in the release gate as degraded runtime evidence and not used to waive test proof
- release-gate artifact:
  - `00-current-auth-merge-gate-release-gate.md`

## 9. Validation-First Evidence
- not a validation-first packet
- packet boundary and open-regression classification are already recorded in `00-preflight-review-merged-auth-company-admin-hardening.md`
- reviewer sign-off for the packet verdict is deferred to orchestrator base-branch review because workers do not merge or push

## 10. Rollback Pack
- revert commit `4ef5f4e1` first; do not roll back unrelated baseline/preflight packets unless the regression pack shows wider fallout
- no schema or data migration is part of this packet, so rollback is code-only
- keep the masked forgot-password and masked admin contracts stable during rollback; only the persistence-failure and revocation-ordering behavior should move
- rollback trigger threshold: any regression that reintroduces false-success forgot-password masking, loses revocation ordering, weakens tenant masking, or breaks canonical runtime-policy enforcement
- rollback rehearsal evidence: rerun the targeted merge-gate pack after reverting `4ef5f4e1` if a rollback is proposed during review
- expected RTO: under 1 hour for code-only rollback plus proof rerun
- expected RPO: none

## 11. Stop Rule
- split the slice immediately if token schema changes, lifecycle model changes, global-settings governance, or broader admin/auth behavior cleanup starts entering the same review packet

## 12. Exit Gate
- canonical company runtime-policy updates still invalidate cache immediately
- public forgot-password still masks delivery semantics correctly but no longer hides token-persistence failures
- token revocation preserves full timestamp ordering
- masked tenant-admin foreign-user actions still avoid foreign write locks before scope checks
- targeted merge-gate pack and full gate-fast are green
- release roles, rollback notes, and contract-parity evidence are present in this packet and `00-current-auth-merge-gate-release-gate.md`

## 13. Handoff
- next lane: orchestrator base-branch review, then Lane 01 and Lane 02 foundation packets after review acceptance
- remaining transitional paths: broader token-storage migration and runtime-policy unification stay for later lane work
- operator or frontend note: no new frontend cutover is needed; the only frontend-relevant behavior remains the already-tracked forgot-password controlled non-success path, with explicit no-new-shape-change evidence in `.factory/library/frontend-handoff.md` and `docs/frontend-update-v2/merge-gate-release-governance-review.md`
- compatibility window and wrapper duration: not applicable for this packet
- consumer sign-off needed before cutover: orchestrator base-branch review plus backend/security review before any merge recommendation
- deprecation or removal cutoff: none
