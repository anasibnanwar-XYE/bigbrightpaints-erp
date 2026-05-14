# orchestrator-erp — Backend Documentation Index

Last reviewed: 2026-05-09

This is the canonical entrypoint for backend documentation. Every major docs section is linked from here. If a change is not reachable through this index, it is not part of the canonical docs tree.

`README.md` and repo-root [`ARCHITECTURE.md`](../ARCHITECTURE.md) are signposts into this spine. Public runtime and deployment truth lives under `docs/`.

---

## Repo-Root Entry Points

| Document | Purpose |
| --- | --- |
| [README.md](../README.md) | Repository overview and setup entrypoint that routes readers into the canonical docs spine |
| [ARCHITECTURE.md](../ARCHITECTURE.md) | Repo-root architecture signpost; use [`docs/ARCHITECTURE.md`](ARCHITECTURE.md) for the full runtime architecture reference |

## Architecture and Platform

| Document | Purpose |
| --- | --- |
| [docs/ARCHITECTURE.md](ARCHITECTURE.md) | Runtime architecture, module map, cross-module boundaries, data model, security, and event contracts |
| [docs/RELIABILITY.md](RELIABILITY.md) | Reliability posture: idempotency patterns, retry/dead-letter handling, outbox guarantees, and known safety gaps |
| [docs/SECURITY.md](SECURITY.md) | Security controls, high-risk change classes, and R2 approval workflow |
| [docs/ci-cd-contract.md](ci-cd-contract.md) | CI/CD lane contract: PR ship-safety checks, main/release/quality lanes, and blocker classification |
| [docs/CONVENTIONS.md](CONVENTIONS.md) | Truth-first writing rules, cross-link expectations, implemented-vs-planned language, and stale-doc handling policy |
| [docs/platform/db-migration.md](platform/db-migration.md) | Persistence technology, schema areas, entity/repository conventions, Flyway v2 migration posture, profile activation, and data-import entry surfaces |
| [docs/platform/config-feature-toggles.md](platform/config-feature-toggles.md) | High-impact platform settings and feature toggles: security, licensing, mail/notification, export-approval, module/runtime gating, integration, accounting-event, inventory, orchestrator, seed, and benchmark switches with scope and default caveats |
| [docs/platform/health-readiness-gating.md](platform/health-readiness-gating.md) | Operator-facing health and readiness endpoints, integration-health surfaces, module-gating mechanics, runtime-admission gates, and caveats around which checks to trust |

## Quick Reference: Backend Feature Catalog

| Document | Purpose |
| --- | --- |
| [docs/BACKEND-FEATURE-CATALOG.md](BACKEND-FEATURE-CATALOG.md) | Reader-friendly summary of the complete backend feature landscape with links to deeper module, flow, and ADR documents |

## Authoritative Recommendations

| Document | Purpose |
| --- | --- |
| [docs/RECOMMENDATIONS.md](RECOMMENDATIONS.md) | **Canonical recommendations register** — single authoritative surface for user-approved verdicts on formerly open items from flow docs and module documents. Classifies each item as Bug to Fix Now, Future Work (high/medium/low priority), or Accepted Product Decision. All open-decision sections in flow/module documents should defer to this register. |

## Modules

Module documents explain what each module owns: controllers, services, DTOs, entities, helpers, events, and cross-module boundaries. Canonical module documents are listed below:

| Module | Description |
| --- | --- |
| [accounting](../erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/AGENTS.md) | Journals, ledgers, settlements, period controls, reconciliation, and imports |
| [admin/portal/rbac](modules/admin-portal-rbac.md) | Admin user management, system settings, changelog, export approvals, support tickets, portal insights/finance, dealer self-service host ownership, role-action matrices, and RBAC enforcement |
| [auth](modules/auth.md) | Login, refresh, logout, MFA, password reset, must-change-password corridor, token/session revocation, JWT-based tenant scoping, and security filter chain |
| [company](modules/company.md) | Tenant lifecycle, runtime admission, module gating, licensing checks, tenant onboarding, super-admin control plane, and company-scoping assumptions |
| [core security/error](modules/core-security-error.md) | Security filter chain (JWT, company context, must-change-password corridor), exception/error contract (`ApplicationException`, `ErrorCode`, global handlers), fail-open vs fail-closed boundaries — first slice of the three-part core platform contract |
| [core audit/runtime/settings](modules/core-audit-runtime-settings.md) | Audit-surface ownership (platform audit, enterprise audit trail, accounting event store), runtime-gating split (three enforcement layers), global-versus-tenant settings risk — second slice of the three-part core platform contract |
| [core idempotency](modules/core-idempotency.md) | Shared idempotency infrastructure (key normalization, reservation, signature building), module-local idempotency implementations, contract inconsistencies, and the reconciled core platform contract reference — third/integrating slice |
| [orchestrator](modules/orchestrator.md) | Background coordination: outbox publishing, command dispatch, Spring event bridges, schedulers, retry/dead-letter behavior, and feature flags |
| [hr](modules/hr.md) | Employees, leave, attendance, payroll runs, and payroll posting/payment |
| [inventory](modules/inventory-stock-control.md) | Stock truth boundary: stock summaries, batches, adjustments, opening stock import, valuation, traceability, dispatch execution, and inventory–accounting event bridge |
| [factory/manufacturing](modules/factory.md) | Manufacturing execution: production logs, packing, packaging mappings, batch registration, cost allocation, dispatch handoff boundary, and replay/config caveats |
| [sales](modules/sales.md) | Dealer/customer management, order lifecycle, credit controls, dispatch coordination, dealer self-service, and canonical O2C path |
| [production/catalog](modules/catalog-setup.md) | Catalog and setup readiness: brands, items, import, SKU readiness evaluation, packaging-material definitions, payload families, and setup prerequisites for downstream flows |
| [purchasing/procure-to-pay](modules/purchasing.md) | Supplier lifecycle, purchase orders, goods receipt (GRN), purchase invoices, purchase returns, supplier settlements, and explicit stock-truth vs AP-truth boundaries |

## Flows

Flow documents explain cross-module behavior: actors, entrypoints, preconditions, lifecycle, completion boundary, and current limitations. The [backend feature catalog](BACKEND-FEATURE-CATALOG.md) summarizes the live flow families and links to the detailed flow documents.

## Architecture Decision Records (ADRs)

ADRs explain accepted current decisions already embodied by the backend — why the architecture looks the way it does today.

| Document | Purpose |
| --- | --- |
| [docs/adrs/INDEX.md](adrs/INDEX.md) | ADR index: lists all accepted architecture and product decisions with links to individual ADR files |

The seeded ADR set covers multi-tenant auth scoping (ADR-002), outbox/idempotency strategy (ADR-003), audit layering (ADR-004), migration posture (ADR-005), and portal/host boundaries (ADR-006).

## Canonical Frontend Documentation

> **This is the canonical source for frontend contracts.**

| Document | Purpose |
| --- | --- |
| [docs/frontend/roles-and-permissions.md](frontend/roles-and-permissions.md) | **Frontend role map** — portal entry, role abilities, forbidden actions, shared permission UX rules |
| [docs/frontend/api-contract.md](frontend/api-contract.md) | **Compact current API contract** — auth bootstrap, tenant scope, endpoint ownership, error/idempotency rules, request examples |
| [docs/frontend/workflows.md](frontend/workflows.md) | **Cross-portal workflow map** — bootstrap, onboarding, user management, O2C, P2P, production, period close, support and approvals |
| [docs/frontend/removed-legacy-behavior.md](frontend/removed-legacy-behavior.md) | **Hard-cut frontend boundary** — legacy routes, scope assumptions, workflow aliases, and compatibility UX that must not be rebuilt |
| [docs/frontend-portals/README.md](frontend-portals/README.md) | **Canonical portal ownership map** — six portal shells (superadmin, tenant-admin, accounting, sales, factory, dealer-client), each with routes, API contracts, workflows, role-boundaries, states-and-errors, and playwright-journeys |
| [docs/frontend-api/README.md](frontend-api/README.md) | **Canonical shared API contracts** — bootstrap rules (`GET /api/v1/auth/me` as sole entry), tenant scoping (`companyCode`), retired-route warnings, and shared topic files (auth/company-scope, pagination/filters, exports/approvals, idempotency/errors, accounting-reference-chains, dto-examples) |

### Portal Details

| Portal | Folder | Contents |
| --- | --- | --- |
| Superadmin | [docs/frontend-portals/superadmin/](frontend-portals/superadmin/) | Control-plane ownership, cross-tenant operations |
| Tenant Admin | [docs/frontend-portals/tenant-admin/](frontend-portals/tenant-admin/) | Tenant-scoped admin, export approvals, reporting |
| Accounting | [docs/frontend-portals/accounting/](frontend-portals/accounting/) | Financial reports, period controls, journal access |
| Sales | [docs/frontend-portals/sales/](frontend-portals/sales/) | Order management, dealer management, credit controls |
| Factory | [docs/frontend-portals/factory/](frontend-portals/factory/) | Production logs, packing, dispatch execution |
| Dealer Client | [docs/frontend-portals/dealer-client/](frontend-portals/dealer-client/) | Self-service portal, own orders/invoices/ledger |

## Governance and Agents

| Document | Purpose |
| --- | --- |
| [docs/agents/CATALOG.md](agents/CATALOG.md) | Agent/role catalog: responsibilities and required evidence before handoff |
| [docs/agents/PERMISSIONS.md](agents/PERMISSIONS.md) | Agent permission boundaries: what each role may and must not do |
| [docs/agents/WORKFLOW.md](agents/WORKFLOW.md) | Review and remediation workflow: change types, review ordering, and merge-readiness gates |
| [docs/agents/ENTERPRISE_MODE.md](agents/ENTERPRISE_MODE.md) | Enterprise policy mode: high-risk change detection, R2 triggers, and escalation rules |
| [docs/agents/ORCHESTRATION_LAYER.md](agents/ORCHESTRATION_LAYER.md) | Orchestration layer governance: outbox, event, scheduler, and background-coordination boundaries |
| [ACCOUNTING_PORTAL_SCOPE_GUARDRAIL.md](ACCOUNTING_PORTAL_SCOPE_GUARDRAIL.md) | Mandatory accounting-portal scope guardrail used by `scripts/guard_accounting_portal_scope_contract.sh` |
| [AUDIT_TRAIL_OWNERSHIP.md](AUDIT_TRAIL_OWNERSHIP.md) | Mandatory audit de-dup/change-control contract used by `scripts/guard_audit_trail_ownership_contract.sh` |
| [openapi-endpoint-contract.md](openapi-endpoint-contract.md) | Human-readable OpenAPI endpoint contract used by `scripts/guard_openapi_contract_drift.sh` |

## Approvals

| Document | Purpose |
| --- | --- |
| [docs/approvals/R2-CHECKPOINT.md](approvals/R2-CHECKPOINT.md) | Active R2 checkpoint evidence for the current high-risk document |
| [docs/approvals/R2-CHECKPOINT-TEMPLATE.md](approvals/R2-CHECKPOINT-TEMPLATE.md) | Template for creating new R2 checkpoints |

## Runbooks

| Document | Purpose |
| --- | --- |
| [docs/runbooks/rollback.md](runbooks/rollback.md) | Rollback procedures for applied migrations and coordinated app/schema cuts |
| [docs/runbooks/migrations.md](runbooks/migrations.md) | Migration forward plans, dry-run commands, and rollback strategies |
