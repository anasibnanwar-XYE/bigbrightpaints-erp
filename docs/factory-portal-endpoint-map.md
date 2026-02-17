# Factory Portal Endpoint Map (V1)

Source of truth: `openapi.json`

Portal: `FACTORY`  
Scope: production, manufacturing, packing, factory execution

## 1) Core Endpoint Packet

### Production plans
| Method | Path | Purpose |
|---|---|---|
| GET | `/api/v1/factory/production-plans` | list plans |
| POST | `/api/v1/factory/production-plans` | create plan |
| PUT | `/api/v1/factory/production-plans/{id}` | update plan |
| PATCH | `/api/v1/factory/production-plans/{id}/status` | status transition |
| DELETE | `/api/v1/factory/production-plans/{id}` | delete plan |

### Production execution
| Method | Path | Purpose |
|---|---|---|
| GET | `/api/v1/factory/production/logs` | list logs |
| POST | `/api/v1/factory/production/logs` | create production log |
| GET | `/api/v1/factory/production/logs/{id}` | production log detail |
| GET | `/api/v1/factory/production-batches` | batch listing |
| POST | `/api/v1/factory/production-batches` | batch logging |

### Packing and mappings
| Method | Path | Purpose |
|---|---|---|
| POST | `/api/v1/factory/pack` | pack bulk to sizes |
| POST | `/api/v1/factory/packing-records` | packing event |
| POST | `/api/v1/factory/packing-records/{productionLogId}/complete` | complete packing |
| GET | `/api/v1/factory/production-logs/{productionLogId}/packing-history` | packing history |
| GET | `/api/v1/factory/packaging-mappings` | mapping list |
| GET | `/api/v1/factory/packaging-mappings/active` | active mappings |
| POST | `/api/v1/factory/packaging-mappings` | create mapping |
| PUT | `/api/v1/factory/packaging-mappings/{id}` | update mapping |
| DELETE | `/api/v1/factory/packaging-mappings/{id}` | deactivate mapping |

### Costing, tasks, and visibility
| Method | Path | Purpose |
|---|---|---|
| POST | `/api/v1/factory/cost-allocation` | cost allocation execution |
| GET | `/api/v1/factory/tasks` | task list |
| POST | `/api/v1/factory/tasks` | create task |
| PUT | `/api/v1/factory/tasks/{id}` | update task |
| GET | `/api/v1/factory/dashboard` | plant KPI summary |
| GET | `/api/v1/factory/unpacked-batches` | unpacked batches queue |
| GET | `/api/v1/factory/bulk-batches/{finishedGoodId}` | bulk batch view |
| GET | `/api/v1/factory/bulk-batches/{parentBatchId}/children` | child batch lineage |

## 2) Shared Inventory Endpoints Used by Factory Flows

| Method | Path | Purpose |
|---|---|---|
| GET | `/api/v1/finished-goods` | FG catalog |
| GET | `/api/v1/finished-goods/{id}` | FG detail |
| GET | `/api/v1/finished-goods/{id}/batches` | FG batches |
| GET | `/api/v1/finished-goods/stock-summary` | FG stock rollup |
| GET | `/api/v1/raw-materials/stock` | RM stock rollup |

## 3) Workflow UX Contracts

1. Plan lifecycle: create -> status transition -> execution linkage.
2. Packing lifecycle: log packing -> complete packing -> verify history.
3. Costing lifecycle: allocate costs only when related production records are complete.

Frontend rules:
- Render next valid action only for current workflow state.
- Do not show destructive actions when status is terminal.
- Show deterministic retry messaging for duplicate submits/replays.

## 4) Role and Security Contract
- Base guard: factory endpoints require `ROLE_ADMIN` or `ROLE_FACTORY`.
- Tenant context is mandatory; cross-tenant access must fail closed.

## 5) Error Contract
- `401`: redirect to login.
- `403`: show role or tenant boundary denial.
- `409`: status conflict; force refresh before retry.
- `422`: request validation feedback with field-level markers.

## 6) Frontend Implementation Checklist
- Build screens around state transitions, not raw CRUD pages.
- Preserve sequence integrity (plan before packing/costing).
- Add confirmation UX for status changes and cost allocation.
- Use optimistic updates only for non-financial reads; re-fetch after financial-impact actions.
