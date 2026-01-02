# Stabilization Log

## 2026-01-02
- Scope: baseline inventory, dedupe identification, OpenAPI tagging, alias deprecations.
- Assumptions (defaults if unknown): Postgres 14+, Flyway previously applied in at least one env, JWT auth, all major modules are critical.
- Detected: Java 21 (pom.xml), Spring Boot 3.3.4 (pom.xml), JWT auth (SecurityConfig), Flyway enabled.
- Commands run:
  - `find src/main/java -maxdepth 4 -type d`
  - `rg --files -g '*Controller.java' src/main/java`
  - `rg -n "@Entity" src/main/java`
  - `rg -n "extends JpaRepository" src/main/java`
  - `ls src/main/resources/db/migration`
  - `python3` inventory scripts for endpoint list and Flyway duplicate detection
  - `mvn test`
  - `mvn package`
- Artifacts created:
  - `docs/endpoint_inventory.tsv` (endpoint inventory snapshot)
- Findings logged:
  - Duplicate/alias endpoints (dealers, HR payroll runs, orchestrator dispatch alias).
  - Duplicate Flyway table/index creation guarded by `IF NOT EXISTS` / DO blocks.
  - Payroll DTO duplication (service-level vs dto package) noted for follow-up.
- Code changes:
  - Added OpenAPI tagging customizer to group endpoints by major modules.
  - Marked legacy alias endpoints as deprecated for OpenAPI visibility.
- Validation:
  - `mvn test` failed: `JAVA_HOME` not set (Java runtime not available).
  - `mvn package` failed: `JAVA_HOME` not set (Java runtime not available).
  - Boot/migration validation blocked until Java is installed/configured.
- Risks/unknowns:
  - Tests rely on Testcontainers (Docker required).
  - RabbitMQ/Kafka/SMTP endpoints may be required for full boot beyond health.
  - Existing DBs may already have Flyway applied; strategy documented in `docs/FLYWAY_AUDIT_AND_STRATEGY.md`.

## Final Deliverable Summary (Draft)
- Broken/fixed: no runtime fixes validated yet due to missing Java runtime.
- Canonical implementations chosen:
  - Dealers: `/api/v1/dealers` (alias `/api/v1/sales/dealers` deprecated).
  - Payroll runs: `/api/v1/payroll/runs` (legacy `/api/v1/hr/payroll-runs` deprecated).
  - Orchestrator dispatch: `/api/v1/orchestrator/dispatch` (alias with `{orderId}` deprecated).
- Duplicate clusters resolved: alias endpoints marked deprecated; OpenAPI tags added by module for consistent grouping.
- Flyway status: 91 migrations; duplicate table/index creations are guarded; no rewrites performed.
- How to deploy: see `docs/DEPLOY_CHECKLIST.md`; requires Java 21 and configured DB credentials.
- Remaining risks: tests/build/boot blocked until Java runtime is installed; Docker required for Testcontainers suites.
