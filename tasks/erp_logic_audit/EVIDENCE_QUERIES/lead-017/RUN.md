# LEAD-017 Evidence Run

## Objective
Reproduce unpacked-batches endpoint lazy-load error and capture logs.

## Command log
```bash
# Login
TS=$(date -u +"%Y%m%dT%H%M%SZ"); curl -sS -X POST -H 'Content-Type: application/json' \
  -d '{"email":"admin@bbp.dev","password":"ChangeMe123!","companyCode":"BBP"}' \
  http://localhost:8081/api/v1/auth/login \
  > tasks/erp_logic_audit/EVIDENCE_QUERIES/lead-017/OUTPUTS/${TS}_login.json
jq -r '.accessToken' tasks/erp_logic_audit/EVIDENCE_QUERIES/lead-017/OUTPUTS/${TS}_login.json \
  > tasks/erp_logic_audit/EVIDENCE_QUERIES/lead-017/OUTPUTS/${TS}_token.txt

# Call unpacked-batches endpoint
TOKEN=$(cat tasks/erp_logic_audit/EVIDENCE_QUERIES/lead-017/OUTPUTS/*_token.txt | tail -n 1)
TS=$(date -u +"%Y%m%dT%H%M%SZ"); BASE_URL=http://localhost:8081 COMPANY_CODE=BBP TOKEN="$TOKEN" \
  bash tasks/erp_logic_audit/EVIDENCE_QUERIES/lead-017/curl/01_unpacked_batches_get.sh \
  > tasks/erp_logic_audit/EVIDENCE_QUERIES/lead-017/OUTPUTS/${TS}_unpacked_batches_get.txt

# Capture app logs for LazyInitializationException
TS=$(date -u +"%Y%m%dT%H%M%SZ"); docker logs erp_domain_app --since 10m \
  > tasks/erp_logic_audit/EVIDENCE_QUERIES/lead-017/OUTPUTS/${TS}_app_logs.txt
```

## Outputs captured
- `tasks/erp_logic_audit/EVIDENCE_QUERIES/lead-017/OUTPUTS/*_unpacked_batches_get.txt`
- `tasks/erp_logic_audit/EVIDENCE_QUERIES/lead-017/OUTPUTS/*_app_logs.txt`
