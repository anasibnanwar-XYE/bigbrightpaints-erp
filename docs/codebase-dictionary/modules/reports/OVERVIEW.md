# Reports Module Overview

**Module Path:** `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/reports/`  
**Package Root:** `com.bigbrightpaints.erp.modules.reports`

## Purpose

The Reports module provides financial reporting capabilities including:
- Trial Balance report
- Profit & Loss statement
- Balance Sheet report
- Cash Flow statement
- Inventory Valuation
- GST Return report
- A aging reports (AR/AP)
- Production cost reports

- Export approval workflow

## Module Boundaries

### Inbound Dependencies
- **Accounting** (`modules/accounting`) - Account balances, journal entries
- **Inventory** (`modules/inventory`) - Inventory movements
- **Sales** (`modules/sales`) - Sales orders, dealers
- **Invoice** (`modules/invoice`) - Invoice data
- **Factory** (`modules/factory`) - Production logs, packing records
- **Purchasing** (`modules/purchasing`) - Purchase data
- **HR** (`modules/hr`) - Payroll data

- **Admin** (`modules/admin`) - Export approvals

### Outbound Dependencies
- None (pure read module)

## Architecture Layers

```
modules/reports/
├── controller/     # REST endpoints (1 controller)
├── service/        # Business logic (9 services)
└── dto/            # Data transfer objects (16 classes)
```

modules/reports/
├── controller/     # REST endpoints (1 controller)
│   ├── ReportController.java
├── service/        # Business logic (9 services)
│   ├── ReportService.java (main)
│   ├── TrialBalanceReportQueryService.java
│   ├── ProfitLossReportQueryService.java
│   ├── BalanceSheetReportQueryService.java
│   ├── A400 A │   ├── A300  |
│   | | |   ├── 200  |
│   | | |   ├── 100  |
│   | | |   ├── 50  |
│   | | |   ├── 25  |
│   | | |   ├── 10  |
│   | | |   ├── 5  |

## Key Design Patterns

### 1. Period-Aware Reporting
- Reports can use accounting periods as query windows
- Closed period snapshots used for efficient reporting
- Comparative periods supported for year-over-year analysis

- |
| InvoiceStatus | DRAFT | ISSUED | PARTIAL | PAID | VOID | REVERSED |
