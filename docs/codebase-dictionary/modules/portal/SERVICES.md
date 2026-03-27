# Portal Services

## Overview

| Service | Purpose |
|---------|---------|
| PortalInsightsService | Dashboard, operations, workforce insights |
| EnterpriseDashboardService | Enterprise snapshot with trends |
| TenantRuntimeEnforcementInterceptor | Request rate limiting |
| TenantRuntimeEnforcementConfig | Configuration |

---

## PortalInsightsService

**File**: `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/portal/service/PortalInsightsService.java`

**Package**: `com.bigbrightpaints.erp.modules.portal.service`

**Responsibility**: Aggregate dashboard, operations, and workforce insights

 supporting HR module

### Dependencies
- CompanyContextService
- EntityManager
- DealerRepository
- PackagingSlipRepository
- EmployeeRepository
- ProductionPlanRepository
- ProductionBatchRepository
- FactoryTaskRepository
- RawMaterialRepository
- FinishedGoodRepository
- FinishedGoodBatchRepository
- LeaveRequestRepository
- PayrollRunRepository
- AccountRepository
- CompanyClock
- ModuleGatingService

### Public Methods

| Method | Signature | Description |
|--------|-----------|-------------|
| dashboard | `DashboardInsights dashboard()` | Dashboard metrics |
| operations | `OperationsInsights operations()` | Operations metrics |
| workforce | `WorkforceInsights workforce()` | Workforce metrics (HR required) |

### Side Effects
- DB reads: Multiple entity queries

- Calculations: Revenue, production metrics

### Status
✅ **Canonical** - Portal insights

---

## EnterpriseDashboardService
**File**: `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/portal/service/EnterpriseDashboardService.java`

**Package**: `com.bigbrightpaints.erp.modules.portal.service`

**Responsibility**: Enterprise-wide dashboard snapshot with financial, sales, operations, and alerts data

 trends
### Dependencies
- CompanyContextService
- InvoiceRepository
- SalesOrderRepository
- PartnerSettlementAllocationRepository
- JournalEntryRepository
- AccountRepository
- ProductionLogRepository
- PackingRecordRepository
- PackagingSlipRepository
- ReportService

### Public Methods

| Method | Signature | Description |
|--------|-----------|-------------|
| snapshot | `EnterpriseDashboardSnapshot snapshot(String window, String compare, String timezone)` | Generate snapshot |

### Snapshot Components
- **Financial**: Revenue, COGS, AR, cash, inventory
- **Sales**: Booked backlog, order count, order-to-cash days
- **Operations**: Production, packing, dispatch metrics
- **Ratios**: Gross margin, overdue %, inventory turns
- **Trends**: Revenue, COGS, cash, AR overdue by time
- **Alerts**: Low cash, high overdue, low inventory, dispatch backlog
- **Breakdowns**: Top dealers, SKus, overdue invoices

### Status
✅ **Canonical** - Enterprise dashboard
