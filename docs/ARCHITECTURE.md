# Architecture Specification - orchestrator_erp

Last reviewed: 2026-02-16

This is the canonical architecture specification referenced by root `ARCHITECTURE.md`.

## Inter-Module Dependency Rules

- Module-to-module imports must remain allowlisted in `ci/architecture/module-import-allowlist.txt`.
- Any new dependency edge must preserve bounded-context ownership and be justified with ADR evidence.
- Cross-module behavior changes must follow contract-first sequencing:
  - contracts/events/interfaces first
  - producer changes second
  - consumer changes third
  - orchestrator integration last
- Scope boundaries from `agents/*.agent.yaml` are merge-gating constraints for implementation slices.

## Governance and Enforcement

- Architecture boundaries are mechanically enforced by:
  - `bash ci/check-architecture.sh`
  - `bash ci/check-enterprise-policy.sh`
  - `bash ci/check-orchestrator-layer.sh`
- Allowlist updates require proof notes:
  - why needed
  - alternatives rejected
  - boundary preserved

## DB Touchpoints
- JPA repositories: `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/**/domain/*Repository.java`
- Migration roots:
  - legacy: `erp-domain/src/main/resources/db/migration`
  - active: `erp-domain/src/main/resources/db/migration_v2`
- Migration policy references:
  - `erp-domain/docs/FLYWAY_AUDIT_AND_STRATEGY.md`
  - `docs/runbooks/migrations.md`
- Predeploy data guards:
  - `scripts/flyway_overlap_scan.sh`
  - `scripts/schema_drift_scan.sh`
  - `scripts/run_db_predeploy_scans.sh`

## Canonical References

- Agent routing and ownership: `agents/orchestrator-layer.yaml`, `agents/catalog.yaml`
- Workflow and merge controls: `docs/agents/WORKFLOW.md`
- Permissions model: `docs/agents/PERMISSIONS.md`
- Master staging plan: `docs/system-map/Goal/ERP_STAGING_MASTER_PLAN.md`
