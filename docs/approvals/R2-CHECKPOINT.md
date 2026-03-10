# R2 Checkpoint

## Scope
- Feature: `recovery-review.corrections-control-migration-closure`
- Follow-up packet: `recovery-followup.corrections-control-comment-recheck-and-closure`
- Branch: `recovery/05-corrections-control`
- High-risk paths touched: accounting controller/core services, sales return service, purchase return service, `db/migration_v2/V161__manual_journal_attachments_and_closed_period_exceptions.sql`, and the focused accounting/control regression suites; the latest follow-up narrows to `SalesReturnService`, `AccountingCoreEngineCore`, and their focused regression tests.
- Why this is R2: the recovery packet closes PR #99 by hardening correction provenance, stabilizing header-level settlement replay on one canonical key path, failing closed on unsupported legacy return state, and carrying the non-destructive corrections/control schema change with explicit governance evidence. The latest follow-up keeps the same high-risk accounting scope while isolating keyed sales-return replay relinks from legacy unkeyed line references and ensuring closed-period reversal/void override rows stay linked to the resulting journals for audit traceability.

## Risk Trigger
- Triggered by edits under `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/`, `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/sales/service/`, `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/purchasing/`, and `erp-domain/src/main/resources/db/migration_v2/`.
- Contract surfaces affected: linked sales returns, linked purchase returns, correction journal provenance, header-level settlement idempotency, supplier lifecycle fail-closed ordering, correction preview endpoints, and accounting period-close blocker checks.
- Main risks being controlled: silent mutation of posted source documents, unlinked correction journals that allow close drift, unsupported marker-less legacy returns being retried through hidden compatibility branches, settlement retries that miss prior header-level allocations, mutable supplier lifecycle blocking legitimate replays, and schema drift around manual-journal attachments plus closed-period exception rows.

## Approval Authority
- Mode: orchestrator
- Approver: ERP truth-stabilization mission orchestration
- Basis: compatibility-preserving accounting remediation within the active mission scope; the packet adds one non-destructive Flyway v2 migration (`V161`) for manual-journal attachment references and closed-period posting exceptions, without privilege expansion or tenant-boundary widening.

## Escalation Decision
- Human escalation required: no
- Reason: the packet tightens existing accounting controls and linkage invariants, keeps one canonical corrections/control replay path with explicit recovery guidance for old local data, and introduces only additive/non-destructive persistence changes.

## Rollback Owner
- Owner: corrections-and-control feature worker
- Rollback method: revert the recovery fix commit, leave `V161` unapplied if rollback happens pre-deploy, or deploy the previous app build and manually drop the additive schema objects from `V161` in the same maintenance window if rollback happens after apply; rerun the targeted correction suites and `MIGRATION_SET=v2 mvn -Pgate-fast -Djacoco.skip=true test` before merge.

## Expiry
- Valid until: 2026-03-13
- Re-evaluate if: additional accounting reversal types, tenant-crossing journal flows, or further migration-backed linkage/idempotency fields are added.

## Verification Evidence
- Verification bundle: enterprise-policy, codex-review-guidelines, targeted accounting replay/correction tests, and gate-fast were rerun for this corrections/control packet.
- Result summary: the recovery packet keeps only canonical `CRN-` sales return references on the live path, relinks correction journals back to posted source entries, reuses stable header-settlement idempotency keys for dealer and supplier retries, lets supplier settlement replay win before later lifecycle suspension, fails closed on unsupported marker-less legacy return movements with documented recovery steps, and now reserves supplier-settlement idempotency with a deterministic placeholder-first mapping so concurrent duplicate submissions without `referenceNumber` replay to the same canonical settlement reference instead of failing with `CONCURRENCY_CONFLICT`.
- Artifacts/links: `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/controller/AccountingController.java`, `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/internal/AccountingCoreEngineCore.java`, `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/sales/service/SalesReturnService.java`, `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/purchasing/service/PurchaseReturnService.java`, `erp-domain/src/main/resources/db/migration_v2/V161__manual_journal_attachments_and_closed_period_exceptions.sql`, `erp-domain/src/test/java/com/bigbrightpaints/erp/modules/accounting/service/AccountingServiceTest.java`, `erp-domain/src/test/java/com/bigbrightpaints/erp/modules/sales/service/SalesReturnServiceTest.java`

## Additional Scope: `recovery-followup.corrections-control-comment-recheck-and-closure`
- Packet target: `recovery/05-corrections-control` rechecked on the latest stacked head.
- High-risk paths touched: `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/sales/service/SalesReturnService.java`, `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/internal/AccountingCoreEngineCore.java`, `erp-domain/src/test/java/com/bigbrightpaints/erp/modules/sales/service/SalesReturnServiceTest.java`, `erp-domain/src/test/java/com/bigbrightpaints/erp/modules/accounting/service/AccountingServiceTest.java`, and this checkpoint.
- Why this is R2: the packet rechecks the newest live PR #99 corrections/control threads and narrows the change to two audit-critical fixes: keyed sales-return replays now relink only the replayed keyed return movements, and closed-period reversal/void flows now keep their one-hour override rows linked to the resulting journal entries.

### Additional Risk Trigger
- Triggered by the newest live PR #99 review threads on keyed sales-return replay relinking and closed-period reversal authorization linkage.
- Contract surfaces affected: sales-return replay relink isolation, reversal/void closed-period override audit linkage, and the focused regression coverage that guards both behaviors.
- Main risks being controlled: replayed keyed returns overwriting legacy unkeyed movement `journalEntryId` values, successful closed-period reversal/void flows leaving their override rows orphaned from the resulting journal record, and accidental widening that would disturb the earlier settlement, period-close, and replay fixes already carried on the branch.

### Additional Approval / Escalation
- Mode: orchestrator.
- Human escalation required: no.
- Basis: the fix is compatibility-preserving within the approved corrections/control scope, adds no migration behavior, and keeps the change limited to replay/audit linkage on the existing sales-return and journal-reversal paths.

### Additional Rollback Owner
- Owner: corrections/control recovery worker.
- Rollback method: revert the feature commit, then rerun `cd erp-domain && MIGRATION_SET=v2 mvn -Djacoco.skip=true -Dtest='AccountingServiceTest,SalesReturnServiceTest,AccountingPeriodServicePolicyTest,PurchaseReturnServiceTest' test`, `cd erp-domain && MIGRATION_SET=v2 mvn -T8 test -Pgate-fast -Djacoco.skip=true`, `bash ci/check-enterprise-policy.sh`, and `bash ci/check-codex-review-guidelines.sh` before re-review.

### Additional Expiry
- Valid until: 2026-03-14.
- Re-evaluate if: later packets change sales-return replay keying again, add new reversal authorization document types, or introduce migration-backed linkage/backfill work for closed-period override rows.

### Additional Verification Evidence
- Commands run: `cd erp-domain && MIGRATION_SET=v2 mvn -Djacoco.skip=true -Dtest='SalesReturnServiceTest#relinkExistingReturnMovements_updatesOnlyKeyedLineScopedReferencesForRequestedReturnKey,SalesReturnServiceTest#salesReturnReferences_handleMissingInvoiceAndMissingReturnKey,AccountingServiceTest#reverseJournalEntry_linksClosedPeriodPostingExceptionUsingReversalAuthorizationKey,AccountingServiceTest#reverseJournalEntry_voidOnlyLinksClosedPeriodPostingExceptionUsingReversalAuthorizationKey' test`; `cd erp-domain && MIGRATION_SET=v2 mvn -Djacoco.skip=true -Dtest='AccountingServiceTest,SalesReturnServiceTest,AccountingPeriodServicePolicyTest,PurchaseReturnServiceTest' test`; `cd erp-domain && MIGRATION_SET=v2 mvn -T8 test -Pgate-fast -Djacoco.skip=true`; `bash ci/check-enterprise-policy.sh`; `bash ci/check-codex-review-guidelines.sh`; `gh pr checks 99 --repo anasibnanwar-XYE/bigbrightpaints-erp || true`.
- Result summary: keyed sales-return replays now relink only the replayed keyed `invoice:line:RET-*` references and leave legacy unkeyed line movements untouched, while reversal/void postings created under a closed-period exception now carry the reversal authorization key through journal creation so the corresponding override row links back to the resulting journal for audit traceability; the broader corrections/control validation suite stays green; and PR #99 checks were re-read on the latest propagated head.
- Artifacts/links: `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/sales/service/SalesReturnService.java`, `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/internal/AccountingCoreEngineCore.java`, `erp-domain/src/test/java/com/bigbrightpaints/erp/modules/sales/service/SalesReturnServiceTest.java`, `erp-domain/src/test/java/com/bigbrightpaints/erp/modules/accounting/service/AccountingServiceTest.java`.
- Migration guidance note: no `db/migration_v2` files changed in this packet, so no migration-runbook update was required.
