# Invoice Controllers

## Overview

| Controller | Endpoints | Purpose |
|------------|-----------|---------|
| InvoiceController | 5 | Invoice management |

---

## InvoiceController

**File**: `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/invoice/controller/InvoiceController.java`

**Package**: `com.bigbrightpaints.erp.modules.invoice.controller`

**Base Path**: `/api/v1/invoices`

**Dependencies**:
- InvoiceService
- InvoicePdfService
- EmailService
- AuditService

### Endpoints

| Method | Path | Signature | Description |
|--------|------|-----------|-------------|
| GET | `/` | `ResponseEntity<ApiResponse<List<InvoiceDto>>> listInvoices(@RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="100") int size)` | List invoices |
| GET | `/{id}` | `ResponseEntity<ApiResponse<InvoiceDto>> getInvoice(@PathVariable Long id)` | Get invoice by ID |
| GET | `/{id}/pdf` | `ResponseEntity<byte[]> downloadInvoicePdf(@PathVariable Long id)` | Download PDF (admin only) |
| GET | `/dealers/{dealerId}` | `ResponseEntity<ApiResponse<List<InvoiceDto>>> dealerInvoices(@PathVariable Long dealerId, @RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="100") int size)` | List dealer invoices |
| POST | `/{id}/email` | `ResponseEntity<ApiResponse<String>> sendInvoiceEmail(@PathVariable Long id)` | Email invoice to dealer |
