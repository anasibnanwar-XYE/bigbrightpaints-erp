# Documentation Conventions

Last reviewed: 2026-05-05

This document defines the writing conventions, cross-link expectations, and stale-doc handling policy for the orchestrator-erp backend documentation library.

---

## 1) Source of Truth Rules

### 1.1 Code is the primary truth source

Documentation must be grounded in the actual implementation. The evidence hierarchy is:

1. Controller annotations and DTOs for route/payload truth.
2. Service/facade/engine code for lifecycle and side-effect truth.
3. Events/listeners and config for hidden coupling and runtime gating.
4. Tests for executed/guarded behavior.
5. Existing docs only as secondary inputs to preserve or retire.

### 1.2 Do not invent behavior

- If a capability is implemented, document it as implemented.
- If a capability is planned but not yet built, label it as **pending**, **planned**, or **future work** — never as done.
- If a capability is partially built, document what exists today and explicitly note what is incomplete.

### 1.3 No duplicate truth

- Each truth concern should be owned by exactly one canonical document.
- Module documents own structural truth.
- Flow documents own behavioral truth.
- Frontend portal/API documents own consumer framing.
- ADRs own decision truth.
- If a change needs to reference another document's truth, it should link to it rather than duplicate it.

## 2) Writing Style

### 2.1 Truth-first writing

- Describe the system as it is today, not as it should be.
- When behavior is partial, confusing, or controlled by configuration rather than hard enforcement, say so explicitly.
- Preserve the distinction between module ownership and flow ownership — they often differ.

### 2.2 Backend + frontend readable

- Docs should be readable by both backend and frontend engineers.
- Use clear headings, tables, and bullet lists.
- Avoid jargon that only makes sense to one team without explanation.

### 2.3 Explicit about limitations

- If something is partial, dead-end, retired, or ambiguous, the docs must say so clearly.
- Use callout sections for known gaps, retired boundaries, and configuration-guarded behavior.

## 3) Implemented vs Planned Language

### 3.1 Status labeling

When writing or updating docs, use explicit status labels:

- **Implemented** — the behavior exists in the codebase today and is covered by tests. No special label is needed; the absence of a qualifier implies implemented.
- **Pending** / **Planned** / **Future work** — the behavior is designed or discussed but not yet built. Always label explicitly.
- **Partial** — the behavior is partially built. Document what exists today and explicitly note what is incomplete.
- **Deprecated** — the behavior was once canonical but has been replaced. Always point to the replacement.
- **No replacement** — the behavior was retired and nothing replaces it. State this explicitly so readers know it was an intentional retirement rather than an accidental gap.

### 3.2 Avoid aspirational prose

- Do not describe the system as it *should* be. Describe it as it *is*.
- Do not document a future improvement as though it is already in place.
- If a surface is controlled by a feature toggle or configuration flag, say so explicitly and note the default posture.

### 3.3 Caveat discipline

- When a behavior has a known gap, edge case, or fail-open posture, document it as a **caveat** or **known limitation** rather than hiding it or treating it as a temporary oversight.
- Caveats should be specific: name the affected path, the expected behavior, and the actual behavior.

## 4) Cross-Link Expectations

### 4.1 Every document links to related documents

- Module documents link to relevant flow documents, ADRs, and frontend handoff documents.
- Flow documents link back to their owning module documents and related ADRs.
- Frontend portal/API documents link back to canonical module and flow docs.
- ADRs link to the module/flow documents they affect.
- Retired behavior notes link to their canonical replacement or state explicitly that no replacement exists.

### 4.2 Navigation is bidirectional

- If module A links to flow B, then flow B should link back to module A.
- The root index (`docs/INDEX.md`) is the top-level entrypoint and must link to all major sections.

### 4.3 Link format

- Use relative paths for internal links within the docs tree.
- Use `[text](path)` markdown format.
- Verify links exist when editing docs.

## 5) Freshness Markers

### 5.1 Required marker

Every markdown file in the canonical docs tree must carry a `Last reviewed: YYYY-MM-DD` marker near the top of the file. This is enforced by `ci/lint-knowledgebase.sh`.

### 5.2 Updating markers

- When you edit a canonical docs file, update the `Last reviewed:` date to the current date.
- The date must be a valid ISO calendar date and must not be in the future.

## 6) Stale-Doc Handling Policy

### 6.1 When docs become stale

A doc is stale when:

- It describes behavior that has changed in the implementation.
- It references endpoints, DTOs, or services that no longer exist.
- It contradicts the current canonical documentation.

### 6.2 Handling stale docs

- **Delete:** If a stale doc is replaced by a canonical change and no guard or runtime contract consumes it, remove the stale doc.
- **Rename:** If the doc is still enforced by a guard, give it a current-purpose name rather than carrying archive-era vocabulary.
- **Align:** If a stale doc still owns active behavior, update it in place and keep it reachable from `docs/INDEX.md`.
- **Do not keep archive-only docs by default.** Historical value belongs in git history unless the current maintenance workflow needs the file.

### 6.3 Competing truth is not allowed

- No two active documents may claim to own the same truth.
- If a new document replaces an old one, the old document must be deleted or renamed in the same change unless an active guard requires it.

## 7) Docs-Only Lane

Docs-only changes are limited to the canonical docs/governance lane:

- repo-root signposts/governance: `README.md`, `AGENTS.md`, `ARCHITECTURE.md`, `CHANGELOG.md`
- canonical docs spine files: `docs/INDEX.md`, `docs/ARCHITECTURE.md`, `docs/CONVENTIONS.md`, `docs/SECURITY.md`, `docs/RELIABILITY.md`, `docs/BACKEND-FEATURE-CATALOG.md`, `docs/RECOMMENDATIONS.md`
- canonical directories: `docs/adrs/**`, `docs/agents/**`, `docs/approvals/**`, `docs/modules/**`, `docs/flows/**`, `docs/frontend-api/**`, `docs/frontend-portals/**`

Markdown outside that lane is **not** docs-only. That includes `docs/platform/**`, `docs/runbooks/**`, root worklogs/reports, and any mixed markdown-plus-code/config/test/script/OpenAPI diff.

Docs-only changes:

- run `bash ci/lint-knowledgebase.sh` only
- skip manual review/subagent review
- skip runtime validators and service startup
- must not change backend runtime behavior

## Cross-references

- [docs/INDEX.md](INDEX.md) — canonical documentation index
- [docs/ARCHITECTURE.md](ARCHITECTURE.md) — architecture reference
- [docs/RELIABILITY.md](RELIABILITY.md) — reliability posture
