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

## Canonical References

- Agent routing and ownership: `agents/orchestrator-layer.yaml`, `agents/catalog.yaml`
- Workflow and merge controls: `docs/agents/WORKFLOW.md`
- Permissions model: `docs/agents/PERMISSIONS.md`
- Master staging plan: `docs/system-map/Goal/ERP_STAGING_MASTER_PLAN.md`
