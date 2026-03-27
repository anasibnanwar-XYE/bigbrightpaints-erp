# Invoice Module Overview

**Module Path:** `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/invoice/`  
**Package Root:** `com.bigbrightpaints.erp.modules.invoice`

## Purpose

The Invoice module provides invoice generation and management capabilities including:
- Invoice creation from sales orders and dispatch confirmations
- Invoice PDF generation
- Invoice email delivery
- Invoice number sequence generation
- Payment/settlement application and tracking

## Module Boundaries

### Inbound Dependencies
- **Company** (`modules/company`) - Tenant context
- **Sales** (`modules/sales`) - Sales orders, dealers
- **Inventory** (`modules/inventory`) - Packaging slips
- **Accounting** (`modules/accounting`) - Journal entries, settlements

### Outbound Dependencies
- **Email Service** (`core/notification`) - Invoice email delivery
- **Audit Service** (`core/audit`) - Export logging

## Architecture Layers

```
modules/invoice/
├── controller/     # REST endpoints (1 controller)
├── service/        # Business logic (6 services)
├── domain/         # Entities & repositories (5 classes)
└── dto/            # Data transfer objects (2 classes)
```

## Key Design Patterns

### 1. Invoice Generation Flow
1. Dispatch confirmation creates invoice
2. Invoice number generated via `InvoiceNumberService`
3. Invoice linked to sales order and packaging slip

### 2. Settlement Policy
- `InvoiceSettlementPolicy` manages status transitions
- Idempotent payment application via reference tracking
- Status updates based on outstanding amount

### 3. Invoice Number Sequence
- Format: `{COMPANY_CODE}-INV-{FISCAL_YEAR}-{SEQUENCE}`
- Example: `BBP-INV-2025-00001`
- Retry on concurrent access conflicts

## Invoice Status Flow

```
DRAFT → ISSUED → PARTIAL → PAID
                ↘ VOID
                ↘ REVERSED
```

## Anti-Patterns to Avoid

### 1. Direct Invoice Creation
❌ **Wrong:** Creating invoices without dispatch confirmation
✅ **Correct:** Invoices are created by dispatch confirmation workflow

### 2. Duplicate Payment Application
❌ **Wrong:** Applying same payment reference twice
✅ **Correct:** Use `applyPayment()` which is idempotent

## Canonicality Status

| Component | Status | Notes |
|-----------|--------|-------|
| `InvoiceService` | ✅ Canonical | Invoice retrieval |
| `InvoicePdfService` | ✅ Canonical | PDF generation |
| `InvoiceNumberService` | ✅ Canonical | Invoice numbering |
| `InvoiceSettlementPolicy` | ✅ Canonical | Payment application |
| `Invoice` entity | ✅ Canonical | Invoice persistence |

## Security Requirements

- **Roles:** `ROLE_ADMIN`, `ROLE_SALES`, `ROLE_ACCOUNTING` for viewing
- **Admin Only:** PDF download
- **Tenant Isolation:** All queries filtered by company
