# MERGE-SPECIALIST RECOVERY HANDOFF

- Ticket: `TKT-ERP-STAGE-113`
- Review mode: merge-specialist integration integrity review only (no merge/rebase/push executed)
- Reviewed at (UTC): `2026-02-26T15:36:26Z`
- Integration base branch: `tickets/tkt-erp-stage-113/blocker-remediation-orchestrator`
- Integration base SHA: `63b3daaf92075b3ca2d7a188d20e4ec5756250b2`

## Reviewed Recovery Heads

1. `B08` `tickets/tkt-erp-stage-113/b08-auth-secret-hardening-recovery` @ `a81f2f8045310e27d4248416884d2c47d1b52ba0`
2. `B09` `tickets/tkt-erp-stage-113/b09-orchestrator-correlation-sanitization-recovery` @ `3a4ea699c3831dccb33d57024bcb0192f9bcb196`
3. `B14` `tickets/tkt-erp-stage-113/b14-verifylocal-bash32-portability-recovery` @ `c75e07d4093e5f006b7f7525162240e4547e0ad6`

## Required Check Evidence

Executed for each recovery SHA against base `63b3daaf`:

- `git diff --name-status <base>..<sha>`
- `git diff --check <base>..<sha>`
- conflict-marker scans:
  - `git diff <base>..<sha> | rg -n "^[+-](<{7}|={7}|>{7})"`
  - `git grep -nE "<<<<<<<|=======|>>>>>>>" <sha> -- $(git diff --name-only <base>..<sha>)`
- dropped/overwritten hunk integrity:
  - `git range-diff <base>..<original-branch> <base>..<recovery-branch>`
  - stable patch-id parity checks (original functional commit vs recovery functional commit)
- cross-branch conflict readiness:
  - `git merge-tree <base> <shaA> <shaB>` for `(B08,B09)`, `(B08,B14)`, `(B09,B14)`

Result summary:

- `B08`: `git diff --check` PASS, conflict-marker scans PASS
- `B09`: `git diff --check` FAIL (trailing whitespace in review markdown files), conflict-marker scans PASS
- `B14`: `git diff --check` PASS, conflict-marker scans PASS
- `merge-tree` conflict markers: none across all recovery pairings
- direct file overlap across branch diffs: none

## Branch Assessments

## B08 - `a81f2f8045310e27d4248416884d2c47d1b52ba0`

### Scope alignment (blocker fit)

Observed runtime scope:
- `erp-domain/src/main/java/com/bigbrightpaints/erp/core/security/JwtProperties.java`
- `erp-domain/src/main/resources/application-{dev,mock,benchmark,openapi}.yml`
- initializer/bootstrap paths in `erp-domain/src/main/java/com/bigbrightpaints/erp/core/config/*`
- `erp-domain/src/main/java/com/bigbrightpaints/erp/ErpDomainApplication.java` (hardcoded dev seed removal)
- security/bootstrap tests under `erp-domain/src/test/java/com/bigbrightpaints/erp/core/{config,security}/`

Blocker alignment: functionally aligned to B08 secret/credential hardening intent (predictable secret removal, env-backed secrets, fail-closed bootstrap checks), but broader than the matrix's narrow auth/company path wording due `core/config` + app bootstrap edits.

### Contract and coherence verification

- API/event route contracts: unchanged.
- Startup/security contract changed to stricter fail-closed behavior for non-test JWT secret handling and bootstrap admin seeding.
- Cross-module coherence: no direct overlap/conflict with B09/B14 touched files.

### Dependency/coupling hygiene

- Centralized secret validation in `JwtProperties`; no new external dependencies introduced.
- Bootstrap behavior now depends on explicit env provision in non-test contexts; downstream dev/bootstrap workflows must satisfy env contract.

### Observability impact

- Added explicit startup log messages for skipped seed paths and test-profile secret fallback.
- Removed hardcoded user credential emission path from app bootstrap.

### Dropped-hunk/overwrite verification

- `range-diff` maps original B08 functional commit to recovery functional commit.
- Stable patch-id parity confirmed for core functional commit:
  - original `d7a52780` patch-id == recovery `6f29dcc7` patch-id (`51047e295714cab078c554edaee3ef2cf9f5de6a`)
- Historical recovery conflict was documented in branch evidence (`tickets/TKT-ERP-STAGE-113/TIMELINE.md` during cherry-pick), not in current merge simulation.

### Decision

`BLOCK`

### Required prerequisites before merge

1. Re-run blocker-mandated diff-wide coverage/gate evidence on the current recovery head lineage and attach passing output on same reviewed SHA lineage:
   - `DIFF_BASE=tickets/tkt-erp-stage-113/blocker-remediation-orchestrator bash scripts/gate_fast.sh` (or equivalent approved baseline)
   - `python3 scripts/changed_files_coverage.py --diff-base tickets/tkt-erp-stage-113/blocker-remediation-orchestrator --jacoco erp-domain/target/site/jacoco/jacoco.xml`
2. Record explicit scope-waiver note for `core/config` + `ErpDomainApplication` deltas as B08-necessary hardening, or trim scope if waiver is not accepted.

### Residual integration risks

- Dev/bootstrap startup behavior is stricter; local bootstrap flows relying on implicit defaults may fail until env contract is updated.
- Evidence currently shows earlier changed-files coverage miss; passing rerun on final reviewed lineage is required for D4 confidence.

## B09 - `3a4ea699c3831dccb33d57024bcb0192f9bcb196`

### Scope alignment (blocker fit)

Observed runtime scope is orchestrator-focused and blocker-aligned:
- `OrchestratorController`, `CommandDispatcher`, `IntegrationCoordinator`, `TraceService`, `OrchestratorIdempotencyService`, `EventPublisherService`
- new sanitizer utility `CorrelationIdentifierSanitizer`
- guard contract update `scripts/guard_orchestrator_correlation_contract.sh`
- orchestrator IT/unit/truthsuite coverage updates

Blocker alignment: strong for B09 correlation/idempotency sanitization and fail-closed validation at ingress/sink boundaries.

### Contract and coherence verification

- Correlation/idempotency contract is now strict (character class + length enforcement).
- Malformed `orderId` flows now fail-closed before side effects (behavioral tightening in orchestrator paths).
- No API route additions/removals; request validation semantics are stricter.

### Dependency/coupling hygiene

- Sanitization logic centralized in one utility and consumed consistently across controller/service/persistence surfaces.
- Guard script updated to enforce sanitizer usage in critical paths.
- Potential downstream compatibility risk for callers sending previously tolerated malformed correlation identifiers.

### Observability impact

- Logging now uses sanitized/fingerprinted values (`invalid#...`) instead of raw malformed identifiers in parse-failure paths.
- Improves audit/log poisoning resistance.

### Dropped-hunk/overwrite verification

- `range-diff` preserves original B09 functional commit and adds recovery-only remediation commits.
- Stable patch-id parity confirmed for core functional commit:
  - original `993fe53f` patch-id == recovery `406c8329` patch-id (`44e7730010dd13149a61dd6726002c6ef8df1007`)

### Decision

`BLOCK`

### Required prerequisites before merge

1. Fix `git diff --check` failures (trailing whitespace) in:
   - `tickets/TKT-ERP-STAGE-113/reports/reviews/B09-recovery-code-review-1.md`
   - `tickets/TKT-ERP-STAGE-113/reports/reviews/B09-recovery-code-review-2.md`
   - `tickets/TKT-ERP-STAGE-113/reports/reviews/B09-recovery-security-review-1.md`
   - `tickets/TKT-ERP-STAGE-113/reports/reviews/B09-recovery-security-review-2.md`
   - `tickets/TKT-ERP-STAGE-113/reports/reviews/B09-recovery-security-review-3.md`
2. Re-run and capture a clean `git diff --check` result on final head after whitespace cleanup.

### Residual integration risks

- Stricter identifier/order validation can surface client-side compatibility issues where previously permissive values were accepted.
- Recommended to keep B12 dependency planning aware that B09 now enforces stricter fail-closed behavior at orchestration ingress.

## B14 - `c75e07d4093e5f006b7f7525162240e4547e0ad6`

### Scope alignment (blocker fit)

Observed scope is tightly blocker-aligned:
- `scripts/verify_local.sh`
- flyway guard scripts + fixture matrix scripts
- shell compatibility sourcing via existing `scripts/bash_compat.sh` / `scripts/bash_env_bootstrap.sh`

### Contract and coherence verification

- No API/event/domain contract changes.
- Local verification shell portability contract is improved for Bash 3.2 by ensuring compatibility bootstrap is consistently sourced.

### Dependency/coupling hygiene

- Reuses existing compatibility helper scripts; no external dependency changes.
- Guard fixture updates explicitly enforce presence/use of compatibility helper.

### Observability impact

- Contract guard wrapper now logs effective env propagation in test harness checks, improving diagnosability.

### Dropped-hunk/overwrite verification

- `range-diff` preserves original B14 functional commit and adds recovery evidence commits.
- Stable patch-id parity confirmed for core functional commit:
  - original `7aa45ecc` patch-id == recovery `ce506c89` patch-id (`945cb972787db1ea6be5450efd4c379ecbf2ffe5`)

### Decision

`GO`

### Required prerequisites before merge

- None blocking from merge-specialist integrity standpoint.

### Residual integration risks

- Evidence was executed with `VERIFY_LOCAL_SKIP_TESTS=true`; full heavy test lanes remain QA/release-ops responsibility.
- Cross-platform behavior beyond local Bash 3.2 remains a standard downstream QA concern, not a merge blocker.

## Cross-Branch Ordering Risks

- Pairwise merge simulation (`git merge-tree`) found no textual conflicts among B08/B09/B14.
- Changed-file overlap among the three recovery diffs is empty.
- Main ordering risks are semantic/gate readiness, not file-level conflicts.

## Final Recommended Merge Order

Recommended sequence after branch-specific prerequisites are satisfied:

1. `B14` (`c75e07d4`) - currently `GO`, unlocks portability baseline for local verification chain.
2. `B08` (`a81f2f80`) - merge after D4 evidence/prereq closure.
3. `B09` (`3a4ea699`) - merge after whitespace cleanup and clean `git diff --check` rerun.

Order note:
- `B08` and `B09` are file-independent in this base and can be swapped once both are unblocked; the listed order keeps wave-0 auth/runtime hardening sequence conservative.
