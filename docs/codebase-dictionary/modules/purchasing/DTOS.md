# Purchasing Module DTOs

## Overview

The Purchasing module contains 17 Data Transfer Objects (DTOs) organized into Request and Response records for API contracts.

---

## DTO Index

### Request DTOs (8)

| DTO | Description |
|-----|-------------|
| `SupplierRequest` | Create/update supplier |
| `PurchaseOrderRequest` | Create purchase order |
| `PurchaseOrderLineRequest` | PO line item |
| `PurchaseOrderVoidRequest` | Void PO with reason |
| `GoodsReceiptRequest` | Create goods receipt |
| `GoodsReceiptLineRequest` | Receipt line item |
| `RawMaterialPurchaseRequest` | Create purchase invoice |
| `RawMaterialPurchaseLineRequest` | Invoice line item |
| `PurchaseReturnRequest` | Purchase return |

### Response DTOs (8)

| DTO | Description |
|-----|-------------|
| `SupplierResponse` | Supplier data |
| `PurchaseOrderResponse` | Purchase order data |
| `PurchaseOrderLineResponse` | PO line item |
| `PurchaseOrderStatusHistoryResponse` | Status history entry |
| `GoodsReceiptResponse` | Goods receipt data |
| `GoodsReceiptLineResponse` | Receipt line item |
| `RawMaterialPurchaseResponse` | Purchase invoice data |
| `RawMaterialPurchaseLineResponse` | Invoice line item |
| `PurchaseReturnPreviewDto` | Return preview |

---

## Request DTOs

### SupplierRequest

**File:** `dto/SupplierRequest.java`

```java
public record SupplierRequest(
    @NotBlank @Size(max = 64) String name,
    @Size(max = 64) String code,
    @Email String contactEmail,
    @Size(max = 32) String contactPhone,
    @Size(max = 512) String address,
    @DecimalMin(value = "0.00") BigDecimal creditLimit,
    @Pattern(regexp = "^$|[0-9]{2}[A-Za-z0-9]{13}$") String gstNumber,
    @Pattern(regexp = "^$|[A-Za-z0-9]{2}$") String stateCode,
    GstRegistrationType gstRegistrationType,
    SupplierPaymentTerms paymentTerms,
    @Size(max = 128) String bankAccountName,
    @Size(max = 64) String bankAccountNumber,
    @Size(max = 32) String bankIfsc,
    @Size(max = 128) String bankBranch
) {}
```

| Field | Type | Validation | Description |
|-------|------|------------|-------------|
| `name` | String | @NotBlank, max 64 | Supplier name (required) |
| `code` | String | max 64 | Supplier code (auto-generated if empty) |
| `contactEmail` | String | @Email | Contact email |
| `contactPhone` | String | max 32 | Contact phone |
| `address` | String | max 512 | Address |
| `creditLimit` | BigDecimal | >= 0 | Credit limit |
| `gstNumber` | String | GSTIN pattern | 15-char GST number |
| `stateCode` | String | 2 chars | State code |
| `gstRegistrationType` | GstRegistrationType | - | GST type |
| `paymentTerms` | SupplierPaymentTerms | - | Payment terms |
| `bankAccountName` | String | max 128 | Bank account name |
| `bankAccountNumber` | String | max 64 | Bank account number |
| `bankIfsc` | String | max 32 | IFSC code |
| `bankBranch` | String | max 128 | Bank branch |

---

### PurchaseOrderRequest

**File:** `dto/PurchaseOrderRequest.java`

```java
public record PurchaseOrderRequest(
    @NotNull Long supplierId,
    @NotBlank String orderNumber,
    @NotNull LocalDate orderDate,
    String memo,
    @NotEmpty List<@Valid PurchaseOrderLineRequest> lines
) {}
```

| Field | Type | Validation | Description |
|-------|------|------------|-------------|
| `supplierId` | Long | @NotNull | Supplier ID |
| `orderNumber` | String | @NotBlank | PO number |
| `orderDate` | LocalDate | @NotNull | Order date |
| `memo` | String | - | Notes |
| `lines` | List<PurchaseOrderLineRequest> | @NotEmpty | Line items |

---

### PurchaseOrderLineRequest

**File:** `dto/PurchaseOrderLineRequest.java`

```java
public record PurchaseOrderLineRequest(
    @NotNull Long rawMaterialId,
    @NotNull @Positive BigDecimal quantity,
    String unit,
    @NotNull @Positive BigDecimal costPerUnit,
    String notes
) {}
```

| Field | Type | Validation | Description |
|-------|------|------------|-------------|
| `rawMaterialId` | Long | @NotNull | Material ID |
| `quantity` | BigDecimal | @Positive | Quantity |
| `unit` | String | - | Unit (defaults to material unit) |
| `costPerUnit` | BigDecimal | @Positive | Unit cost |
| `notes` | String | - | Line notes |

---

### PurchaseOrderVoidRequest

**File:** `dto/PurchaseOrderVoidRequest.java`

```java
public record PurchaseOrderVoidRequest(
    @NotBlank String reasonCode,
    String reason
) {}
```

| Field | Type | Validation | Description |
|-------|------|------------|-------------|
| `reasonCode` | String | @NotBlank | Reason code for voiding |
| `reason` | String | - | Detailed reason |

---

### GoodsReceiptRequest

**File:** `dto/GoodsReceiptRequest.java`

```java
public record GoodsReceiptRequest(
    @NotNull Long purchaseOrderId,
    @NotBlank String receiptNumber,
    @NotNull LocalDate receiptDate,
    String memo,
    String idempotencyKey,
    @NotEmpty List<@Valid GoodsReceiptLineRequest> lines
) {}
```

| Field | Type | Validation | Description |
|-------|------|------------|-------------|
| `purchaseOrderId` | Long | @NotNull | Related PO ID |
| `receiptNumber` | String | @NotBlank | Receipt number |
| `receiptDate` | LocalDate | @NotNull | Receipt date |
| `memo` | String | - | Notes |
| `idempotencyKey` | String | - | Idempotency key (also via header) |
| `lines` | List<GoodsReceiptLineRequest> | @NotEmpty | Line items |

---

### GoodsReceiptLineRequest

**File:** `dto/GoodsReceiptLineRequest.java`

```java
public record GoodsReceiptLineRequest(
    @NotNull Long rawMaterialId,
    String batchCode,
    @NotNull @Positive BigDecimal quantity,
    String unit,
    @NotNull @Positive BigDecimal costPerUnit,
    LocalDate manufacturingDate,
    LocalDate expiryDate,
    String notes
) {}
```

| Field | Type | Validation | Description |
|-------|------|------------|-------------|
| `rawMaterialId` | Long | @NotNull | Material ID |
| `batchCode` | String | - | Batch code (auto-generated if empty) |
| `quantity` | BigDecimal | @Positive | Received quantity |
| `unit` | String | - | Unit (must match PO) |
| `costPerUnit` | BigDecimal | @Positive | Unit cost |
| `manufacturingDate` | LocalDate | - | Manufacturing date |
| `expiryDate` | LocalDate | - | Expiry date |
| `notes` | String | - | Line notes |

---

### RawMaterialPurchaseRequest

**File:** `dto/RawMaterialPurchaseRequest.java`

```java
public record RawMaterialPurchaseRequest(
    @NotNull Long supplierId,
    @NotBlank String invoiceNumber,
    @NotNull LocalDate invoiceDate,
    String memo,
    Long purchaseOrderId,
    @NotNull Long goodsReceiptId,
    @PositiveOrZero BigDecimal taxAmount,
    @NotEmpty List<@Valid RawMaterialPurchaseLineRequest> lines
) {}
```

| Field | Type | Validation | Description |
|-------|------|------------|-------------|
| `supplierId` | Long | @NotNull | Supplier ID |
| `invoiceNumber` | String | @NotBlank | Invoice number |
| `invoiceDate` | LocalDate | @NotNull | Invoice date |
| `memo` | String | - | Notes |
| `purchaseOrderId` | Long | - | PO ID (optional, derived from receipt) |
| `goodsReceiptId` | Long | @NotNull | Goods receipt ID |
| `taxAmount` | BigDecimal | @PositiveOrZero | Total tax (or computed per-line) |
| `lines` | List<RawMaterialPurchaseLineRequest> | @NotEmpty | Line items |

**Note:** Legacy field aliases (invoiceNo, grnId, etc.) are explicitly rejected.

---

### RawMaterialPurchaseLineRequest

**File:** `dto/RawMaterialPurchaseLineRequest.java`

```java
public record RawMaterialPurchaseLineRequest(
    @NotNull Long rawMaterialId,
    String batchCode,
    @NotNull @Positive BigDecimal quantity,
    String unit,
    @NotNull @Positive BigDecimal costPerUnit,
    BigDecimal taxRate,
    Boolean taxInclusive,
    String notes
) {}
```

| Field | Type | Validation | Description |
|-------|------|------------|-------------|
| `rawMaterialId` | Long | @NotNull | Material ID |
| `batchCode` | String | - | Batch code |
| `quantity` | BigDecimal | @Positive | Quantity (must match receipt) |
| `unit` | String | - | Unit (must match receipt) |
| `costPerUnit` | BigDecimal | @Positive | Unit cost (must match receipt) |
| `taxRate` | BigDecimal | - | Tax rate override |
| `taxInclusive` | Boolean | - | Whether price is tax-inclusive |
| `notes` | String | - | Line notes |

---

### PurchaseReturnRequest

**File:** `dto/PurchaseReturnRequest.java`

```java
public record PurchaseReturnRequest(
    @NotNull Long supplierId,
    @NotNull Long purchaseId,
    @NotNull Long rawMaterialId,
    @NotNull @Positive BigDecimal quantity,
    @NotNull @Positive BigDecimal unitCost,
    String referenceNumber,
    LocalDate returnDate,
    String reason
) {}
```

| Field | Type | Validation | Description |
|-------|------|------------|-------------|
| `supplierId` | Long | @NotNull | Supplier ID |
| `purchaseId` | Long | @NotNull | Purchase invoice ID |
| `rawMaterialId` | Long | @NotNull | Material ID |
| `quantity` | BigDecimal | @Positive | Return quantity |
| `unitCost` | BigDecimal | @Positive | Unit cost |
| `referenceNumber` | String | - | Reference (auto-generated if empty) |
| `returnDate` | LocalDate | - | Return date (defaults to today) |
| `reason` | String | - | Return reason |

---

## Response DTOs

### SupplierResponse

**File:** `dto/SupplierResponse.java`

```java
public record SupplierResponse(
    Long id,
    UUID publicId,
    String code,
    String name,
    SupplierStatus status,
    String email,
    String phone,
    String address,
    BigDecimal creditLimit,
    BigDecimal outstandingBalance,
    Long payableAccountId,
    String payableAccountCode,
    String gstNumber,
    String stateCode,
    GstRegistrationType gstRegistrationType,
    SupplierPaymentTerms paymentTerms,
    String bankAccountName,
    String bankAccountNumber,
    String bankIfsc,
    String bankBranch
) {}
```

---

### PurchaseOrderResponse

**File:** `dto/PurchaseOrderResponse.java`

```java
public record PurchaseOrderResponse(
    Long id,
    UUID publicId,
    String orderNumber,
    LocalDate orderDate,
    BigDecimal totalAmount,
    String status,
    String memo,
    Long supplierId,
    String supplierCode,
    String supplierName,
    Instant createdAt,
    List<PurchaseOrderLineResponse> lines
) {}
```

---

### PurchaseOrderLineResponse

**File:** `dto/PurchaseOrderLineResponse.java`

```java
public record PurchaseOrderLineResponse(
    Long rawMaterialId,
    String rawMaterialName,
    BigDecimal quantity,
    String unit,
    BigDecimal costPerUnit,
    BigDecimal lineTotal,
    String notes
) {}
```

---

### PurchaseOrderStatusHistoryResponse

**File:** `dto/PurchaseOrderStatusHistoryResponse.java`

```java
public record PurchaseOrderStatusHistoryResponse(
    Long id,
    String fromStatus,
    String toStatus,
    String reasonCode,
    String reason,
    String changedBy,
    Instant changedAt
) {}
```

---

### GoodsReceiptResponse

**File:** `dto/GoodsReceiptResponse.java`

```java
public record GoodsReceiptResponse(
    Long id,
    UUID publicId,
    String receiptNumber,
    LocalDate receiptDate,
    BigDecimal totalAmount,
    String status,
    String memo,
    Long supplierId,
    String supplierCode,
    String supplierName,
    Long purchaseOrderId,
    String purchaseOrderNumber,
    Instant createdAt,
    List<GoodsReceiptLineResponse> lines,
    DocumentLifecycleDto lifecycle,
    List<LinkedBusinessReferenceDto> linkedReferences
) {}
```

**Extended Fields:**
- `lifecycle` - Document lifecycle state
- `linkedReferences` - Links to PO, purchase invoice, journal

---

### GoodsReceiptLineResponse

**File:** `dto/GoodsReceiptLineResponse.java`

```java
public record GoodsReceiptLineResponse(
    Long rawMaterialId,
    String rawMaterialName,
    String batchCode,
    BigDecimal quantity,
    String unit,
    BigDecimal costPerUnit,
    BigDecimal lineTotal,
    String notes
) {}
```

---

### RawMaterialPurchaseResponse

**File:** `dto/RawMaterialPurchaseResponse.java`

```java
public record RawMaterialPurchaseResponse(
    Long id,
    UUID publicId,
    String invoiceNumber,
    LocalDate invoiceDate,
    BigDecimal totalAmount,
    BigDecimal taxAmount,
    BigDecimal outstandingAmount,
    String status,
    String memo,
    Long supplierId,
    String supplierCode,
    String supplierName,
    Long purchaseOrderId,
    String purchaseOrderNumber,
    Long goodsReceiptId,
    String goodsReceiptNumber,
    Long journalEntryId,
    Instant createdAt,
    List<RawMaterialPurchaseLineResponse> lines,
    DocumentLifecycleDto lifecycle,
    List<LinkedBusinessReferenceDto> linkedReferences
) {}
```

**Extended Fields:**
- `lifecycle` - Document lifecycle state
- `linkedReferences` - Links to PO, receipt, journal, settlements

---

### RawMaterialPurchaseLineResponse

**File:** `dto/RawMaterialPurchaseLineResponse.java`

```java
public record RawMaterialPurchaseLineResponse(
    Long rawMaterialId,
    String rawMaterialName,
    Long rawMaterialBatchId,
    String batchCode,
    BigDecimal quantity,
    String unit,
    BigDecimal costPerUnit,
    BigDecimal lineTotal,
    BigDecimal taxRate,
    BigDecimal taxAmount,
    String notes,
    BigDecimal cgstAmount,
    BigDecimal sgstAmount,
    BigDecimal igstAmount
) {}
```

**GST Fields:**
- `cgstAmount` - Central GST (intra-state)
- `sgstAmount` - State GST (intra-state)
- `igstAmount` - Integrated GST (inter-state)

---

### PurchaseReturnPreviewDto

**File:** `dto/PurchaseReturnPreviewDto.java`

```java
public record PurchaseReturnPreviewDto(
    Long purchaseId,
    String purchaseInvoiceNumber,
    Long rawMaterialId,
    String rawMaterialName,
    BigDecimal requestedQuantity,
    BigDecimal remainingReturnableQuantity,
    BigDecimal lineAmount,
    BigDecimal taxAmount,
    BigDecimal totalAmount,
    LocalDate returnDate,
    String referenceNumber
) {}
```

| Field | Description |
|-------|-------------|
| `purchaseId` | Source purchase invoice ID |
| `purchaseInvoiceNumber` | Invoice number |
| `rawMaterialId` | Material being returned |
| `rawMaterialName` | Material name |
| `requestedQuantity` | Quantity to return |
| `remainingReturnableQuantity` | Remaining after this return |
| `lineAmount` | Net line amount |
| `taxAmount` | Tax credit reversal |
| `totalAmount` | Total return amount |
| `returnDate` | Proposed return date |
| `referenceNumber` | Generated reference |

---

## Shared DTOs

These DTOs are defined in the shared module and referenced by purchasing responses:

### DocumentLifecycleDto

```java
public record DocumentLifecycleDto(
    String status,
    String eligibility
) {}
```

### LinkedBusinessReferenceDto

```java
public record LinkedBusinessReferenceDto(
    String relationshipType,
    String documentType,
    Long documentId,
    String documentNumber,
    DocumentLifecycleDto lifecycle,
    Long journalEntryId
) {}
```
