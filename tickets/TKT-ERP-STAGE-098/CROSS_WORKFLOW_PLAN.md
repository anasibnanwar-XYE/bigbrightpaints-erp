# Cross Workflow Plan

Ticket: `TKT-ERP-STAGE-098`
Generated: `2026-02-20T19:08:59+00:00`

## In-Ticket Dependency Edges

| Upstream Slice | Upstream Agent | Downstream Slice | Downstream Agent | Contract |
| --- | --- | --- | --- | --- |
| SLICE-02 | auth-rbac-company | SLICE-01 | accounting-domain | finance/admin authority boundaries |
| SLICE-02 | auth-rbac-company | SLICE-03 | purchasing-invoice-p2p | tenant-scoped supplier/AP access rules |
| SLICE-02 | auth-rbac-company | SLICE-04 | reports-admin-portal | admin/report access boundaries |
| SLICE-02 | auth-rbac-company | SLICE-05 | sales-domain | tenant and role boundary contract |
| SLICE-03 | purchasing-invoice-p2p | SLICE-01 | accounting-domain | ap/posting and settlement linkage |
| SLICE-05 | sales-domain | SLICE-01 | accounting-domain | o2c posting and receivable linkage |

## Recommended Merge Order

1. `SLICE-02` (auth-rbac-company)
2. `SLICE-03` (purchasing-invoice-p2p)
3. `SLICE-04` (reports-admin-portal)
4. `SLICE-05` (sales-domain)
5. `SLICE-01` (accounting-domain)
6. `SLICE-06` (refactor-techdebt-gc)

## Slice Coordination Notes

### SLICE-02 (auth-rbac-company)
- Upstream slices: none
- Downstream slices: SLICE-01, SLICE-03, SLICE-04, SLICE-05
- External upstream agents to watch: none
- External downstream agents to watch: factory-production, hr-domain, inventory-domain

### SLICE-03 (purchasing-invoice-p2p)
- Upstream slices: SLICE-02
- Downstream slices: SLICE-01
- External upstream agents to watch: none
- External downstream agents to watch: inventory-domain

### SLICE-04 (reports-admin-portal)
- Upstream slices: SLICE-02
- Downstream slices: none
- External upstream agents to watch: none
- External downstream agents to watch: none

### SLICE-05 (sales-domain)
- Upstream slices: SLICE-02
- Downstream slices: SLICE-01
- External upstream agents to watch: orchestrator-runtime
- External downstream agents to watch: inventory-domain

### SLICE-01 (accounting-domain)
- Upstream slices: SLICE-02, SLICE-03, SLICE-05
- Downstream slices: none
- External upstream agents to watch: factory-production, hr-domain, orchestrator-runtime
- External downstream agents to watch: none

### SLICE-06 (refactor-techdebt-gc)
- Upstream slices: none
- Downstream slices: none
- External upstream agents to watch: none
- External downstream agents to watch: none

