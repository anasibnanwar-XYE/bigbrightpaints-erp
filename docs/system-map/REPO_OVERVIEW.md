# Repo Overview

Last reviewed: 2026-02-16

Primary runtime areas:
- `erp-domain/`: Spring Boot backend and all module/domain code.
- `scripts/`: verification, guards, migrations, and CI wiring.
- `docs/`: architecture, policy, and execution contracts.
- `testing/`, `artifacts/`: harness and result history.

Top-level module list:
- `modules/accounting`, `admin`, `auth`, `company`, `factory`, `hr`, `inventory`, `invoice`, `production`, `purchasing`, `rbac`, `reports`, `sales`, plus shared cross-module runtime `orchestrator`.
- `docs/system-map/modules/*/FILES.md` contains module file-level maps.

Canonical path and migration policy:
- All future schema work must target `erp-domain/src/main/resources/db/migration_v2`.
- Legacy `db/migration` is historical and not modified for new work.

Duplicate/overlap evidence map (current):
Endpoint duplication:
- Sales dispatch can be initiated through canonical sales flow and legacy orchestrator endpoints (`OrchestratorController.fulfillOrder/dispatchOrder`), creating duplicated orchestration control.
- Partner onboarding (dealer role) is exposed through multiple onboarding-related paths and handlers, with legacy `createDealer` touchpoints that need canonical ownership cleanup.

M18-S3A canonical write-path decision guard (workflow scope):

| Workflow | Canonical write path | Keep decisions | Merge decisions | Deprecate decisions |
|---|---|---|---|---|
| O2C | `POST /api/v1/sales/dispatch/confirm` (`SalesService.confirmDispatch`) | Keep `POST /api/v1/dispatch/confirm` as compatibility alias to canonical dispatch. | Merge orchestrator fulfillment dispatch-status mutation into canonical dispatch truth checks. | Deprecate `POST /api/v1/orchestrator/dispatch*` as write entrypoints. |
| P2P | `purchasing` invoice creation + `accounting` settlement/payment (`/purchasing/raw-material-purchases`, `/accounting/suppliers/payments`, `/accounting/settlements/suppliers`) | Keep manual intake/opening-stock writes for migration/emergency only, with prod-gating and audit expectations. | Merge duplicated settlement/idempotency helper behavior into one accounting-boundary semantic contract. | None declared in this docs lane; deprecation decisions are limited to unsafe bypass endpoints when discovered. |
| Production-to-Pack | `POST /api/v1/factory/production/logs` + canonical packing service boundary (`/factory/packing-records`) | None declared; compatibility is handled via merged alias route only. | Merge `POST /api/v1/factory/pack` into the same canonical packing service path and posting boundary. | Deprecate legacy `POST /api/v1/factory/production-batches` path. |
| Payroll | `POST /api/v1/payroll/runs*` lifecycle under `PayrollService` with accounting payment clearing boundary | Keep accounting payroll payment routes as canonical payment-clearing path (not run creation). | Merge `POST /api/v1/hr/payroll-runs` into canonical payroll run lifecycle semantics. | Deprecate/prod-gate `POST /api/v1/orchestrator/payroll/run` pending proven canonical routing parity. |

File-level overlap candidates:
- `PayrollRunRequest` duplicates exist in `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/hr/dto/PayrollRunRequest.java` and `erp-domain/src/main/java/com/bigbrightpaints/erp/orchestrator/dto/PayrollRunRequest.java`.
- `ResetPasswordRequest` and `ForgotPasswordRequest` appear in both `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/auth/dto/` and `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/auth/web/`.
- Governance guidance is split across multiple AGENTS files (`accounting`, `sales`, `inventory`, `hr`) with different update cadences and overlap risk.

Method-level overlap candidates:
- `applyIdempotencyKey` is repeated in controllers in `modules/accounting`, `modules/factory`, `modules/inventory`, `modules/purchasing`.
- `resolveIdempotencyKey` and `dispatchOrder` are implemented both in `SalesService` and `OrchestratorController`.
- `postSalesJournal` and `postCogsJournal` cross module boundaries (`SalesJournalService`, `SalesService`, `AccountingFacade`), with duplicate posting pathways.
- `normalizeIdempotencyKey` is implemented in `modules/purchasing`, `modules/inventory`, `modules/production` helpers with semantically similar behavior.

Prioritized cleanup queue from map evidence:
1. **Decision execution proofs (high)**: add/maintain alias parity and fail-closed tests for each published keep/merge/deprecate workflow decision.
2. **Flow overlap cleanups (high)**: reconcile sales dispatch/dealer onboarding; retire deprecated orchestrator-only execution routes.
3. **Journal overlap control (high)**: consolidate `postSalesJournal`/`postCogsJournal` ownership in canonical `SalesFulfillmentService` + `AccountingFacade` edge.
4. **Schema overlap risk (high)**: finish V2 convergence migrations and keep v1 legacy untouched except compatibility.
5. **Idempotency helper debt (medium)**: centralize helper behavior so request-key canonicalization and partner settlement key handling are single-source and auditable.
6. **DTO/API overlap (medium)**: deduplicate `PayrollRunRequest` and auth reset/forgot request models behind module boundary contracts.

Residual risks:
- Any duplicate paths listed as deprecated remain temporarily usable for compatibility and should be guarded by strict contract tests until fully retired.
- Some test-only legacy overlaps are intentional for backward-compatibility verification.
