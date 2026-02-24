# Cross Workflow Plan

Ticket: `TKT-ERP-STAGE-107`
Generated: `2026-02-23T19:10:40+00:00`

## In-Ticket Dependency Edges

| Upstream Slice | Upstream Agent | Downstream Slice | Downstream Agent | Contract |
| --- | --- | --- | --- | --- |
| SLICE-02 | sales-domain | SLICE-01 | accounting-domain | o2c posting and receivable linkage |

## Recommended Merge Order

1. `SLICE-02` (sales-domain)
2. `SLICE-01` (accounting-domain)

## Slice Coordination Notes

### SLICE-02 (sales-domain)
- Upstream slices: none
- Downstream slices: SLICE-01
- External upstream agents to watch: auth-rbac-company, orchestrator-runtime
- External downstream agents to watch: inventory-domain

### SLICE-01 (accounting-domain)
- Upstream slices: SLICE-02
- Downstream slices: none
- External upstream agents to watch: auth-rbac-company, factory-production, hr-domain, orchestrator-runtime, purchasing-invoice-p2p
- External downstream agents to watch: none

