# R2 Checkpoint

Last reviewed: 2026-04-02

## Scope
- Feature: `ERP failing-suite remediation — debit-note replay hardening`
- Branch: `erp-failing-suite-remediation-20260401` under the `anas` namespace
- PR: `#180` — https://github.com/anasibnanwar-XYE/bigbrightpaints-erp/pull/180
- Review candidate:
  - harden `postDebitNote(...)` in `AccountingCoreEngineCore` so leader-path replays validate journal provenance before mutating correction metadata
  - persist debit-note provenance early via `sourceModule=DEBIT_NOTE` and `sourceReference=<purchaseInvoiceNumber>` on journal creation
  - expand accounting regression coverage in `AccountingServiceTest`, plus carried AP regression coverage in `ProcureToPayE2ETest` and `HighImpactRegressionIT`
  - update canonical docs for accounting idempotency, accounting reference chains, frontend idempotency/error handling, accounting portal states/errors, and this R2 checkpoint
- Why this is R2: the packet changes executable code under `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/internal/`, which is a high-risk accounting path guarded by enterprise policy.

## Risk Trigger
- Triggered by:
  - `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/internal/AccountingCoreEngineCore.java`
- Contract surfaces affected:
  - debit-note idempotent replay semantics in `postDebitNote(...)`
  - correction-journal provenance fields used for replay truth: `sourceModule`, `sourceReference`, and `reversalOf`
  - frontend-facing conflict contract for cross-purchase reference reuse, as captured in the canonical accounting docs packet
- Failure mode if wrong:
  - a concurrent retry could bind an existing debit-note journal to the wrong purchase, corrupting AP correction provenance and making audit/reference-chain reads point to the wrong source document

## Approval Authority
- Mode: orchestrator
- Approver: Droid mission orchestrator
- Canary owner: Droid mission orchestrator
- Approval status: approved for branch-local remediation
- Basis: the change narrows replay behavior without widening permissions, schema, migrations, or public route surface. Targeted unit/E2E coverage and docs/policy validators provide sufficient pre-merge evidence.

## Escalation Decision
- Human escalation required: no
- Reason: this is a compatibility-preserving correctness fix to accounting replay behavior. It does not alter auth boundaries, tenant scoping, schema, or irreversible data migration behavior, and the risk is bounded by deterministic tests around the corrected race.

## Rollback Owner
- Owner: Droid mission orchestrator
- Rollback method:
  - before merge: reset or revert the branch-local remediation
  - after merge: revert the hardening commit(s) and rerun the accounting regression slice
- Rollback trigger:
  - debit-note replay for the same purchase stops returning the previously posted journal
  - AP correction journals lose correct `sourceReference` / `reversalOf` provenance
  - accounting regression slice or enterprise policy/docs validation regresses after merge

## Expiry
- Valid until: 2026-04-15
- Re-evaluate if: scope expands beyond accounting replay hardening, or if auth/company/RBAC/schema/migration files are added to the packet.

## Test Waiver
- Not applicable — executable code and tests changed, and targeted validators were run.

## Verification Evidence
- Commands run:
  - `cd /Users/anas/Documents/Factory/bigbrightpaints-erp_worktrees/erp-failing-suite-remediation-20260401/erp-domain && mvn --settings ".mvn/settings.xml" -B -ntp -Djacoco.skip=true -Dtest=AccountingServiceTest,ProcureToPayE2ETest,HighImpactRegressionIT test`
  - `cd /Users/anas/Documents/Factory/bigbrightpaints-erp_worktrees/erp-failing-suite-remediation-20260401/erp-domain && mvn --settings ".mvn/settings.xml" -B -ntp -DspotlessFiles=src/main/java/com/bigbrightpaints/erp/modules/accounting/internal/AccountingCoreEngineCore.java,src/test/java/com/bigbrightpaints/erp/modules/accounting/service/AccountingServiceTest.java spotless:check`
  - `bash ci/lint-knowledgebase.sh`
  - `bash scripts/enforce_codex_review_policy.sh`
  - `bash ci/check-enterprise-policy.sh`
- Result summary:
  - targeted accounting unit + E2E slice passes after the provenance hardening
  - Spotless check passes for the edited Java files
  - docs knowledgebase lint passes
  - strict codex review policy passes with the carried non-doc packet
  - enterprise policy requires this R2 checkpoint for the accounting runtime scope and passes once the scope-specific evidence is present
- Artifacts/links:
  - repo checkout: local workspace
  - remote branch: `erp-failing-suite-remediation-20260401` under the `origin` remote and `anas` namespace
