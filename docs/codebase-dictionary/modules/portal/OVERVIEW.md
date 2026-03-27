# Portal Module Overview

**Module Path:** `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/portal/`  
**Package Root:** `com.bigbrightpaints.erp.modules.portal`

## Purpose

The Portal module provides dealer and admin portal capabilities including:
- Dashboard insights for dealers and admins
- Enterprise-wide operational metrics
- Workforce insights (HR integration)
- Tenant runtime enforcement

## Module Boundaries

### Inbound Dependencies
- **Company Context** (`modules/company`) - Tenant isolation
- **Accounting** (`modules/accounting`) - Revenue, AR, COGS calculations
- **Sales** (`modules/sales`) - Dealer data, sales orders
- **Inventory** (`modules/inventory`) - Stock, dispatch data
- **Factory** (`modules/factory`) - Production data
- **HR** (`modules/hr`) - Employee, payroll, leave data
- **Invoice** (`modules/invoice`) - Invoice data

### Outbound Dependencies
- **Reports** (`modules/reports`) - Inventory valuation

## Architecture Layers

```
modules/portal/
├── controller/      # REST endpoints (1 controller)
├── service/         # Business logic (4 services)
└── dto/             # Data transfer objects (4 classes)
```

## Key Design Patterns

### 1. Aggregated Insights
- `PortalInsightsService` aggregates data from multiple modules
- Dashboard shows key metrics: revenue, fulfilment, workforce, dealers

### 2. Enterprise Dashboard
- `EnterpriseDashboardService` provides comprehensive business intelligence
- Time-series data, aging analysis, alerts, and breakdowns

### 3. Tenant Runtime Enforcement
- `TenantRuntimeEnforcementInterceptor` enforces tenant quotas
- Request-level rate limiting and concurrent request tracking

## Anti-Patterns to Avoid

### 1. Direct Repository Access
❌ **Wrong:** Querying repositories directly for dashboard data
✅ **Correct:** Use `PortalInsightsService` for aggregated insights

### 2. Bypassing Tenant Limits
❌ **Wrong:** Disabling runtime enforcement interceptor
✅ **Correct:** Work within tenant quotas or request limit increases

## Canonicality Status

| Component | Status | Notes |
|-----------|--------|-------|
| `PortalInsightsService` | ✅ Canonical | Dashboard data aggregation |
| `EnterpriseDashboardService` | ✅ Canonical | Enterprise metrics |
| `TenantRuntimeEnforcementInterceptor` | ✅ Canonical | Request enforcement |

## Security Requirements
- **Roles:** `ROLE_ADMIN` for portal access
- **HR Integration:** HR module must be enabled for workforce insights
- **Tenant Isolation:** All data filtered by company context
