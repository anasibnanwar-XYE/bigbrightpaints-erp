# Sales Module - DTOs

## Overview

The Sales module contains 26 DTOs (Data Transfer Objects) organized by function:
- **Request DTOs**: Input payloads for API operations
- **Response DTOs**: Output payloads for API responses
- **Internal DTOs**: Data structures for internal operations

Package: `com.bigbrightpaints.erp.modules.sales.dto`
Directory: `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/sales/dto/`

---

## Request DTOs

### SalesOrderRequest

**Purpose:** Create/update sales order payload

**Fields:**
| Field | Type | Nullable | Validation | Description |
|-------|------|----------|------------|-------------|
| `dealerId` | Long | Yes | - | Dealer ID |
| `totalAmount` | BigDecimal | No | @NotNull | Order total |
| `currency` | String | Yes | - | Currency code (default: INR) |
| `notes` | String | Yes | - | Order notes |
| `items` | List | No | @NotEmpty, @Valid | Line items |
| `gstTreatment` | String | Yes | - | GST treatment |
| `gstRate` | BigDecimal | Yes | - | GST rate |
| `gstInclusive` | Boolean | Yes | - | GST inclusive flag |
| `idempotencyKey` | String | Yes | - | Idempotency key |
| `paymentMode` | String | Yes | - | Payment mode (CREDIT, CASH, HYBRID) |

**Methods:**
- `normalizedPaymentMode()` - Normalizes payment mode
- `resolveIdempotencyKey()` - Resolves idempotency key

---

### SalesOrderItemRequest

**Purpose:** Line item in sales order request

**Fields:**
| Field | Type | Nullable | Validation | Description |
|-------|------|----------|------------|-------------|
| `productCode` | String | No | @NotBlank | Product code |
| `quantity` | BigDecimal | No | @NotNull | Quantity |
| `unitPrice` | BigDecimal | No | @NotNull | Unit price |
| `gstRate` | BigDecimal | Yes | - | GST rate |
| `description` | String | Yes | - | Description |

---

### CreateDealerRequest

**Purpose:** Create dealer payload

**Fields:**
| Field | Type | Nullable | Validation | Description |
|-------|------|----------|------------|-------------|
| `name` | String | No | @NotBlank | Dealer name |
| `companyName` | String | No | @NotBlank | Company name |
| `contactEmail` | String | No | @Email, @NotBlank | Contact email |
| `contactPhone` | String | No | @NotBlank | Phone number |
| `address` | String | Yes | - | Address |
| `creditLimit` | BigDecimal | Yes | @PositiveOrZero | Credit limit |
| `gstNumber` | String | Yes | @Pattern (GSTIN) | GST number |
| `stateCode` | String | Yes | @Pattern (2 chars) | State code |
| `gstRegistrationType` | GstRegistrationType | Yes | - | GST type |
| `paymentTerms` | DealerPaymentTerms | Yes | - | Payment terms |
| `region` | String | Yes | - | Region |

---

### DealerRequest

**Purpose:** Internal dealer request (simplified)

**Fields:**
| Field | Type | Nullable | Description |
|-------|------|----------|-------------|
| `code` | String | No | Dealer code |
| `name` | String | No | Dealer name |
| `email` | String | Yes | Email |
| `phone` | String | Yes | Phone |
| `gstNumber` | String | Yes | GST number |
| `stateCode` | String | Yes | State code |
| `gstRegistrationType` | GstRegistrationType | Yes | GST type |
| `creditLimit` | BigDecimal | Yes | Credit limit |

---

### DispatchConfirmRequest

**Purpose:** Dispatch confirmation payload

**Fields:**
| Field | Type | Nullable | Description |
|-------|------|----------|-------------|
| `packingSlipId` | Long | Yes | Packaging slip ID |
| `orderId` | Long | Yes | Sales order ID |
| `lines` | List<DispatchLine> | Yes | Dispatch lines |
| `dispatchNotes` | String | Yes | Notes |
| `confirmedBy` | String | Yes | Confirmer |
| `adminOverrideCreditLimit` | Boolean | Yes | Credit override flag |
| `overrideReason` | String | Yes | Override reason |
| `overrideRequestId` | Long | Yes | Override request ID |
| `transporterName` | String | Yes | Transporter |
| `driverName` | String | Yes | Driver name |
| `vehicleNumber` | String | Yes | Vehicle number |
| `challanReference` | String | Yes | Challan reference |

**Nested DispatchLine:**
| Field | Type | Nullable | Description |
|-------|------|----------|-------------|
| `lineId` | Long | Yes | Order line ID |
| `batchId` | Long | Yes | Batch ID |
| `shipQty` | BigDecimal | No | Quantity to ship |
| `priceOverride` | BigDecimal | Yes | Price override |
| `discount` | BigDecimal | Yes | Discount |
| `taxRate` | BigDecimal | Yes | Tax rate |
| `taxInclusive` | Boolean | Yes | Tax inclusive flag |
| `notes` | String | Yes | Notes |

---

### PromotionRequest

**Purpose:** Create/update promotion payload

**Fields:**
| Field | Type | Nullable | Description |
|-------|------|----------|-------------|
| `name` | String | No | Promotion name |
| `description` | String | Yes | Description |
| `imageUrl` | String | Yes | Image URL |
| `discountType` | String | No | Discount type |
| `discountValue` | BigDecimal | No | Discount value |
| `startDate` | LocalDate | No | Start date |
| `endDate` | LocalDate | No | End date |

---

### SalesTargetRequest

**Purpose:** Create/update sales target payload

**Fields:**
| Field | Type | Nullable | Description |
|-------|------|----------|-------------|
| `name` | String | No | Target name |
| `periodStart` | LocalDate | No | Period start |
| `periodEnd` | LocalDate | No | Period end |
| `targetAmount` | BigDecimal | No | Target amount |
| `assignee` | String | Yes | Assigned person |

---

### CreditLimitRequestCreateRequest

**Purpose:** Create credit limit increase request

**Fields:**
| Field | Type | Nullable | Description |
|-------|------|----------|-------------|
| `dealerId` | Long | No | Dealer ID |
| `amountRequested` | BigDecimal | No | Amount requested |
| `reason` | String | Yes | Reason |

---

### DealerPortalCreditLimitRequestCreateRequest

**Purpose:** Dealer portal credit limit request

**Fields:**
| Field | Type | Nullable | Description |
|-------|------|----------|-------------|
| `amountRequested` | BigDecimal | No | Amount requested |
| `reason` | String | Yes | Reason |

---

### CreditLimitOverrideRequestCreateRequest

**Purpose:** Create credit limit override request

**Fields:**
| Field | Type | Nullable | Description |
|-------|------|----------|-------------|
| `dealerId` | Long | Yes | Dealer ID |
| `packagingSlipId` | Long | Yes | Packaging slip ID |
| `salesOrderId` | Long | Yes | Sales order ID |
| `dispatchAmount` | BigDecimal | No | Dispatch amount |
| `reason` | String | No | Reason |
| `expiresAt` | Instant | Yes | Expiration time |

---

### CreditLimitRequestDecisionRequest

**Purpose:** Approve/reject credit limit request

**Fields:**
| Field | Type | Nullable | Description |
|-------|------|----------|-------------|
| `reason` | String | Yes | Decision reason |

---

### CreditLimitOverrideDecisionRequest

**Purpose:** Approve/reject credit override request

**Fields:**
| Field | Type | Nullable | Description |
|-------|------|----------|-------------|
| `reason` | String | Yes | Decision reason |
| `expiresAt` | Instant | Yes | Expiration override |

---

### DealerPortalCreditRequestCreateRequest

**Purpose:** Dealer portal credit request

**Fields:**
| Field | Type | Nullable | Description |
|-------|------|----------|-------------|
| `amountRequested` | BigDecimal | No | Amount requested |
| `reason` | String | Yes | Reason |

---

## Response DTOs

### SalesOrderDto

**Purpose:** Sales order response

**Fields:**
| Field | Type | Description |
|-------|------|-------------|
| `id` | Long | Order ID |
| `publicId` | UUID | Public ID |
| `orderNumber` | String | Order number |
| `status` | String | Current status |
| `totalAmount` | BigDecimal | Total amount |
| `subtotalAmount` | BigDecimal | Subtotal |
| `gstTotal` | BigDecimal | GST total |
| `gstRate` | BigDecimal | GST rate |
| `gstTreatment` | String | GST treatment |
| `gstInclusive` | boolean | GST inclusive flag |
| `gstRoundingAdjustment` | BigDecimal | Rounding adjustment |
| `currency` | String | Currency |
| `dealerName` | String | Dealer name |
| `paymentMode` | String | Payment mode |
| `traceId` | String | Trace ID |
| `createdAt` | Instant | Creation timestamp |
| `items` | List<SalesOrderItemDto> | Line items |
| `timeline` | List<SalesOrderStatusHistoryDto> | Status history |

---

### SalesOrderItemDto

**Purpose:** Order line item response

**Fields:**
| Field | Type | Description |
|-------|------|-------------|
| `id` | Long | Item ID |
| `productCode` | String | Product code |
| `description` | String | Description |
| `quantity` | BigDecimal | Quantity |
| `unitPrice` | BigDecimal | Unit price |
| `lineSubtotal` | BigDecimal | Subtotal |
| `lineTotal` | BigDecimal | Total with tax |
| `gstRate` | BigDecimal | GST rate |
| `gstAmount` | BigDecimal | GST amount |

---

### DealerResponse

**Purpose:** Dealer response with full details

**Fields:**
| Field | Type | Description |
|-------|------|-------------|
| `id` | Long | Dealer ID |
| `publicId` | UUID | Public ID |
| `code` | String | Dealer code |
| `name` | String | Dealer name |
| `companyName` | String | Company name |
| `email` | String | Email |
| `phone` | String | Phone |
| `address` | String | Address |
| `creditLimit` | BigDecimal | Credit limit |
| `outstandingBalance` | BigDecimal | Outstanding balance |
| `receivableAccountId` | Long | Receivable account ID |
| `receivableAccountCode` | String | Account code |
| `portalEmail` | String | Portal email |
| `gstNumber` | String | GST number |
| `stateCode` | String | State code |
| `gstRegistrationType` | GstRegistrationType | GST type |
| `paymentTerms` | DealerPaymentTerms | Payment terms |
| `region` | String | Region |

---

### DealerDto

**Purpose:** Simplified dealer response

**Fields:**
| Field | Type | Description |
|-------|------|-------------|
| `id` | Long | Dealer ID |
| `publicId` | UUID | Public ID |
| `name` | String | Dealer name |
| `code` | String | Dealer code |
| `email` | String | Email |
| `phone` | String | Phone |
| `status` | String | Status |
| `creditLimit` | BigDecimal | Credit limit |
| `outstandingBalance` | BigDecimal | Outstanding balance |
| `gstNumber` | String | GST number |
| `stateCode` | String | State code |
| `gstRegistrationType` | GstRegistrationType | GST type |

---

### DealerLookupResponse

**Purpose:** Dealer search/lookup result

**Fields:**
| Field | Type | Description |
|-------|------|-------------|
| `id` | Long | Dealer ID |
| `publicId` | UUID | Public ID |
| `name` | String | Dealer name |
| `code` | String | Dealer code |
| `outstandingBalance` | BigDecimal | Outstanding balance |
| `creditLimit` | BigDecimal | Credit limit |
| `receivableAccountId` | Long | Account ID |
| `receivableAccountCode` | String | Account code |
| `stateCode` | String | State code |
| `gstRegistrationType` | GstRegistrationType | GST type |
| `paymentTerms` | DealerPaymentTerms | Payment terms |
| `region` | String | Region |
| `creditStatus` | String | Credit status (WITHIN_LIMIT, NEAR_LIMIT, OVER_LIMIT) |

---

### SalesDashboardDto

**Purpose:** Sales dashboard metrics

**Fields:**
| Field | Type | Description |
|-------|------|-------------|
| `activeDealers` | long | Active dealer count |
| `totalOrders` | long | Total order count |
| `orderStatusBuckets` | Map<String, Long> | Orders by status bucket |
| `pendingCreditRequests` | long | Pending credit requests |

---

### DispatchConfirmResponse

**Purpose:** Dispatch confirmation result

**Fields:**
| Field | Type | Description |
|-------|------|-------------|
| `packingSlipId` | Long | Packaging slip ID |
| `salesOrderId` | Long | Sales order ID |
| `finalInvoiceId` | Long | Invoice ID |
| `arJournalEntryId` | Long | AR journal ID |
| `cogsPostings` | List<CogsPostingDto> | COGS postings |
| `dispatched` | boolean | Dispatch flag |
| `arPostings` | List<AccountPostingDto> | AR postings |
| `gstBreakdown` | GstBreakdownDto | GST breakdown |

**Nested DTOs:**
- `CogsPostingDto(inventoryAccountId, cogsAccountId, cost)`
- `AccountPostingDto(accountId, accountName, debit, credit)`
- `GstBreakdownDto(taxableAmount, cgst, sgst, igst, totalTax)`

---

### DispatchMarkerReconciliationResponse

**Purpose:** Order marker reconciliation result

---

### SalesOrderStatusHistoryDto

**Purpose:** Order status history entry

**Fields:**
| Field | Type | Description |
|-------|------|-------------|
| `fromStatus` | String | Previous status |
| `toStatus` | String | New status |
| `reasonCode` | String | Reason code |
| `reason` | String | Reason text |
| `changedBy` | String | Actor |
| `changedAt` | Instant | Timestamp |

---

### CreditLimitRequestDto

**Purpose:** Credit limit request response

**Fields:**
| Field | Type | Description |
|-------|------|-------------|
| `id` | Long | Request ID |
| `publicId` | UUID | Public ID |
| `dealerName` | String | Dealer name |
| `amountRequested` | BigDecimal | Amount requested |
| `status` | String | Status (PENDING, APPROVED, REJECTED) |
| `reason` | String | Reason |
| `createdAt` | Instant | Creation timestamp |

---

### CreditLimitOverrideRequestDto

**Purpose:** Credit override request response

**Fields:**
| Field | Type | Description |
|-------|------|-------------|
| `id` | Long | Request ID |
| `publicId` | UUID | Public ID |
| `dealerId` | Long | Dealer ID |
| `dealerName` | String | Dealer name |
| `packagingSlipId` | Long | Packaging slip ID |
| `salesOrderId` | Long | Sales order ID |
| `dispatchAmount` | BigDecimal | Dispatch amount |
| `currentExposure` | BigDecimal | Current exposure |
| `creditLimit` | BigDecimal | Credit limit |
| `requiredHeadroom` | BigDecimal | Required headroom |
| `status` | String | Status |
| `reason` | String | Reason |
| `requestedBy` | String | Requester |
| `reviewedBy` | String | Reviewer |
| `reviewedAt` | Instant | Review timestamp |
| `expiresAt` | Instant | Expiration |
| `createdAt` | Instant | Creation timestamp |

---

### PromotionDto

**Purpose:** Promotion response

**Fields:**
| Field | Type | Description |
|-------|------|-------------|
| `id` | Long | Promotion ID |
| `publicId` | UUID | Public ID |
| `name` | String | Name |
| `description` | String | Description |
| `imageUrl` | String | Image URL |
| `discountType` | String | Discount type |
| `discountValue` | BigDecimal | Discount value |
| `startDate` | LocalDate | Start date |
| `endDate` | LocalDate | End date |
| `status` | String | Status |

---

### SalesTargetDto

**Purpose:** Sales target response

**Fields:**
| Field | Type | Description |
|-------|------|-------------|
| `id` | Long | Target ID |
| `publicId` | UUID | Public ID |
| `name` | String | Name |
| `periodStart` | LocalDate | Period start |
| `periodEnd` | LocalDate | Period end |
| `targetAmount` | BigDecimal | Target amount |
| `achievedAmount` | BigDecimal | Achieved amount |
| `assignee` | String | Assigned person |

---

## Internal DTOs

### SalesOrderSearchFilters

**Purpose:** Order search filter parameters

**Fields:**
| Field | Type | Description |
|-------|------|-------------|
| `status` | String | Filter by status |
| `dealerId` | Long | Filter by dealer |
| `orderNumber` | String | Filter by order number |
| `fromDate` | Instant | From date |
| `toDate` | Instant | To date |
| `page` | int | Page number |
| `size` | int | Page size |
