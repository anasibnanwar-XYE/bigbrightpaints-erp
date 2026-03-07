# Merge-gate release gate

- packet: [`00-current-auth-merge-gate.md`](./00-current-auth-merge-gate.md)
- base branch: `Factory-droid`
- packet code commit: `4ef5f4e1` (`fix(auth): surface reset persistence failures and preserve revocation ordering`)
- implementer: `Anas Ibn Anwar`
- reviewer: `Factory-droid orchestrator base-branch reviewer`
- QA owner: `Factory-droid merge-gate regression pack owner`
- release approver: `Factory-droid release gate approver`
- status: review-ready, return to orchestrator before any merge recommendation

## Must-pass checks

- Exit gate from `00-current-auth-merge-gate.md` is satisfied by the targeted merge-gate pack and the green `gate-fast` rerun.
- changed-files proof for the packet code commit is limited to:
  - `erp-domain/src/main/java/com/bigbrightpaints/erp/core/security/TokenBlacklistService.java`
  - `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/auth/service/PasswordResetService.java`
  - `erp-domain/src/test/java/com/bigbrightpaints/erp/core/security/TokenBlacklistServiceTest.java`
  - `erp-domain/src/test/java/com/bigbrightpaints/erp/modules/auth/AuthPasswordResetPublicContractIT.java`
  - `erp-domain/src/test/java/com/bigbrightpaints/erp/modules/auth/service/PasswordResetServiceTest.java`
- `TEN-10` and `ADMIN-14` remain guardrails in the same proof pack; this packet does not reopen runtime-policy or masked-admin lock-scope drift.
- `openapi.json` has no diff for this packet review.
- frontend parity is explicitly recorded in:
  - `.factory/library/frontend-handoff.md`
  - `docs/frontend-update-v2/README.md`
  - `docs/frontend-update-v2/merge-gate-release-governance-review.md`

## Commands / evidence

1. `cd /home/realnigga/Desktop/Mission-control/erp-domain && MIGRATION_SET=v2 mvn test -Djacoco.skip=true -pl . '-Dtest=TokenBlacklistServiceTest,AuthPasswordResetPublicContractIT,AdminUserServiceTest,AdminUserSecurityIT,TenantRuntimeEnforcementServiceTest,TS_RuntimeTenantPolicyControlExecutableCoverageTest'`
   - observed in this governance session after packet edits: `BUILD SUCCESS`, `Tests run: 105, Failures: 0, Errors: 0, Skipped: 0`.
2. `cd /home/realnigga/Desktop/Mission-control/erp-domain && MIGRATION_SET=v2 mvn -T8 test -Pgate-fast -Djacoco.skip=true`
   - observed in this governance session before packet edits: `BUILD SUCCESS`, `Tests run: 395, Failures: 0, Errors: 0, Skipped: 0`.
3. `git show --stat --name-only --format=fuller 4ef5f4e1`
   - observed in this governance session: only the two production files plus three targeted regression tests changed in the merge-gate code packet.
4. `git diff -- openapi.json .factory/library/frontend-handoff.md docs/frontend-update-v2 docs/code-review/executable-specs`
   - used to verify packet/release-gate/front-end parity evidence before review handoff.
5. `bash /home/realnigga/Desktop/Mission-control/ci/lint-knowledgebase.sh && bash /home/realnigga/Desktop/Mission-control/ci/check-architecture.sh && bash /home/realnigga/Desktop/Mission-control/ci/check-enterprise-policy.sh && bash /home/realnigga/Desktop/Mission-control/ci/check-orchestrator-layer.sh && python3 /home/realnigga/Desktop/Mission-control/scripts/check_flaky_tags.py --tests-root /home/realnigga/Desktop/Mission-control/erp-domain/src/test/java --gate gate-fast && bash /home/realnigga/Desktop/Mission-control/scripts/guard_openapi_contract_drift.sh`
   - observed in this governance session after packet edits: all validators exited successfully; knowledgebase/openapi guards reported compatibility-mode warnings only because optional legacy inventory docs are absent.

## Data and migration controls

- no schema or migration files changed
- no forward-only data action exists in this packet
- rollback is code-only: revert `4ef5f4e1` first and rerun the targeted merge-gate pack before any re-promotion
- expected RTO: under 1 hour
- expected RPO: none

## Runtime evidence

- management probe attempted during this governance session: `000`
- user-facing `/api/v1/auth/me` probe attempted during this governance session: `000`
- interpretation: approved compose-backed runtime was not running locally when the release gate was assembled, so runtime evidence is degraded/unavailable in-session
- waiver rule applied correctly: this degraded runtime evidence is recorded only as a confidence note and does **not** waive the targeted regression pack or `gate-fast` proof

## Frontend and operator controls

- no new request-body or success-response shape change was introduced by the release-governance packet itself
- the previously shipped forgot-password controlled non-success behavior remains the only frontend-relevant merge-gate delta and is already tracked as review-only follow-up
- no route removal, wrapper cutoff, or frontend cutover is required before base-branch review
- consumer sign-off required before merge recommendation: orchestrator base-branch review plus backend/security review

## No-go check

- no-go conditions currently blocked by process, not by proof: workers must return to the orchestrator for base-branch review before any merge recommendation
