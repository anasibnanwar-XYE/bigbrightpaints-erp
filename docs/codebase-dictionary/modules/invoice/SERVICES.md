# Invoice Services

## Overview

| Service | Purpose |
|---------|---------|
| InvoiceService | Invoice retrieval and lifecycle |
| InvoicePdfService | PDF generation |
| InvoiceNumberService | Invoice number sequencing |
| InvoiceSettlementPolicy | Payment application |
| SettlementApprovalDecision | Settlement approval (record type) |

---

## InvoiceService

**File**: `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/invoice/service/InvoiceService.java`

**Package**: `com.bigbrightpaints.erp.modules.invoice.service`

**Responsibility**: Invoice retrieval, creation, and linked reference building

### Dependencies
- CompanyContextService
- InvoiceRepository
- SalesOrderCrudService
- SalesOrderRepository
- CompanyEntityLookup
- PackagingSlipRepository
- PartnerSettlementAllocationRepository

### Public Methods

| Method | Signature | Description |
|--------|-----------|-------------|
| issueInvoiceForOrder | `InvoiceDto issueInvoiceForOrder(Long salesOrderId)` | Issue invoice for order |
| listInvoices | `List<InvoiceDto> listInvoices(int page, int size)` | Paginated invoice list |
| listDealerInvoices | `List<InvoiceDto> listDealerInvoices(Long dealerId, int page, int size)` | Paginated dealer invoices |
| getInvoice | `InvoiceDto getInvoice(Long id)` | Get invoice by ID |
| getInvoiceWithDealerEmail | `InvoiceWithEmail getInvoiceWithDealerEmail(Long id)` | Get invoice with dealer email |

### Side Effects
- DB reads: Invoice and related entity lookups
- No direct writes (invoices created by dispatch confirmation)

### Status
✅ **Canonical** - Invoice retrieval

---

## InvoicePdfService
**File**: `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/invoice/service/InvoicePdfService.java`

**Package**: `com.bigbrightpaints.erp.modules.invoice.service`

**Responsibility**: Generate PDF documents for invoices using Thymeleaf templates

### Dependencies
- CompanyContextService
- CompanyEntityLookup
- TemplateEngine

### Public Methods

| Method | Signature | Description |
|--------|-----------|-------------|
| renderInvoicePdf | `PdfDocument renderInvoicePdf(Long invoiceId)` | Render invoice PDF |

### Records

```java
record InvoiceLineView(String description, BigDecimal quantity, BigDecimal unitPrice, BigDecimal lineTotal, BigDecimal taxRate) {}
record InvoiceView(String companyName, String companyCode, String companyAddress, String companyGstin, String companyPhone, String invoiceNumber, String orderNumber, LocalDate issueDate, LocalDate dueDate, String billToName, String billToAddress, String billToPhone, List<InvoiceLineView> lines, BigDecimal subtotal, BigDecimal discount, BigDecimal tax, BigDecimal total, String currencySymbol) {}
record PdfDocument(String fileName, byte[] content) {}
```

### Status
✅ **Canonical** - PDF generation

---

## InvoiceNumberService
**File**: `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/invoice/service/InvoiceNumberService.java`

**Package**: `com.bigbrightpaints.erp.modules.invoice.service`

**Responsibility**: Generate sequential invoice numbers with company code and fiscal year

### Dependencies
- InvoiceSequenceRepository
- PlatformTransactionManager
- CompanyClock

### Public Methods

| Method | Signature | Description |
|--------|-----------|-------------|
| nextInvoiceNumber | `String nextInvoiceNumber(Company company)` | Generate next invoice number |

### Number Format
`{COMPANY_CODE}-INV-{FISCAL_YEAR}-{SEQUENCE:05d}`

Example: `BBP-INV-2024-00001`

### Status
✅ **Canonical** - Invoice numbering

---

## InvoiceSettlementPolicy
**File**: `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/invoice/service/InvoiceSettlementPolicy.java`

**Package**: `com.bigbrightpaints.erp.modules.invoice.service`

**Responsibility**: Centralized policy for invoice status transitions and payment handling

### Public Methods

| Method | Signature | Description |
|--------|-----------|-------------|
| ensureIssuable | `void ensureIssuable(Invoice invoice)` | Validate invoice can be issued |
| applyPayment | `void applyPayment(Invoice invoice, BigDecimal amount, String reference)` | Apply payment |
| applySettlement | `void applySettlement(Invoice invoice, BigDecimal clearedAmount, String reference)` | Apply settlement |
| applySettlementWithOverride | `void applySettlementWithOverride(Invoice invoice, BigDecimal appliedAmount, BigDecimal discountAmount, BigDecimal writeOffAmount, BigDecimal fxAdjustmentAmount, String reference, SettlementApprovalDecision approval)` | Settlement with override |
| updateStatusFromOutstanding | `void updateStatusFromOutstanding(Invoice invoice, BigDecimal outstanding)` | Update status |
| applyCredit | `void applyCredit(Invoice invoice, BigDecimal amount, String reference)` | Apply credit |
| reversePayment | `void reversePayment(Invoice invoice, BigDecimal amount, String reference)` | Reverse payment |
| voidInvoice | `void voidInvoice(Invoice invoice)` | Void invoice |
| isPastDue | `boolean isPastDue(Invoice invoice, LocalDate asOf)` | Check if overdue |

### Invoice Status Values
- `DRAFT` - Initial state
- `ISSUED` - Posted to accounting
- `PARTIAL` - Partially paid
- `PAID` - Fully paid
- `VOID` - Cancelled
- `REVERSED` - Accounting reversal

### Status
✅ **Canonical** - Payment application
