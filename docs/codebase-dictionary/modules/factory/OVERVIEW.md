# Factory Module Overview

## Purpose
The Factory module handles manufacturing operations for BigBright ERP, managing the complete production lifecycle from raw material consumption through finished goods packing. It implements the Manufacturing-to-Stock (M2S) workflow for paint production.

## Module Statistics
- **Controllers**: 4
- **Services**: 21
- **Entities**: 12 domain entities + 6 repositories
- **DTOs**: 25
- **Total Source Files**: 69 (excluding tests)

## Package Structure
```
com.bigbrightpaints.erp.modules.factory
├── controller/          # REST API endpoints
├── service/             # Business logic layer
├── domain/              # JPA entities and repositories
├── dto/                 # Data transfer objects
└── event/               # Domain events
```

## Core Capabilities

### 1. Production Planning
- Create and manage production plans
- Track plan status (PLANNED, IN_PROGRESS, COMPLETED)
- Associate plans with products and quantities

### 2. Production Logging (M2S Flow)
- Record production batches with material consumption
- Automatic raw material FIFO batch selection
- WIP (Work-in-Progress) accounting entries
- Semi-finished goods batch creation
- Labor and overhead cost tracking

### 3. Packing Operations
- **Standard Packing**: Convert semi-finished bulk into sellable sizes
- **Bulk-to-Size Packing**: Repack bulk batches into smaller SKUs
- Automatic packaging material consumption
- Idempotent packing operations
- Finished goods batch registration

### 4. Cost Management
- Material cost accumulation via batch FIFO
- Labor and overhead cost allocation
- Periodic cost variance distribution
- Unit cost calculation per production batch

### 5. Packaging Rules
- Configure packaging size mappings (1L, 5L, 10L, etc.)
- Associate packaging materials with sizes
- Automatic packaging deduction during packing

## Key Domain Flows

### Manufacturing-to-Stock (M2S) Flow
```
Raw Materials → Production Log → Semi-Finished Batch → Packing → Finished Goods
      ↓              ↓                    ↓                ↓            ↓
   Inventory    WIP Account         Valuation        Packaging    Sales-Ready
   Deduction    Journaling          Account          Costing      Inventory
```

### Packing Flow
```
Unpacked Batch → Select Size Variant → Consume Packaging → Register FG Batch → Journal Entry
```

## Security Model
- **ROLE_FACTORY**: Standard factory operations (view, create, update)
- **ROLE_ACCOUNTING**: Read access to packing records
- **ROLE_ADMIN**: Full access including packaging rule management

## Integration Points
- **Inventory Module**: Raw materials, finished goods, batch management
- **Accounting Module**: Journal entries, WIP tracking, cost allocation
- **Production Module**: Product definitions, brands, SKU management
- **Sales Module**: Order linkage for production batches

## File Reference
| Stereotype | File Count | Key Files |
|------------|------------|-----------|
| Controllers | 4 | FactoryController, PackingController, ProductionLogController, PackagingMappingController |
| Services | 21 | FactoryService, PackingService, ProductionLogService, BulkPackingService |
| Entities | 12 | ProductionLog, PackingRecord, ProductionPlan, FactoryTask |
| DTOs | 25 | ProductionLogRequest, PackingRequest, BulkPackRequest |
| Repositories | 6 | ProductionLogRepository, PackingRecordRepository |

## Related Documentation
- [CONTROLLERS.md](./CONTROLLERS.md) - REST API endpoints
- [SERVICES.md](./SERVICES.md) - Business logic services
- [ENTITIES.md](./ENTITIES.md) - Domain model
- [DTOS.md](./DTOS.md) - Data transfer objects
- [CANONICAL_FLOWS.md](./CANONICAL_FLOWS.md) - Manufacturing workflows
