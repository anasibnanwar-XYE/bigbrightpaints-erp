# R2 Checkpoint

## Scope
- Feature: `ERP-46 Wave 2 integrated hard-cut`
- Branch: `packet/erp-46-wave-2-integration`
- PR: `not opened; local integration worktree proof only`
- Review candidate:
  - keep `POST /api/v1/superadmin/tenants/onboard` as the sole tenant onboarding path
  - keep `/api/v1/catalog/brands`, `/api/v1/catalog/import`, and `/api/v1/catalog/items` as the canonical catalog write surfaces
  - keep `POST /api/v1/accounting/journal-entries` as the sole manual journal write surface and remove legacy dual-header/manual-journal write seams
  - make goods receipt replay fail closed, require eager supplier payable-account loading, and remove purchase-reference prefix fallback in accounting replay
  - delete orphan verification helpers `Task00_async_verify.sh`, `scripts/task00_async_verify.sh`, and `scripts/task00_watch_verify.sh`
  - refresh integrated `openapi.json` and `docs/endpoint-inventory.md` so published contract truth matches merged runtime behavior
- Why this is R2: this branch hard-cuts live write surfaces across tenant onboarding, catalog, accounting, and purchasing/reconciliation. A wrong merge could leave duplicate or dead public routes, stale replay fallback behavior, or broken cross-module reconciliation/accounting flows behind a green packet-local surface.

## Risk Trigger
- Triggered by:
  - `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/company/controller/CompanyController.java`
  - `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/company/service/CompanyService.java`
  - `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/production/controller/CatalogController.java`
  - `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/production/service/ProductionCatalogService.java`
  - `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/controller/AccountingController.java`
  - `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/internal/AccountingFacadeCore.java`
  - `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/purchasing/service/GoodsReceiptService.java`
  - `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/purchasing/domain/SupplierRepository.java`
  - `openapi.json`
  - `docs/endpoint-inventory.md`
- Contract surfaces affected:
  - tenant onboarding and first-admin company binding under `/api/v1/superadmin/tenants/onboard`, `/api/v1/auth/login`, and `/api/v1/auth/me`
  - canonical catalog writes under `/api/v1/catalog/brands`, `/api/v1/catalog/import`, and `/api/v1/catalog/items`
  - canonical accounting manual journal writes under `/api/v1/accounting/journal-entries`
  - purchasing goods receipt replay plus accounting/reconciliation supplier payable-account loading
  - published contract truth in `openapi.json` and `docs/endpoint-inventory.md`
- Failure mode if wrong:
  - deleted or dead public endpoints could remain published beside the canonical write surface
  - catalog/accounting callers could still discover legacy write routes or headers
  - goods receipt replay could silently repair stale state instead of failing closed
  - purchase journal, reconciliation, or period-close flows could regress on supplier payable-account loading
  - OpenAPI and endpoint inventory could drift from the live merged runtime surface

## Approval Authority
- Mode: human
- Approver: `ERP-46 owner`
- Canary owner: `ERP-46 owner`
- Approval status: `pending human review; integrated Wave 2 proof green`
- Basis: this branch changes multiple finance and control-plane write surfaces together, so merge still requires explicit human signoff after integrated proof is green.

## Escalation Decision
- Human escalation required: yes
- Reason: the branch changes business-critical public write contracts plus reconciliation/accounting replay semantics.

## Rollback Owner
- Owner: `ERP-46 owner`
- Rollback method:
  - before merge: drop the integrated branch/worktree
  - after merge: revert the integrated Wave 2 diff as one unit so contract, replay, and helper cleanup changes roll back together
  - never restore docs or OpenAPI without restoring the matching runtime code in the same revert
- Rollback trigger:
  - `DELETE /api/v1/companies/{id}` or `POST /api/v1/accounting/journals/manual` reappears in runtime or published contract
  - a goods receipt replay succeeds when the persisted idempotency hash is missing or mismatched
  - purchase journal, reconciliation, or period-close flows fail because supplier payable-account loading falls back to lazy traversal
  - catalog callers can still access a removed service-only write path

## Expiry
- Valid until: `2026-04-04`
- Re-evaluate if: Wave 2 scope expands, another packet touches these same write surfaces, or reviewer feedback requires broader replay/accounting changes.

## Verification Evidence
- Commands run:
  - `cd /Users/anas/Documents/Factory/bigbrightpaints-erp_worktrees/erp-46-wave-2-integration/erp-domain && MIGRATION_SET=v2 mvn -B -ntp -Djacoco.skip=true -Derp.openapi.snapshot.verify=true -Derp.openapi.snapshot.refresh=true -Dtest=OpenApiSnapshotIT test`
  - `cd /Users/anas/Documents/Factory/bigbrightpaints-erp_worktrees/erp-46-wave-2-integration/erp-domain && MIGRATION_SET=v2 mvn -B -ntp -DskipTests compile`
  - `cd /Users/anas/Documents/Factory/bigbrightpaints-erp_worktrees/erp-46-wave-2-integration/erp-domain && MIGRATION_SET=v2 mvn -B -ntp -Djacoco.skip=true -Derp.openapi.snapshot.verify=true -Dtest=OpenApiSnapshotIT,CompanyControllerIT,TenantOnboardingControllerTest test`
  - `cd /Users/anas/Documents/Factory/bigbrightpaints-erp_worktrees/erp-46-wave-2-integration/erp-domain && mvn -B -ntp -Dtest=CatalogControllerCanonicalProductIT,ProductionCatalogServiceRetryPolicyTest,CR_CatalogImportIdempotencyIT,CR_CatalogImportDeterminismIT,CR_CatalogImportConcurrencyIT,ProductionCatalogRawMaterialInvariantIT,ProductionCatalogFinishedGoodInvariantIT test`
  - `cd /Users/anas/Documents/Factory/bigbrightpaints-erp_worktrees/erp-46-wave-2-integration/erp-domain && mvn -B -ntp -Dtest=AccountingControllerJournalEndpointsTest,AccountingControllerIdempotencyHeaderParityTest,CR_ManualJournalSafetyTest,TS_AccountingControllerIdempotencyHeaderParityRuntimeCoverageTest,TS_RuntimeAccountingReplayConflictExecutableCoverageTest,AccountingFacadeTest,PurchasingServiceGoodsReceiptTest,ReconciliationServiceTest,TS_RuntimeAccountingFacadeExecutableCoverageTest,TS_RuntimeAccountingFacadePeriodCloseBoundaryTest,CR_PurchasingToApAccountingTest,CR_PurchasingGrnPeriodCloseTest,ReconciliationControlsIT,InventoryGlReconciliationIT,PeriodCloseLockIT test`
  - `cd /Users/anas/Documents/Factory/bigbrightpaints-erp_worktrees/erp-46-wave-2-integration && bash scripts/gate_fast.sh`
  - `cd /Users/anas/Documents/Factory/bigbrightpaints-erp_worktrees/erp-46-wave-2-integration && bash scripts/gate_core.sh`
- Result summary:
  - `MIGRATION_SET=v2 mvn -B -ntp -DskipTests compile` passed with `BUILD SUCCESS`
  - integrated OpenAPI refresh passed with `BUILD SUCCESS`; canonical snapshot is `openapi.json` sha256 `54d64bd865903ef5a42e3828c8ff244b8326bd010c9bffe14bd29b6930842970` with `277` paths and `329` operations
  - retired routes `DELETE /api/v1/companies/{id}` and `POST /api/v1/accounting/journals/manual` are absent from the canonical OpenAPI surface
  - targeted onboarding/OpenAPI proof passed with `Tests run: 18, Failures: 0, Errors: 0, Skipped: 0`
  - targeted catalog proof passed with `Tests run: 46, Failures: 0, Errors: 0, Skipped: 0`
  - targeted accounting/purchasing/reconciliation proof passed with `Tests run: 131, Failures: 0, Errors: 0, Skipped: 0`
  - `bash scripts/gate_fast.sh` passed with `[gate-fast] OK`
  - `bash scripts/gate_core.sh` passed with `[gate-core] OK`
  - gate scripts emitted only non-blocking canonical-base warnings before resolving against `origin/harness-engineering-orchestrator`
- Artifacts/links:
  - repo checkout: `/Users/anas/Documents/Factory/bigbrightpaints-erp_worktrees/erp-46-wave-2-integration`
  - module path: `/Users/anas/Documents/Factory/bigbrightpaints-erp_worktrees/erp-46-wave-2-integration/erp-domain`
  - `gate_fast` artifacts: `/Users/anas/Documents/Factory/bigbrightpaints-erp_worktrees/erp-46-wave-2-integration/artifacts/gate-fast`
  - `gate_core` artifacts: `/Users/anas/Documents/Factory/bigbrightpaints-erp_worktrees/erp-46-wave-2-integration/artifacts/gate-core`
