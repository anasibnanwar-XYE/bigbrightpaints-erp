# Agent Permissions

Last reviewed: 2026-04-02

## Role Boundaries

| Role | May do | Must not do |
| --- | --- | --- |
| Orchestrator | Approve change readiness, coordinate stacked review order, decide whether human escalation is needed | Bypass required change evidence or approve widened scope without updating governance artifacts |
| Document-governance worker | Edit docs/governance artifacts, assemble document/release-gate evidence, run policy validators | Push, merge, rewrite history, or widen into unrelated product-code work |
| Implementation worker | Change in-scope code/tests, refresh change evidence tied to the feature, run required validators | Skip change controls for high-risk paths or mix lanes in one document |
| Review-only validator/reviewer | Audit diffs, run review checks, report findings | Commit product-code changes or act as merge approver |

## Required Approval Rules

- Review-only agents do not merge or push changes.
- Docs-only governance documents may skip manual governance review/subagent review only when every changed file stays inside the canonical docs/governance lane (`README.md`, `AGENTS.md`, `ARCHITECTURE.md`, `CHANGELOG.md`, `docs/INDEX.md`, `docs/ARCHITECTURE.md`, `docs/CONVENTIONS.md`, `docs/SECURITY.md`, `docs/RELIABILITY.md`, `docs/BACKEND-FEATURE-CATALOG.md`, `docs/RECOMMENDATIONS.md`, `docs/adrs/**`, `docs/agents/**`, `docs/approvals/**`, `docs/modules/**`, `docs/flows/**`, `docs/frontend-api/**`, `docs/frontend-portals/**`) and `bash ci/lint-knowledgebase.sh` passes.
- High-risk changes must update `docs/approvals/R2-CHECKPOINT.md` and preserve rollback ownership before review is considered complete.
