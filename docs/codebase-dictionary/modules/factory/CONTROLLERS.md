# Factory Controllers

REST API endpoints for the Factory module.

## FactoryController
**Path**: `/api/v1/factory`
**Authorization**: `ROLE_ADMIN`, `ROLE_FACTORY`

Primary controller for production planning, task management, and cost allocation.

### Endpoints

#### Production Plans

| Method | Path | Description |
|--------|------|-------------|
| GET | `/production-plans` | List all production plans |
| POST | `/production-plans` | Create a new production plan |
| PUT | `/production-plans/{id}` | Update a production plan |
| PATCH | `/production-plans/{id}/status` | Update plan status |
| DELETE | `/production-plans/{id}` | Delete a production plan |

#### Factory Tasks

| Method | Path | Description |
|--------|------|-------------|
| GET | `/tasks` | List all factory tasks |
| POST | `/tasks` | Create a new factory task |
| PUT | `/tasks/{id}` | Update a factory task |

#### Dashboard & Cost Allocation

| Method | Path | Description |
|--------|------|-------------|
| GET | `/dashboard` | Get factory dashboard metrics |
| POST | `/cost-allocation` | Allocate period costs to batches |

### Dependencies
- `FactoryService` - Plan and task management
- `CostAllocationService` - Period cost allocation

---

## PackingController
**Path**: `/api/v1/factory`
**Authorization**: `ROLE_FACTORY`, `ROLE_ACCOUNTING`, `ROLE_ADMIN`

Handles packing operations for production batches.

### Endpoints

| Method | Path | Description | Required Headers |
|--------|------|-------------|------------------|
| POST | `/packing-records` | Record a packing operation | `Idempotency-Key` |
| GET | `/unpacked-batches` | List batches ready for packing | - |
| GET | `/production-logs/{productionLogId}/packing-history` | Get packing history for a batch | - |
| GET | `/bulk-batches/{finishedGoodId}` | List bulk batches for a finished good | - |
| GET | `/bulk-batches/{parentBatchId}/children` | List child batches from a bulk batch | - |

### Idempotency
The `POST /packing-records` endpoint requires an `Idempotency-Key` header for safe retries. Legacy headers `X-Idempotency-Key` and `X-Request-Id` are rejected with explicit migration guidance.

### Dependencies
- `PackingService` - Standard packing operations
- `BulkPackingService` - Bulk-to-size packing

---

## ProductionLogController
**Path**: `/api/v1/factory/production/logs`
**Authorization**: `ROLE_ADMIN`, `ROLE_FACTORY`

Manages production log entries (M2S - Manufacturing to Stock).

### Endpoints

| Method | Path | Description |
|--------|------|-------------|
| POST | `/` | Create a production log entry |
| GET | `/` | List recent production logs (top 25) |
| GET | `/{id}` | Get production log details |

### Production Log Creation Flow
1. Validates brand and product ownership
2. Issues raw materials from inventory (FIFO)
3. Creates semi-finished batch
4. Posts material journal (WIP debit, inventory credit)
5. Posts labor/overhead journal if applicable

### Dependencies
- `ProductionLogService` - Core production logging

---

## PackagingMappingController
**Path**: `/api/v1/factory/packaging-mappings`
**Authorization**: Varies by endpoint

Manages packaging size mappings for automatic material consumption during packing.

### Endpoints

| Method | Path | Description | Authorization |
|--------|------|-------------|---------------|
| GET | `/` | List all packaging mappings | `ROLE_ADMIN`, `ROLE_FACTORY` |
| GET | `/active` | List active mappings only | `ROLE_ADMIN`, `ROLE_FACTORY` |
| POST | `/` | Create a packaging mapping | `ROLE_ADMIN` |
| PUT | `/{id}` | Update a packaging mapping | `ROLE_ADMIN` |
| DELETE | `/{id}` | Deactivate a mapping | `ROLE_ADMIN` |

### Purpose
Packaging mappings define which raw material (bucket/container) is consumed when packing a specific size (1L, 5L, 10L, etc.). This enables automatic packaging material deduction during the packing process.

### Dependencies
- `PackagingMaterialService` - Mapping CRUD and consumption logic

---

## Controller Summary

| Controller | Base Path | Primary Responsibility |
|------------|-----------|----------------------|
| FactoryController | `/api/v1/factory` | Plans, tasks, dashboard, cost allocation |
| PackingController | `/api/v1/factory` | Packing operations, bulk-to-size |
| ProductionLogController | `/api/v1/factory/production/logs` | Production batch logging |
| PackagingMappingController | `/api/v1/factory/packaging-mappings` | Packaging rule configuration |
