# Orchestrator Next Step Plan

Date: 2026-02-16
Ticket Anchor: `TKT-ERP-STAGE-001`

## Planning Inputs Used
- Primary batch source: `docs/system-map/Goal/ERP_STAGING_MASTER_PLAN.md`
- Control-plane sequencing: `docs/system-map/Goal/ENTERPRISE_BACKEND_STABILIZATION_PLAN.md`
- Deep invariants and strict-lane review stack: `docs/system-map/Goal/ERP_ENTERPRISE_DEPLOYMENT_DEEP_SPEC.md`

## Execution Rules (Locked)
- R1 and R2 are orchestrator-owned with proof.
- R3 is human-only for irreversible production actions.
- Module `scope_paths` enforcement is mandatory at verify/merge.
- Reviewer agents are review-only and do not commit.
- Cross-slice overlap across different primary agents blocks merge until consolidated.

## Batch 1 (Active)
| Slice | Master Plan Ref | Primary Agent | Mandatory Reviewers | Scope |
| --- | --- | --- | --- | --- |
| SLICE-01 | M18-S2 | auth-rbac-company | qa-reliability, security-governance | `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/auth/`, `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/rbac/`, `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/company/` |
| SLICE-02 | M18-S8 | sales-domain | qa-reliability, security-governance | `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/sales/`, `erp-domain/src/test/java/com/bigbrightpaints/erp/modules/sales/` |

## Next Batches (Orchestrator Sequence)
| Batch | Target | Candidate Primary Agents | Mandatory Reviewers | Required Minimum Checks |
| --- | --- | --- | --- | --- |
| Batch 2 | M18-S2A superadmin control plane (metrics, quotas, hold/block runtime enforcement) | auth-rbac-company, reports-admin-portal | security-governance, qa-reliability | `bash ci/check-architecture.sh`, `bash ci/check-enterprise-policy.sh`, targeted auth/admin tests, `bash scripts/verify_local.sh` |
| Batch 3 | M18-S3 O2C/P2P/production/payroll duplicate-path census and closure decisions | sales-domain, purchasing-invoice-p2p, factory-production, hr-domain, accounting-domain | qa-reliability, security-governance | targeted module tests, reconciliation-linked tests where posting is touched, `bash ci/check-architecture.sh`, `bash scripts/verify_local.sh` |
| Batch 4 | M18-S4 + M18-S5 approval/override hardening and split-payment idempotency race closure | sales-domain, purchasing-invoice-p2p, accounting-domain | qa-reliability, security-governance | targeted settlement/approval tests, `bash ci/check-enterprise-policy.sh`, `bash scripts/gate_reconciliation.sh` |
| Batch 5 | M18-S6 + M18-S9 GST/non-GST + API/OpenAPI/front-end parity gates | accounting-domain, reports-admin-portal, repo-cartographer, release-ops | qa-reliability, security-governance | tax/reconciliation tests, OpenAPI drift check, `bash ci/lint-knowledgebase.sh`, `bash scripts/gate_core.sh` |
| Batch 6 | M18-S10 staging rehearsal and rollback proof pack closure | release-ops, data-migration, orchestrator | qa-reliability, security-governance | `bash scripts/gate_fast.sh`, `bash scripts/gate_core.sh`, `bash scripts/gate_reconciliation.sh`, `bash scripts/gate_release.sh` |

## Operational Cadence
1. Execute one batch at a time with strict-lane evidence and reviewer outcomes captured in ticket artifacts and `asyncloop`.
2. Advance only after required checks pass, scope/overlap verification passes, and orchestrator pre-merge review is recorded.
3. Keep final staging closure blocked until all queued code commits have reviewer outcomes and ledger gate evidence is attached to one release SHA.
