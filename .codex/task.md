# ERP Full-Stack Backend Stabilization, Deduplication & Audit (Spring Boot) — HEAVY AUTONOMOUS WORKFLOW

## 0) Mission
You are an autonomous senior architect/SWE + auditor working on a Java Spring Boot ERP backend. The system is messy: inconsistent naming, duplicate endpoints and logic, scattered “micro-modules,” and heavy Flyway migration duplication. Your job is to:
- Make the application **workable and deployable** in its current scope
- Perform a **deep audit** of all modules and cross-module flows
- Identify and remove/merge **duplicates** (endpoints, services, DTOs, repositories, queries, Flyway migrations)
- Stabilize with **heavy verification** (build/test/boot, smoke/integration, DB migrations)
- Produce professional **architecture + API documentation**
- Keep changes practical: fix and consolidate; do not add features beyond what is necessary to make existing flows correct and consistent

This ERP has major product areas on the frontend:
- **ADMIN** (superuser / all-access)
- **ACCOUNTING** (should cover reports, purchasing, inventory, HR — but code may be scattered)
- **FACTORY** (combine/align with production)
- **SALES**
- **DEALERS**

The codebase may contain additional smaller modules/packages; your task includes rationalizing them into these major product areas where feasible, without breaking behavior.

---

## 1) Non-negotiable Success Criteria
The session is successful only if ALL the following are true:

### Build & Boot
- Project builds cleanly: `mvn test` (or equivalent) runs successfully OR a created smoke suite passes consistently
- Artifact packages: `mvn package` produces runnable output
- Application boots locally with a clean, fresh database and reaches “healthy” state

### Database & Flyway
- Flyway migrations validate cleanly on a fresh DB
- A safe strategy is documented for environments where Flyway has already run (no reckless history rewrites)

### Functional Stability (High-level)
- Endpoints and flows across ADMIN, ACCOUNTING, FACTORY/PRODUCTION, SALES, DEALERS are connected and functionally consistent
- Duplicate endpoints do not diverge in behavior; there is a canonical implementation per concept

### Documentation & Audit Deliverables
- Clean OpenAPI docs present and grouped by major modules
- Professional audit outputs exist (see “Required Output Files”)

---

## 2) One-time Questions (Ask Once Only)
Ask these questions ONCE at the start. If no response, proceed with best-guess defaults and record assumptions in `STABILIZATION_LOG.md`.

1) Database engine and version? (Postgres/MySQL/etc.)
2) Java version target? (17/21/etc.)
3) Spring Boot version? (detect if unknown)
4) Has Flyway been applied in any real env (staging/prod)? (unknown => assume YES and behave safely)
5) Auth method in use? (JWT/session/basic)
6) Which flows are must-not-break? (if unknown => treat ALL major modules as must-not-break)

Defaults if unanswered:
- Postgres 14, Java 17
- Flyway has been used in at least one environment => do NOT rewrite applied migrations
- JWT or session as detected
- All module flows considered critical

---

## 3) Operating Rules (Strict)
### Safety & Backward Compatibility
- Do not rename/remove public endpoints unless absolutely necessary. Prefer:
  - Choose a canonical endpoint + canonical service
  - Keep old endpoints as aliases delegating to canonical logic
  - Mark old endpoints as deprecated in OpenAPI
- Do not delete code unless proven unused by:
  - reference search + Spring wiring scan + runtime/endpoint inventory + tests/smoke verification
- Avoid huge refactors early. Stabilize first; then consolidate duplicates.

### Accounting Correctness
- Any accounting/ledger/posting logic must preserve invariants:
  - debit/credit consistency where applicable
  - transactional safety (`@Transactional` boundaries)
  - idempotency safeguards if external references exist
- Add tests around invariants whenever touching accounting logic.

### Audit Discipline
- For every bug or duplicate cluster, write:
  - Symptom
  - Root cause
  - Fix
  - Validation evidence (test run, repro command, logs)

---

## 4) Required Output Files (Create/Update)
Create these in `docs/` (create directory if missing). Keep them updated as you progress.

1) `docs/STABILIZATION_LOG.md`
   - chronological log: commands run, failures found, fixes, validation results, remaining risks

2) `docs/ARCHITECTURE_REVIEW.md`
   - actual module boundaries found in code
   - recommended mapping into major modules:
     - ADMIN
     - ACCOUNTING (incl. reports/purchasing/inventory/HR wherever they exist)
     - FACTORY + PRODUCTION (combined)
     - SALES
     - DEALERS
   - identify problematic coupling and duplication patterns

3) `docs/MODULE_FLOW_MAP.md`
   - end-to-end flows per major module
   - cross-module connections (e.g., Sales -> Accounting posting)
   - key entities and lifecycle transitions

4) `docs/DUPLICATES_REPORT.md`
   - duplicates found and how consolidated:
     - endpoints
     - services
     - DTOs
     - repositories/queries
     - Flyway scripts
   - canonical choices and alias/deprecation notes

5) `docs/FLYWAY_AUDIT_AND_STRATEGY.md`
   - migration inventory and duplicate/conflict analysis
   - fresh DB procedure
   - existing DB safe strategy (baseline/repair rules)
   - what you changed and why

6) `docs/API_NOTES.md`
   - canonical endpoint list grouped by major modules
   - deprecated/alias endpoints
   - naming conventions and response/error standards

7) `docs/DEPLOY_CHECKLIST.md`
   - required env vars (no secrets in repo)
   - profiles (dev/stage/prod)
   - migration steps
   - health checks
   - log/metrics expectations

---

## 5) Tooling & Verification Requirements
Detect build tool and wrappers:
- Prefer `./mvnw` if present; else `mvn`
- Use `./gradlew` if Gradle project

Minimum required command loops:
- `mvn test` (or gradle test)
- `mvn package`
- boot app and confirm health
- validate Flyway on fresh DB (via app startup and/or plugin)

Verification principles:
- After each substantial change: run tests
- Before merging duplicate endpoints: add/confirm tests
- Track all validations in `STABILIZATION_LOG.md`

---

## 6) PHASED HEAVY WORKFLOW (Do not skip; do not ignore smaller modules)

### Phase A — Baseline Snapshot & Inventories (No refactors)
Goal: produce a complete system map.

A1) Repository inventory:
- list modules/packages
- detect sub-modules that belong to major modules (ADMIN/ACCOUNTING/FACTORY+PRODUCTION/SALES/DEALERS)

A2) Endpoint inventory (must be complete):
- list every endpoint: METHOD + PATH -> controller#method -> request/response types
- group endpoints by functional area (even if packages are wrong)
- detect duplicates:
  - same path+method duplicates
  - same semantics with different paths
  - different semantics on same concept name (e.g., multiple “ledger” endpoints behaving differently)

A3) Domain & persistence inventory:
- list JPA entities per package
- map relationships and table names
- detect duplicate/overlapping entities (same table represented multiple ways, multiple “Ledger” models, etc.)
- list repositories and custom queries; detect duplicates and conflicting query logic

A4) Flyway inventory (very detailed):
- list all migrations in order (version, description, checksum)
- detect duplicates:
  - duplicate versions
  - duplicate schema operations
  - conflicting constraints/indexes
- identify “production risk” patterns (renumbering hazards)

A5) Create initial docs:
- `ARCHITECTURE_REVIEW.md` (initial map + issues)
- `MODULE_FLOW_MAP.md` (initial flow guesses)
- `DUPLICATES_REPORT.md` (initial list)
- `FLYWAY_AUDIT_AND_STRATEGY.md` (initial findings)

---

### Phase B — Build-to-Green & Boot Reliability
Goal: compile/package and boot reliably on fresh DB.

B1) Build:
- run tests/build; capture failures
- fix compile errors, missing dependencies, broken bean wiring
- stabilize configuration loading (env vars; safe defaults; no hardcoded secrets)

B2) Boot:
- boot the app; fix startup failures
- ensure health endpoint exists (actuator preferred if already in dependencies; otherwise minimal health controller)
- verify database connection and Flyway migration on fresh DB

B3) Record:
- every failure and fix in `STABILIZATION_LOG.md`

---

### Phase C — Module-by-Module Deep Audit (ALL major modules + small modules)
You must do a deep audit per major module and attach smaller modules to a major module.

For each major module:
- ADMIN
- ACCOUNTING (includes: reports, purchasing, inventory, HR wherever found)
- FACTORY + PRODUCTION (treat these as one combined domain area)
- SALES
- DEALERS

Perform this checklist and document in `ARCHITECTURE_REVIEW.md` and `MODULE_FLOW_MAP.md`:

C1) Entry points:
- list endpoints by capability
- identify duplicates, inconsistencies, missing validations

C2) Domain model:
- key entities, table mapping, lifecycle
- detect duplicates/conflicts and resolve into a canonical domain model when safe

C3) Service layer:
- list use-cases
- detect divergence and duplication; pick canonical logic

C4) Persistence:
- evaluate repositories and queries for overlap, correctness, and transactional integrity

C5) Cross-module connections:
- document dependencies (e.g., Sales -> Accounting postings; Factory -> Inventory; Dealers -> Receivables)

C6) Security boundaries:
- ADMIN has all access
- verify role/permission checks exist and are consistent across modules
- document gaps (and fix critical ones if causing breakage)

C7) Error response consistency:
- define a minimal consistent error format (without breaking existing clients)
- standardize only where it removes confusion and bugs

C8) Tests gap analysis:
- list existing tests
- identify missing critical tests per module flow

---

### Phase D — Stabilization & Dedupe Execution (System-wide)
Goal: eliminate duplicates that cause bugs and inconsistent behavior.

D1) Endpoint canonicalization:
For each duplicate cluster:
- decide canonical endpoint(s) and canonical service method
- refactor duplicates to delegate to canonical service
- keep aliases for backward compatibility
- update OpenAPI: mark deprecated endpoints

D2) Service/DTO/entity canonicalization:
- consolidate duplicate DTOs and mapping logic
- eliminate multiple implementations of same business rule
- ensure transaction boundaries are correct
- reduce redundant repositories/queries

D3) Unused code removal (safe only):
- remove unused code only if proven unused + validated by tests/boot

Update `DUPLICATES_REPORT.md` continuously:
- what was duplicated
- canonical replacement
- what aliases remain
- behavior notes

---

### Phase E — Accounting Hardening (Deep)
Goal: make accounting consistent and professional.

E1) Identify ALL Accounting areas (even if scattered):
- ledger
- journal entries/postings
- vouchers
- purchasing
- inventory valuation signals (if present)
- HR and reports (if present)
- reporting endpoints

E2) Ledger/posting duplication resolution:
- gather all ledger endpoints; classify behaviors
- pick canonical rules based on most consistent/used path
- ensure posting logic is single-source-of-truth

E3) Add invariants tests:
- debit/credit checks
- posting idempotency where references exist
- transactional integrity tests for multi-step postings

E4) Reports consistency:
- ensure reports read from canonical sources and reconcile with postings

---

### Phase F — FACTORY + PRODUCTION consolidation (Deep)
Goal: treat Factory and Production as one consistent domain.

F1) Identify production-related entities and endpoints:
- batches, work orders, BOMs, inventory movements, production status, etc. as present

F2) Consolidate:
- move/organize packages logically (minimal change; prefer delegation over mass renames)
- unify duplicate “production” logic under Factory+Production canonical services

F3) Verify cross-module links:
- production impacts inventory/accounting where applicable

---

### Phase G — SALES and DEALERS stabilization (Deep)
Goal: ensure sales and dealers flows are consistent and connect correctly.

G1) Sales flow verification:
- order -> invoice -> payment/receivable signals (as present)
- ensure any accounting hooks (posting) are consistent

G2) Dealers flow verification:
- dealer creation/management
- dealer-linked sales/receivables (as present)
- validate authorization (ADMIN overrides; others restricted)

G3) Dedupe endpoints and logic similarly to prior phases.

---

### Phase H — Flyway Cleanup & Safe Strategy (90+ migrations) (Very Deep)
Goal: migrations safe on fresh DB + documented strategy for existing DBs.

H1) Categorize migration issues:
- duplicate versions
- duplicate operations
- conflicting constraints/indexes
- risky edits to already-applied migrations

H2) Safe remediation:
- If migrations likely applied anywhere: do NOT rewrite applied files
- Prefer:
  - `repair` strategy notes
  - additive “forward fix” migrations
  - consolidation only for migrations that are provably unused/unapplied

H3) Verify:
- clean migrate on fresh DB
- validate checksums as appropriate

Document fully in `FLYWAY_AUDIT_AND_STRATEGY.md`.

---

### Phase I — Heavy Verification Suite (Mandatory)
Goal: ensure we did not miss connectivity issues.

I1) Create or enhance automated tests:
- at minimum: integration tests per major module that call canonical endpoints
- ensure tests run against a test database

I2) Add smoke tests/scripts:
- if automated tests are lacking, add a script or documented curl/Postman-like sequence in docs
- must validate cross-module hooks:
  - Sales -> Accounting posting (if present)
  - Factory/Production -> Inventory/Accounting (if present)
  - Dealer -> Sales/Receivable (if present)
  - Admin can access all major endpoints

I3) Repeat-run requirement:
- run the full test suite twice to avoid flaky “first-run” passing
- record results

---

### Phase J — OpenAPI & Documentation Polish
Goal: professional API documentation and module grouping.

J1) Ensure OpenAPI available (springdoc preferred).
J2) Group endpoints by module tags:
- ADMIN
- ACCOUNTING
- FACTORY_PRODUCTION
- SALES
- DEALERS
J3) Document canonical endpoints and mark deprecated aliases.
Update `API_NOTES.md`.

---

### Phase K — Deploy Readiness Package
Goal: deployable with clear instructions.

K1) `DEPLOY_CHECKLIST.md` must include:
- required env vars
- DB setup
- Flyway steps
- profiles and startup commands
- health checks
- logs

K2) Final verification:
- `mvn test`
- `mvn package`
- boot app with prod profile (as far as possible without secrets)
- confirm health endpoint

---

## 7) Final Deliverable Summary (Write in STABILIZATION_LOG.md)
- What was broken and fixed (by module)
- Canonical implementations chosen (why)
- Duplicate clusters resolved
- Flyway status and strategy
- How to deploy
- Remaining risks requiring human decisions (if any)

---

## 8) Execution Discipline
- Commit frequently with clear messages:
  - `chore: baseline inventories`
  - `fix: boot wiring and config`
  - `refactor: canonical ledger service and alias endpoints`
  - `fix: flyway forward migration to resolve duplicate constraint`
  - `test: add integration smoke for sales->accounting posting`
  - `docs: openapi grouping and deploy checklist`
- Never do large unrelated changes in one commit.
- Always attach validation evidence in `STABILIZATION_LOG.md`.
 ## Non-Interactive Mode (STRICT)
You must NOT stop for questions. If any input is missing:
- Determine it yourself from repo/config by inspecting files and running safe commands.
- If still unknown, proceed with the safest defaults and continue.
- Record assumptions in docs/STABILIZATION_LOG.md, but do NOT pause.

### Defaults to use if unknown
- DB: Postgres (use Testcontainers for verification if local DB not available)
- Flyway: assume applied somewhere -> NEVER rewrite existing migrations; only forward-fix
- Auth: detect via SecurityConfig / filters; if ambiguous, treat endpoints as requiring auth and only validate boot + health + OpenAPI
- Must-not-break flows: ALL major modules (ADMIN, ACCOUNTING, FACTORY+PRODUCTION, SALES, DEALERS)

### Dirty working tree policy (STRICT)
Do not ask. Treat pre-existing modified/untracked files as OUT-OF-SCOPE unless they block build/test/boot.
- Do not delete user files.
- If they affect build/test, commit a snapshot branch or stash-equivalent strategy and proceed.
- 
do not commit anychanges