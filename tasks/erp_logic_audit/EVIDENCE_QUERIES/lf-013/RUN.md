# LF-013 Evidence Run (Packing status not updated)

## Objective
Show full packing updates production log status to FULLY_PACKED and removes it from unpacked batches.

## Planned probes
1) Create production log with mixed quantity.
2) Capture status before packing (SQL).
3) Record packing for full quantity.
4) Fetch log detail + unpacked batches.
5) Capture status after packing (SQL) and app logs if needed.

## Command log
```bash
# Create production log
TS=$(date -u +"%Y%m%dT%H%M%SZ")
REQUEST_FILE="tasks/erp_logic_audit/EVIDENCE_QUERIES/lf-013/OUTPUTS/${TS}_production_log_request.json"
cat > "$REQUEST_FILE" <<'REQ'
{
  "brandId": 2,
  "productId": 3,
  "batchSize": 5,
  "unitOfMeasure": "KG",
  "mixedQuantity": 5,
  "producedAt": "2026-01-13",
  "createdBy": "Evidence",
  "materials": [
    {
      "rawMaterialId": 1,
      "quantity": 5,
      "unitOfMeasure": "KG"
    }
  ]
}
REQ

TOKEN_FILE=$(ls -t tasks/erp_logic_audit/EVIDENCE_QUERIES/lead-015/OUTPUTS/*_token_after_fix.txt | head -n 1)
TOKEN=$(cat "$TOKEN_FILE")
BASE_URL=http://localhost:8081 COMPANY_CODE=BBP TOKEN="$TOKEN" REQUEST_FILE="$REQUEST_FILE" \
  tasks/erp_logic_audit/EVIDENCE_QUERIES/lf-013/curl/01_production_log_create.sh \
  > "tasks/erp_logic_audit/EVIDENCE_QUERIES/lf-013/OUTPUTS/${TS}_production_log_create_after_fix.txt"

# Status before packing
TS=$(date -u +"%Y%m%dT%H%M%SZ")
PGPASSWORD=erp psql -h localhost -p 55432 -U erp -d erp_domain \
  -v company_id=5 \
  -f tasks/erp_logic_audit/EVIDENCE_QUERIES/lf-013/SQL/01_production_log_status.sql \
  > "tasks/erp_logic_audit/EVIDENCE_QUERIES/lf-013/OUTPUTS/${TS}_production_log_status_before_packing.txt"

# Record packing for full quantity (log id 3)
TS=$(date -u +"%Y%m%dT%H%M%SZ")
REQUEST_FILE="tasks/erp_logic_audit/EVIDENCE_QUERIES/lf-013/OUTPUTS/${TS}_packing_request.json"
cat > "$REQUEST_FILE" <<'REQ'
{
  "productionLogId": 3,
  "packedDate": "2026-01-13",
  "packedBy": "Evidence",
  "lines": [
    {
      "packagingSize": "5L",
      "quantityLiters": 5,
      "piecesCount": 1
    }
  ]
}
REQ

BASE_URL=http://localhost:8081 COMPANY_CODE=BBP TOKEN="$TOKEN" REQUEST_FILE="$REQUEST_FILE" \
  tasks/erp_logic_audit/EVIDENCE_QUERIES/lf-013/curl/02_packing_record_post.sh \
  > "tasks/erp_logic_audit/EVIDENCE_QUERIES/lf-013/OUTPUTS/${TS}_packing_record_post_after_fix.txt"

# Log detail + unpacked list
TS=$(date -u +"%Y%m%dT%H%M%SZ")
LOG_ID=3
BASE_URL=http://localhost:8081 COMPANY_CODE=BBP TOKEN="$TOKEN" LOG_ID="$LOG_ID" \
  tasks/erp_logic_audit/EVIDENCE_QUERIES/lf-013/curl/03_production_log_detail_get.sh \
  > "tasks/erp_logic_audit/EVIDENCE_QUERIES/lf-013/OUTPUTS/${TS}_production_log_detail_after_pack.txt"

TS=$(date -u +"%Y%m%dT%H%M%SZ")
BASE_URL=http://localhost:8081 COMPANY_CODE=BBP TOKEN="$TOKEN" \
  tasks/erp_logic_audit/EVIDENCE_QUERIES/lf-013/curl/04_unpacked_batches_get.sh \
  > "tasks/erp_logic_audit/EVIDENCE_QUERIES/lf-013/OUTPUTS/${TS}_unpacked_batches_after_pack.txt"

# Status after packing
TS=$(date -u +"%Y%m%dT%H%M%SZ")
PGPASSWORD=erp psql -h localhost -p 55432 -U erp -d erp_domain \
  -v company_id=5 \
  -f tasks/erp_logic_audit/EVIDENCE_QUERIES/lf-013/SQL/01_production_log_status.sql \
  > "tasks/erp_logic_audit/EVIDENCE_QUERIES/lf-013/OUTPUTS/${TS}_production_log_status_after_packing.txt"

# Capture logs (unpacked-batches 500 shows LazyInitializationException)
TS=$(date -u +"%Y%m%dT%H%M%SZ")
docker logs --tail 200 erp_domain_app \
  > "tasks/erp_logic_audit/EVIDENCE_QUERIES/lf-013/OUTPUTS/${TS}_app_logs_tail.txt"
```
