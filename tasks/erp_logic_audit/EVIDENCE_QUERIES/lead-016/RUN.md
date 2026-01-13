# LEAD-016 Evidence Run (Admin override vs locked period)

## Objective
Determine whether `adminOverride=true` should bypass locked period posting or if reopen is the expected control.

## Planned probes
1) Confirm period lock state via API and SQL.
2) Attempt journal posting into a locked period with admin override.
3) Attempt the same override into an unlocked period (control).
4) Capture error messages to distinguish policy vs permission failures.

## Command log
```bash
git status -sb
git rev-parse --abbrev-ref HEAD
git rev-parse HEAD
git checkout -b audit-inv-leads-014-016
cat /home/realnigga/.codex/skills/erp-reconciliation-close-audit/SKILL.md
cat /home/realnigga/.codex/skills/erp-release-readiness-audit/SKILL.md
rg -n "LEAD-014|LEAD-016" tasks/erp_logic_audit/HUNT_NOTEBOOK.md
sed -n '300,380p' tasks/erp_logic_audit/HUNT_NOTEBOOK.md
tail -n 120 tasks/erp_logic_audit/LOGIC_FLAWS.md
cat tasks/erp_logic_audit/EVIDENCE_QUERIES/task-06/OUTPUTS/20260113T084648Z_period_lock_response.json
cat tasks/erp_logic_audit/EVIDENCE_QUERIES/task-06/OUTPUTS/20260113T084715Z_journal_locked_override_response.json
cat tasks/erp_logic_audit/EVIDENCE_QUERIES/task-06/OUTPUTS/20260113T084854Z_period_close_response.json
mkdir -p tasks/erp_logic_audit/EVIDENCE_QUERIES/lead-016/{SQL,curl,OUTPUTS}
cat <<'EOF' > tasks/erp_logic_audit/EVIDENCE_QUERIES/lead-016/RUN.md
cat <<'EOF' > tasks/erp_logic_audit/EVIDENCE_QUERIES/lead-014/RUN.md
sed -n '1,120p' tasks/erp_logic_audit/EVIDENCE_QUERIES/task-06/SQL/02_period_integrity_backdating_and_post_close_edits.sql
cat <<'EOF' > tasks/erp_logic_audit/EVIDENCE_QUERIES/lead-016/SQL/01_period_status.sql
cat <<'EOF' > tasks/erp_logic_audit/EVIDENCE_QUERIES/lead-016/curl/01_periods_get.sh
cat <<'EOF' > tasks/erp_logic_audit/EVIDENCE_QUERIES/lead-016/curl/02_period_lock.sh
cat <<'EOF' > tasks/erp_logic_audit/EVIDENCE_QUERIES/lead-016/curl/03_period_reopen.sh
cat <<'EOF' > tasks/erp_logic_audit/EVIDENCE_QUERIES/lead-016/curl/04_journal_entry_post.sh
chmod +x tasks/erp_logic_audit/EVIDENCE_QUERIES/lead-016/curl/*.sh
TS=$(date -u +"%Y%m%dT%H%M%SZ"); curl -sS -X POST -H 'Content-Type: application/json' -d '{"email":"admin@bbp.dev","password":"ChangeMe123!","companyCode":"BBP"}' http://localhost:8081/api/v1/auth/login > "tasks/erp_logic_audit/EVIDENCE_QUERIES/lead-016/OUTPUTS/${TS}_login.json"; jq -r '.accessToken' "tasks/erp_logic_audit/EVIDENCE_QUERIES/lead-016/OUTPUTS/${TS}_login.json" > "tasks/erp_logic_audit/EVIDENCE_QUERIES/lead-016/OUTPUTS/${TS}_token.txt"
TOKEN_FILE=$(ls -t tasks/erp_logic_audit/EVIDENCE_QUERIES/lead-016/OUTPUTS/*_token.txt | head -n 1); TOKEN=$(cat "$TOKEN_FILE"); TS=$(date -u +"%Y%m%dT%H%M%SZ"); BASE_URL=http://localhost:8081 COMPANY_CODE=BBP TOKEN="$TOKEN" bash tasks/erp_logic_audit/EVIDENCE_QUERIES/lead-016/curl/01_periods_get.sh > "tasks/erp_logic_audit/EVIDENCE_QUERIES/lead-016/OUTPUTS/${TS}_periods_get.json"
TS=$(date -u +"%Y%m%dT%H%M%SZ"); PGPASSWORD=erp psql -h localhost -p 55432 -U erp -d erp_domain -v company_id=5 -f tasks/erp_logic_audit/EVIDENCE_QUERIES/lead-016/SQL/01_period_status.sql > "tasks/erp_logic_audit/EVIDENCE_QUERIES/lead-016/OUTPUTS/${TS}_sql_period_status.txt"
TS=$(date -u +"%Y%m%dT%H%M%SZ"); PGPASSWORD=erp psql -h localhost -p 55432 -U erp -d erp_domain -c "select column_name from information_schema.columns where table_name='accounting_periods' order by ordinal_position;" > "tasks/erp_logic_audit/EVIDENCE_QUERIES/lead-016/OUTPUTS/${TS}_sql_period_columns.txt"
cat tasks/erp_logic_audit/EVIDENCE_QUERIES/lead-016/OUTPUTS/*_sql_period_columns.txt
TS=$(date -u +"%Y%m%dT%H%M%SZ"); PGPASSWORD=erp psql -h localhost -p 55432 -U erp -d erp_domain -v company_id=5 -f tasks/erp_logic_audit/EVIDENCE_QUERIES/lead-016/SQL/01_period_status.sql > "tasks/erp_logic_audit/EVIDENCE_QUERIES/lead-016/OUTPUTS/${TS}_sql_period_status.txt"
ls -t tasks/erp_logic_audit/EVIDENCE_QUERIES/lead-016/OUTPUTS | head -n 12
sed -n '1,200p' tasks/erp_logic_audit/EVIDENCE_QUERIES/lead-016/OUTPUTS/20260113T092230Z_periods_get.json
TS=$(date -u +"%Y%m%dT%H%M%SZ"); cat <<'EOF' > "tasks/erp_logic_audit/EVIDENCE_QUERIES/lead-016/OUTPUTS/${TS}_period_lock_request.json"
TS=$(date -u +"%Y%m%dT%H%M%SZ"); cat <<'EOF' > "tasks/erp_logic_audit/EVIDENCE_QUERIES/lead-016/OUTPUTS/${TS}_period_reopen_request.json"
TS=$(date -u +"%Y%m%dT%H%M%SZ"); REF="LEAD-016-LOCK-OVR-${TS}"; cat <<EOF > "tasks/erp_logic_audit/EVIDENCE_QUERIES/lead-016/OUTPUTS/${TS}_journal_locked_override_request.json"
TS=$(date -u +"%Y%m%dT%H%M%SZ"); REF="LEAD-016-OPEN-OVR-${TS}"; cat <<EOF > "tasks/erp_logic_audit/EVIDENCE_QUERIES/lead-016/OUTPUTS/${TS}_journal_open_override_request.json"
TOKEN_FILE=$(ls -t tasks/erp_logic_audit/EVIDENCE_QUERIES/lead-016/OUTPUTS/*_token.txt | head -n 1); TOKEN=$(cat "$TOKEN_FILE"); REQ_FILE=$(ls -t tasks/erp_logic_audit/EVIDENCE_QUERIES/lead-016/OUTPUTS/*_period_lock_request.json | head -n 1); TS=$(date -u +"%Y%m%dT%H%M%SZ"); BASE_URL=http://localhost:8081 COMPANY_CODE=BBP TOKEN="$TOKEN" PERIOD_ID=6 REQUEST_FILE="$REQ_FILE" bash tasks/erp_logic_audit/EVIDENCE_QUERIES/lead-016/curl/02_period_lock.sh > "tasks/erp_logic_audit/EVIDENCE_QUERIES/lead-016/OUTPUTS/${TS}_period_lock_response.json"
TOKEN_FILE=$(ls -t tasks/erp_logic_audit/EVIDENCE_QUERIES/lead-016/OUTPUTS/*_token.txt | head -n 1); TOKEN=$(cat "$TOKEN_FILE"); REQ_FILE=$(ls -t tasks/erp_logic_audit/EVIDENCE_QUERIES/lead-016/OUTPUTS/*_journal_locked_override_request.json | head -n 1); TS=$(date -u +"%Y%m%dT%H%M%SZ"); BASE_URL=http://localhost:8081 COMPANY_CODE=BBP TOKEN="$TOKEN" REQUEST_FILE="$REQ_FILE" bash tasks/erp_logic_audit/EVIDENCE_QUERIES/lead-016/curl/04_journal_entry_post.sh > "tasks/erp_logic_audit/EVIDENCE_QUERIES/lead-016/OUTPUTS/${TS}_journal_locked_override_response.json"
TOKEN_FILE=$(ls -t tasks/erp_logic_audit/EVIDENCE_QUERIES/lead-016/OUTPUTS/*_token.txt | head -n 1); TOKEN=$(cat "$TOKEN_FILE"); REQ_FILE=$(ls -t tasks/erp_logic_audit/EVIDENCE_QUERIES/lead-016/OUTPUTS/*_journal_open_override_request.json | head -n 1); TS=$(date -u +"%Y%m%dT%H%M%SZ"); BASE_URL=http://localhost:8081 COMPANY_CODE=BBP TOKEN="$TOKEN" REQUEST_FILE="$REQ_FILE" bash tasks/erp_logic_audit/EVIDENCE_QUERIES/lead-016/curl/04_journal_entry_post.sh > "tasks/erp_logic_audit/EVIDENCE_QUERIES/lead-016/OUTPUTS/${TS}_journal_open_override_response.json"
TOKEN_FILE=$(ls -t tasks/erp_logic_audit/EVIDENCE_QUERIES/lead-016/OUTPUTS/*_token.txt | head -n 1); TOKEN=$(cat "$TOKEN_FILE"); REQ_FILE=$(ls -t tasks/erp_logic_audit/EVIDENCE_QUERIES/lead-016/OUTPUTS/*_period_reopen_request.json | head -n 1); TS=$(date -u +"%Y%m%dT%H%M%SZ"); BASE_URL=http://localhost:8081 COMPANY_CODE=BBP TOKEN="$TOKEN" PERIOD_ID=6 REQUEST_FILE="$REQ_FILE" bash tasks/erp_logic_audit/EVIDENCE_QUERIES/lead-016/curl/03_period_reopen.sh > "tasks/erp_logic_audit/EVIDENCE_QUERIES/lead-016/OUTPUTS/${TS}_period_reopen_response.json"
ls -t tasks/erp_logic_audit/EVIDENCE_QUERIES/lead-016/OUTPUTS | head -n 12
cat tasks/erp_logic_audit/EVIDENCE_QUERIES/lead-016/OUTPUTS/20260113T092343Z_period_lock_response.json
cat tasks/erp_logic_audit/EVIDENCE_QUERIES/lead-016/OUTPUTS/20260113T092350Z_journal_locked_override_response.json
cat tasks/erp_logic_audit/EVIDENCE_QUERIES/lead-016/OUTPUTS/20260113T092356Z_journal_open_override_response.json
rg -n "adminOverride" erp-domain/src/main/java
rg -n "locked|lock" erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/service/AccountingService.java
sed -n '520,590p' erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/service/AccountingService.java
rg -n "requireOpenPeriod|validateEntryDate" -n erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/service/AccountingService.java
sed -n '220,280p' erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/service/AccountingService.java
sed -n '1730,1785p' erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/service/AccountingService.java
rg -n "requireOpenPeriod" -n erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/service/AccountingPeriodService.java
sed -n '210,260p' erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/service/AccountingPeriodService.java
TS=$(date -u +"%Y%m%dT%H%M%SZ"); sed -n '235,260p' erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/service/AccountingService.java > "tasks/erp_logic_audit/EVIDENCE_QUERIES/lead-016/OUTPUTS/${TS}_accounting_service_period_check.txt"
TS=$(date -u +"%Y%m%dT%H%M%SZ"); sed -n '218,238p' erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/service/AccountingPeriodService.java > "tasks/erp_logic_audit/EVIDENCE_QUERIES/lead-016/OUTPUTS/${TS}_accounting_period_require_open.txt"
ls -t tasks/erp_logic_audit/EVIDENCE_QUERIES/lead-016/OUTPUTS | head -n 40
wc -l tasks/erp_logic_audit/EVIDENCE_QUERIES/lead-016/OUTPUTS/20260113T092234Z_sql_period_status.txt
git status -sb
sed -n '1,200p' tasks/erp_logic_audit/EVIDENCE_QUERIES/lead-016/RUN.md
git add -p tasks/erp_logic_audit/HUNT_NOTEBOOK.md
git add -p tasks/erp_logic_audit/FINDINGS_INDEX.md
git add tasks/erp_logic_audit/EVIDENCE_QUERIES/lead-016
git commit -m "audit-inv: LEAD-016 locked period override evidence + disposition"
```

## Outputs captured
- `tasks/erp_logic_audit/EVIDENCE_QUERIES/lead-016/OUTPUTS/20260113T092225Z_login.json`
- `tasks/erp_logic_audit/EVIDENCE_QUERIES/lead-016/OUTPUTS/20260113T092225Z_token.txt`
- `tasks/erp_logic_audit/EVIDENCE_QUERIES/lead-016/OUTPUTS/20260113T092230Z_periods_get.json`
- `tasks/erp_logic_audit/EVIDENCE_QUERIES/lead-016/OUTPUTS/20260113T092234Z_sql_period_status.txt` (empty; SQL error on first pass)
- `tasks/erp_logic_audit/EVIDENCE_QUERIES/lead-016/OUTPUTS/20260113T092248Z_sql_period_columns.txt`
- `tasks/erp_logic_audit/EVIDENCE_QUERIES/lead-016/OUTPUTS/20260113T092259Z_sql_period_status.txt`
- `tasks/erp_logic_audit/EVIDENCE_QUERIES/lead-016/OUTPUTS/20260113T092319Z_period_lock_request.json`
- `tasks/erp_logic_audit/EVIDENCE_QUERIES/lead-016/OUTPUTS/20260113T092343Z_period_lock_response.json`
- `tasks/erp_logic_audit/EVIDENCE_QUERIES/lead-016/OUTPUTS/20260113T092329Z_journal_locked_override_request.json`
- `tasks/erp_logic_audit/EVIDENCE_QUERIES/lead-016/OUTPUTS/20260113T092350Z_journal_locked_override_response.json`
- `tasks/erp_logic_audit/EVIDENCE_QUERIES/lead-016/OUTPUTS/20260113T092336Z_journal_open_override_request.json`
- `tasks/erp_logic_audit/EVIDENCE_QUERIES/lead-016/OUTPUTS/20260113T092356Z_journal_open_override_response.json`
- `tasks/erp_logic_audit/EVIDENCE_QUERIES/lead-016/OUTPUTS/20260113T092322Z_period_reopen_request.json`
- `tasks/erp_logic_audit/EVIDENCE_QUERIES/lead-016/OUTPUTS/20260113T092403Z_period_reopen_response.json`
- `tasks/erp_logic_audit/EVIDENCE_QUERIES/lead-016/OUTPUTS/20260113T092627Z_accounting_service_period_check.txt`
- `tasks/erp_logic_audit/EVIDENCE_QUERIES/lead-016/OUTPUTS/20260113T092631Z_accounting_period_require_open.txt`
