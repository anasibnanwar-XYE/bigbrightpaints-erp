# Docs Validation

Docs-only validation guidance for the backend truth-library mission.

**What belongs here:** docs validator commands, evidence expectations, and full-contract lint notes.

---

## Primary Commands

- `bash ci/lint-knowledgebase.sh`
- `bash ci/check-enterprise-policy.sh`
- `bash ci/check-architecture.sh`
- `bash ci/check-orchestrator-layer.sh`
- `bash scripts/guard_openapi_contract_drift.sh`

## Docs-Only Validation Rules

- Do not start application services for docs-only validation
- Prefer file/link/inventory proof over runtime proof
- Use `openapi.json` as the public API snapshot when checking routes/payload families
- Verify that canonical docs carry `Last reviewed:` markers when required by lint
- When a packet references a deprecated surface, verify it also points to a replacement or explicitly says none exists

## Full-Contract Lint Expectations

`ci/lint-knowledgebase.sh` should pass in full-contract mode by the end of the mission. The docs tree must therefore include the lint-required canonical docs/governance files, not just compatibility-mode files.

### Two-Mode Contract Design

The lint script has a clear two-mode design:

- **Full-contract mode** activates when all 3 legacy-contract marker files exist:
  - `AGENTS.md` (repo root)
  - `docs/INDEX.md`
  - `agents/catalog.yaml`
  In full-contract mode, the script checks 22 required canonical files, freshness markers, canonical links, and broken path references.

- **Compatibility mode** activates if any marker file is missing. It performs a reduced set of checks and emits WARN output.

To troubleshoot lint failures:
1. Check which mode the script runs in (look for `WARN` vs `OK` output prefix).
2. If in compatibility mode, verify the 3 marker files exist.
3. If in full-contract mode, the script output names each missing file or broken link.

### Architecture Allowlist Evidence Contract

`ci/check-architecture.sh` includes an allowlist evidence contract that requires:
1. `docs/ARCHITECTURE.md` (or `docs/architecture.md`) to be updated whenever `ci/architecture/module-import-allowlist.txt` changes.
2. An ADR file (`docs/adr/ADR-*-allowlist-*.md`) with required sections: `## Why Needed`, `## Alternatives Rejected`, `## Boundary Preserved`.

The filename check is case-insensitive to handle platforms where git tracks the architecture doc with different casing.

## Expected Evidence

- exact file paths edited
- exact validator commands run
- sampled internal links checked
- sampled route/host/role claims cross-checked against `openapi.json` and source
- any contradictions or open decisions discovered during documentation work
