# LEAD-007 Evidence Run

## Objective
Test whether raw material batch codes are enforced unique by the API/DB.

## Command log
```bash
# Schema + lookup
TS=$(date -u +"%Y%m%dT%H%M%SZ"); PGPASSWORD=erp psql -h localhost -p 55432 -U erp -d erp_domain -c "\\d suppliers" \
  > tasks/erp_logic_audit/EVIDENCE_QUERIES/lead-007/OUTPUTS/${TS}_suppliers_schema.txt
TS=$(date -u +"%Y%m%dT%H%M%SZ"); PGPASSWORD=erp psql -h localhost -p 55432 -U erp -d erp_domain \
  -v company_id=5 -f tasks/erp_logic_audit/EVIDENCE_QUERIES/lead-007/SQL/01_raw_materials.sql \
  > tasks/erp_logic_audit/EVIDENCE_QUERIES/lead-007/OUTPUTS/${TS}_raw_materials.txt
TS=$(date -u +"%Y%m%dT%H%M%SZ"); PGPASSWORD=erp psql -h localhost -p 55432 -U erp -d erp_domain \
  -v company_id=5 -f tasks/erp_logic_audit/EVIDENCE_QUERIES/lead-007/SQL/02_suppliers.sql \
  > tasks/erp_logic_audit/EVIDENCE_QUERIES/lead-007/OUTPUTS/${TS}_suppliers.txt

# Login
TS=$(date -u +"%Y%m%dT%H%M%SZ"); curl -sS -X POST -H 'Content-Type: application/json' \
  -d '{"email":"admin@bbp.dev","password":"ChangeMe123!","companyCode":"BBP"}' \
  http://localhost:8081/api/v1/auth/login \
  > tasks/erp_logic_audit/EVIDENCE_QUERIES/lead-007/OUTPUTS/${TS}_login.json
jq -r '.accessToken' tasks/erp_logic_audit/EVIDENCE_QUERIES/lead-007/OUTPUTS/${TS}_login.json \
  > tasks/erp_logic_audit/EVIDENCE_QUERIES/lead-007/OUTPUTS/${TS}_token.txt

# Create the same batch code twice
TOKEN=$(cat tasks/erp_logic_audit/EVIDENCE_QUERIES/lead-007/OUTPUTS/*_token.txt | tail -n 1)
REQ=$(ls tasks/erp_logic_audit/EVIDENCE_QUERIES/lead-007/OUTPUTS/*_batch_request.json | tail -n 1)
TS=$(date -u +"%Y%m%dT%H%M%SZ"); BASE_URL=http://localhost:8081 COMPANY_CODE=BBP TOKEN="$TOKEN" RAW_MATERIAL_ID=1 REQ="$REQ" \
  bash tasks/erp_logic_audit/EVIDENCE_QUERIES/lead-007/curl/01_create_batch.sh \
  > tasks/erp_logic_audit/EVIDENCE_QUERIES/lead-007/OUTPUTS/${TS}_batch_create_1.txt
TS=$(date -u +"%Y%m%dT%H%M%SZ"); BASE_URL=http://localhost:8081 COMPANY_CODE=BBP TOKEN="$TOKEN" RAW_MATERIAL_ID=1 REQ="$REQ" \
  bash tasks/erp_logic_audit/EVIDENCE_QUERIES/lead-007/curl/01_create_batch.sh \
  > tasks/erp_logic_audit/EVIDENCE_QUERIES/lead-007/OUTPUTS/${TS}_batch_create_2.txt

# Confirm duplicates in DB
TS=$(date -u +"%Y%m%dT%H%M%SZ"); PGPASSWORD=erp psql -h localhost -p 55432 -U erp -d erp_domain \
  -v batch_code="'LEAD-007-DUP'" -f tasks/erp_logic_audit/EVIDENCE_QUERIES/lead-007/SQL/03_duplicate_batch_codes.sql \
  > tasks/erp_logic_audit/EVIDENCE_QUERIES/lead-007/OUTPUTS/${TS}_duplicate_batch_codes.txt
```

## Outputs captured
- `tasks/erp_logic_audit/EVIDENCE_QUERIES/lead-007/OUTPUTS/*_batch_request.json`
- `tasks/erp_logic_audit/EVIDENCE_QUERIES/lead-007/OUTPUTS/*_batch_create_1.txt`
- `tasks/erp_logic_audit/EVIDENCE_QUERIES/lead-007/OUTPUTS/*_batch_create_2.txt`
- `tasks/erp_logic_audit/EVIDENCE_QUERIES/lead-007/OUTPUTS/*_duplicate_batch_codes.txt`
