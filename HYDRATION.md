# HYDRATION

## Completed Epics
- Epic 03: branch `epic-03-production-stock`, tip `3f2370c38c0152153369507159e5ae26ca1fa048`.
- Epic 04: branch `epic-04-p2p-ap`, tip `c5dd42334a397b1137d821bd81f50b1504debca4`.
- Epic 05: branch `epic-05-hire-to-pay`, tip `dd1589c00634f9a122ebc9d35caf5114ada1f561`.
- Epic 06: branch `epic-06-admin-security`, tip `dabaeebc8de027491f0974050032bb86afbee5cc`.
- Epic 07: branch `epic-07-performance-scalability`, tip `96c0c71c0d751f3767cfbfb43e970842da9112b5`.
- Epic 08: branch `epic-08-reconciliation-controls`, tip `afe04b5561d9d6510d61bce58640da2dfbec5010`.
- Epic 09: branch `epic-09-operational-readiness`, tip `ca3851aea88ca5b791e65b896a1419a741283c49`.
- Epic 10: branch `epic-10-cross-module-traceability`, tip `c94755d70bcb5ba452ae64ddd7d8a6b96b50d392`.
- Audit investigation 07-08: branch `audit-inv-07-08`, commits `63352c5592b3f1d6c62c40f72e5b17d41803d0c1` (task-07), `381473579213e070e9b9ddfe8dee52012492e1a9` (task-08).
- Audit investigation 01-02: branch `audit-inv-01-02`, commits `2972890f8af382e3a17dcdf9378b87de9664ced4` (task-01), `a0dfdf97a372fa63be015c9db5fec95e39ccea39` (task-02), `edb3fd8a0fa08cbdf3b7ad93a0fd6570bc3ad1df` (task-02 evidence), `225c16a2b4fd3616d9ee1a1208450332d1f0269e` (task-01 evidence).
- Audit investigation 03: branch `audit-inv-03-evidence-and-inventory`, commit `7abdc72d039d7f5d7fbaab6639bf2ee11aa72759` (LEAD-010/011 evidence + task-03 probes).

## Repo / Worktree State
- Worktree: `/home/realnigga/Desktop/CLI_BACKEND_epic04`
- Branch: `audit-inv-03-evidence-and-inventory`
- Tip: `7abdc72d039d7f5d7fbaab6639bf2ee11aa72759`
- Dirty: untracked logs present under `docs/ops_and_debug/LOGS` + `interview/` (pre-existing) and other untracked workspace artifacts.

## Environment Setup
- No new installs; Docker/Testcontainers working.

## Commands Run (Latest)
- `mvn -f erp-domain/pom.xml -DskipTests compile` (PASS; audit task 03 evidence gate).
- `mvn -f erp-domain/pom.xml -Dcheckstyle.failOnViolation=false checkstyle:check` (PASS; 29454 violations reported; audit task 03 evidence gate).
- `mvn -f erp-domain/pom.xml test` (PASS; Tests run 213, Failures 0, Errors 0, Skipped 4).
- `curl http://localhost:9090/actuator/health` (UP).
- `mvn -f erp-domain/pom.xml -DskipTests compile` (PASS; task-02 evidence gate).
- `mvn -f erp-domain/pom.xml -Dcheckstyle.failOnViolation=false checkstyle:check` (PASS; 29454 violations reported; task-02 evidence gate).
- `mvn -f erp-domain/pom.xml -DskipTests compile` (PASS; task-01 evidence gate).
- `mvn -f erp-domain/pom.xml -Dcheckstyle.failOnViolation=false checkstyle:check` (PASS; 29454 violations reported; task-01 evidence gate).
- `mvn -f erp-domain/pom.xml test` (PASS; Tests run 213, Failures 0, Errors 0, Skipped 4).
- `curl http://localhost:9090/actuator/health` (UP).
- `mvn -f erp-domain/pom.xml -DskipTests compile` (PASS).
- `mvn -f erp-domain/pom.xml -Dcheckstyle.failOnViolation=false checkstyle:check` (PASS; 29454 violations reported).
- `mvn -f erp-domain/pom.xml test` (PASS; Tests run 213, Failures 0, Errors 0, Skipped 4).
- `mvn -f erp-domain/pom.xml -Dtest=PerformanceBudgetIT,PerformanceExplainIT,OrchestratorControllerIT,IntegrationCoordinatorTest test` (PASS; Tests run 16, Failures 0, Errors 0, Skipped 0).
- `mvn -f erp-domain/pom.xml -DskipTests compile` (PASS; audit task 07 + 08).
- `mvn -f erp-domain/pom.xml -Dcheckstyle.failOnViolation=false checkstyle:check` (PASS; 29454 violations reported; audit task 07 + 08).
- `mvn -f erp-domain/pom.xml -DskipTests compile` (PASS; audit task 01).
- `mvn -f erp-domain/pom.xml -Dcheckstyle.failOnViolation=false checkstyle:check` (PASS; 29454 violations reported; audit task 01).
- `mvn -f erp-domain/pom.xml -DskipTests compile` (PASS; audit task 02).
- `mvn -f erp-domain/pom.xml -Dcheckstyle.failOnViolation=false checkstyle:check` (PASS; 29454 violations reported; audit task 02).

## Warnings / Notes
- Checkstyle baseline warnings (29454) persisted with failOnViolation=false.
- Testcontainers auth config warnings and dynamic agent loading notices persisted.
- Test logs include expected warnings (invalid company IDs, negative balances, dispatch mapping, sequence contention/duplicate key retries, HTML-to-PDF CSS parsing); no failures.
- Outbox queries returned zero pending/retrying/dead-letter events on seeded dataset; stuck retry count 0.
- Audit tasks 01/02 + task-03 ran full test suite per AGENTS.md; all tests passed with 4 skipped.

## Current Task
- ERP logic audit program: LEAD-010/011 evidence + task-03 probes on `audit-inv-03-evidence-and-inventory`.
- Next recommended investigation: `tasks/erp_logic_audit/taskpack_investigation/task-04-production-costing-wip-hunt.md`.

## Commands Run (Audit)
- `sed -n ... SCOPE.md` and `.codex/AGENTS.md` (scope + execution rules).
- `sed -n ...` on repo docs required by the audit brief (module map, API matrix, stabilization log, debugging/predeploy tasks, ops evidence).
- `git rev-parse --abbrev-ref HEAD` / `git rev-parse HEAD` / `git status --porcelain` (captured in audit report).
- `psql -h localhost -p 55432 -U erp -d erp_domain -v company_id=5 -f tasks/erp_logic_audit/EVIDENCE_QUERIES/SQL/{06_inventory_valuation_fifo,07_inventory_control_vs_valuation,02_orphans_movements_without_journal,12_orphan_reservations,03_dispatch_slips_without_cogs_journal}.sql` (task-03 probes).
- `bash tasks/erp_logic_audit/EVIDENCE_QUERIES/curl/01_accounting_reports_gets.sh` (task-03 accounting reports GETs).
- Targeted POST repros for LEAD-010/011 (sales order idempotency + purchase return retry; outputs saved under task-01/task-02 OUTPUTS).
- `psql -v company_id=5 -f tasks/erp_logic_audit/EVIDENCE_QUERIES/task-02/SQL/*.sql` (P2P probes; no rows returned).
- `psql -v company_id=5 -f tasks/erp_logic_audit/EVIDENCE_QUERIES/task-01/SQL/*.sql` (O2C probes; no rows returned).
- `bash tasks/erp_logic_audit/EVIDENCE_QUERIES/task-02/curl/*.sh` (admin GET probes; recon dashboard requires bankAccountId).
- `bash tasks/erp_logic_audit/EVIDENCE_QUERIES/task-01/curl/*.sh` (admin + dealer portal GET probes).

## Resume Instructions (ERP logic audit)
1. Stay on branch `audit-inv-03-evidence-and-inventory` (no worktrees).
2. Open `tasks/erp_logic_audit/README.md` and follow the recommended order.
3. Run the next investigation task: `tasks/erp_logic_audit/taskpack_investigation/task-04-production-costing-wip-hunt.md`.
4. Use read-only probes under `tasks/erp_logic_audit/EVIDENCE_QUERIES/` to confirm/deny leads before adding any new LF items.
