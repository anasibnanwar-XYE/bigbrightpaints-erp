# Sales Module - Services

## Overview

The Sales module contains 18 service classes organized into:
- **Core Services**: Primary business logic orchestrators
- **CRUD Services**: Data access wrappers
- **Lifecycle Services**: Status transitions
- **Domain Services**: Specialized business operations
- **Utility Services**: Supporting functionality

---

## Core Services

### SalesCoreEngine

**Package:** `com.bigbrightpaints.erp.modules.sales.service`
**File:** `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/sales/service/SalesCoreEngine.java`

**Purpose:** Central orchestrator for all sales operations. Contains the complete Order-to-Cash (O2C) flow logic.

**Responsibilities:**
- Sales order CRUD and lifecycle management
- Dealer CRUD with receivable account provisioning
- Credit limit enforcement
- Inventory reservation and dispatch coordination
- Invoice generation and posting
- Journal entry creation (AR, COGS)
- Promotions and targets management
- Dispatch confirmation and reconciliation

**Key Dependencies:**
| Dependency | Purpose |
|------------|---------|
| `CompanyContextService` | Multi-tenancy context |
| `DealerRepository` | Dealer persistence |
| `SalesOrderRepository` | Order persistence |
| `DealerLedgerService` | Dealer balances |
| `AccountingFacade` | Journal posting |
| `FinishedGoodsService` | Inventory operations |
| `InvoiceNumberService` | Invoice numbering |
| `GstService` | GST calculations |
| `CreditLimitOverrideService` | Override validation |

**Public Methods:**
```java
// Dealer operations
List<DealerDto> listDealers()
DealerDto createDealer(DealerRequest request)
DealerDto updateDealer(Long id, DealerRequest request)
void deleteDealer(Long id)

// Order operations
List<SalesOrderDto> listOrders(String status, int page, int size)
SalesOrderDto createOrder(SalesOrderRequest request)
SalesOrderDto updateOrder(Long id, SalesOrderRequest request)
void deleteOrder(Long id)
PageResponse<SalesOrderDto> searchOrders(SalesOrderSearchFilters filters)

// Order lifecycle
SalesOrderDto confirmOrder(Long id)
SalesOrderDto cancelOrder(Long id, String reason)
SalesOrderDto updateStatus(Long id, String status)
void updateOrchestratorWorkflowStatus(Long id, String status)

// Dispatch
DispatchConfirmResponse confirmDispatch(DispatchConfirmRequest request)
DispatchMarkerReconciliationResponse reconcileStaleOrderLevelMarkers(int limit)

// Dashboard & targets
SalesDashboardDto getDashboard()
List<PromotionDto> listPromotions()
List<SalesTargetDto> listTargets()
```

**Order Status Constants:**
- `DRAFT` → Initial state
- `CONFIRMED` → Ready for fulfillment
- `PROCESSING` → In warehouse
- `RESERVED` → Inventory reserved
- `PENDING_PRODUCTION` → Awaiting manufacturing
- `PENDING_INVENTORY` → Awaiting stock
- `READY_TO_SHIP` → Ready for dispatch
- `DISPATCHED` → Shipped
- `INVOICED` → Invoice generated
- `SETTLED` → Payment received
- `CANCELLED` → Cancelled
- `ON_HOLD` → Manual hold
- `REJECTED` → Rejected
- `CLOSED` → Final state

---

### SalesService

**Package:** `com.bigbrightpaints.erp.modules.sales.service`
**File:** `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/sales/service/SalesService.java`

**Purpose:** Facade aggregating all sales subservices. Primary entry point for controllers.

**Delegates to:**
| Subservice | Responsibility |
|------------|----------------|
| `SalesOrderCrudService` | Order CRUD |
| `SalesOrderLifecycleService` | Status transitions |
| `SalesDealerCrudService` | Dealer CRUD |
| `SalesDispatchReconciliationService` | Dispatch |
| `SalesDashboardService` | Dashboard |
| `SalesCoreEngine` | Core operations |

---

## CRUD Services

### SalesOrderCrudService

**Package:** `com.bigbrightpaints.erp.modules.sales.service`
**File:** `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/sales/service/SalesOrderCrudService.java`

**Purpose:** Thin wrapper for order CRUD with idempotency support.

**Methods:**
```java
List<SalesOrderDto> listOrders(String status, int page, int size)
List<SalesOrderDto> listOrders(String status, Long dealerId, int page, int size)
SalesOrderDto createOrder(SalesOrderRequest request)
SalesOrderDto updateOrder(Long id, SalesOrderRequest request)
void deleteOrder(Long id)
SalesOrder getOrderWithItems(Long id)
```

---

### SalesDealerCrudService

**Package:** `com.bigbrightpaints.erp.modules.sales.service`
**File:** `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/sales/service/SalesDealerCrudService.java`

**Purpose:** Thin wrapper for dealer CRUD operations.

**Methods:**
```java
List<DealerDto> listDealers()
DealerDto createDealer(DealerRequest request)
DealerDto updateDealer(Long id, DealerRequest request)
void deleteDealer(Long id)
```

---

## Lifecycle Services

### SalesOrderLifecycleService

**Package:** `com.bigbrightpaints.erp.modules.sales.service`
**File:** `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/sales/service/SalesOrderLifecycleService.java`

**Purpose:** Manages order status transitions with validation.

**Methods:**
```java
SalesOrderDto confirmOrder(Long id)
SalesOrderDto cancelOrder(Long id, String reason)
SalesOrderDto updateStatus(Long id, String status)
SalesOrderDto updateStatusInternal(Long id, String status)
void updateOrchestratorWorkflowStatus(Long id, String status)
boolean hasDispatchConfirmation(Long id)
void attachTraceId(Long id, String traceId)
List<SalesOrderStatusHistoryDto> orderTimeline(Long id)
```

---

## Domain Services

### DealerService

**Package:** `com.bigbrightpaints.erp.modules.sales.service`
**File:** `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/sales/service/DealerService.java`

**Purpose:** Complete dealer management including provisioning, portal user creation, and credit management.

**Responsibilities:**
- Dealer CRUD with auto-generated codes
- Portal user provisioning with email credentials
- Receivable account provisioning
- Credit utilization calculation
- Aging summary generation
- Ledger view

**Methods:**
```java
DealerResponse createDealer(CreateDealerRequest request)
List<DealerResponse> listDealers()
List<DealerLookupResponse> search(String query, String status, String region, String creditStatus)
DealerResponse updateDealer(Long dealerId, CreateDealerRequest request)
Map<String, Object> creditUtilization(Long dealerId)
Map<String, Object> agingSummary(Long dealerId)
Map<String, Object> ledgerView(Long dealerId)
```

---

### DealerPortalService

**Package:** `com.bigbrightpaints.erp.modules.sales.service`
**File:** `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/sales/service/DealerPortalService.java`

**Purpose:** Dealer-portal scoped data retrieval. Auto-filters data to authenticated dealer.

**Methods:**
```java
Dealer getCurrentDealer()
RequesterIdentity getCurrentRequesterIdentity()
boolean isDealerUser()
void verifyDealerAccess(Long dealerId)
Map<String, Object> getMyDashboard()
Map<String, Object> getMyLedger()
Map<String, Object> getMyInvoices()
Map<String, Object> getMyOutstandingAndAging()
Map<String, Object> getMyOrders()
InvoicePdfService.PdfDocument getMyInvoicePdf(Long invoiceId)
```

---

### CreditLimitRequestService

**Package:** `com.bigbrightpaints.erp.modules.sales.service`
**File:** `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/sales/service/CreditLimitRequestService.java`

**Purpose:** Permanent credit limit increase requests with approval workflow.

**Workflow:**
1. Dealer/Sales creates request with amount and reason
2. Admin/Accounting approves or rejects
3. On approval: Dealer's credit limit is increased

**Methods:**
```java
List<CreditLimitRequestDto> listRequests()
CreditLimitRequestDto createRequest(CreditLimitRequestCreateRequest request)
CreditLimitRequestDto createRequest(CreditLimitRequestCreateRequest request, Long requesterUserId, String requesterEmail)
CreditLimitRequestDto approveRequest(Long id, String decisionReason)
CreditLimitRequestDto rejectRequest(Long id, String decisionReason)
```

---

### CreditLimitOverrideService

**Package:** `com.bigbrightpaints.erp.modules.sales.service`
**File:** `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/sales/service/CreditLimitOverrideService.java`

**Purpose:** One-time credit limit overrides for dispatch exceeding limits.

**Key Features:**
- Maker-checker boundary enforcement
- Expiration support
- Headroom validation

**Methods:**
```java
List<CreditLimitOverrideRequestDto> listRequests(String status)
CreditLimitOverrideRequestDto createRequest(CreditLimitOverrideRequestCreateRequest request, String requestedBy)
CreditLimitOverrideRequestDto approveRequest(Long id, CreditLimitOverrideDecisionRequest request, String reviewedBy)
CreditLimitOverrideRequestDto rejectRequest(Long id, CreditLimitOverrideDecisionRequest request, String reviewedBy)
boolean isOverrideApproved(Long overrideRequestId, Company company, Dealer dealer, PackagingSlip slip, SalesOrder order, BigDecimal dispatchAmount)
```

---

### SalesFulfillmentService

**Package:** `com.bigbrightpaints.erp.modules.sales.service`
**File:** `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/sales/service/SalesFulfillmentService.java`

**Purpose:** Orchestrates complete fulfillment flow in atomic transaction.

**Fulfillment Steps:**
1. Reserve inventory (if not already reserved)
2. Dispatch inventory (FIFO/LIFO cost layers)
3. Post revenue journal entry
4. Post COGS journal entry
5. Update dealer balance
6. Issue invoice

**Methods:**
```java
FulfillmentResult fulfillOrder(Long orderId)
FulfillmentResult fulfillOrder(Long orderId, FulfillmentOptions options)
FulfillmentResult fulfillOrder(SalesOrder order, FulfillmentOptions options)
InventoryReservationResult reserveForOrder(Long orderId)
DispatchResult dispatchOrder(Long orderId)
```

---

### SalesReturnService

**Package:** `com.bigbrightpaints.erp.modules.sales.service`
**File:** `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/sales/service/SalesReturnService.java`

**Purpose:** Sales return processing with inventory restock and journal reversal.

**Return Flow:**
1. Preview return (calculate amounts, inventory value)
2. Validate return quantities vs invoiced
3. Post reversal journal entry (credit AR, debit revenue)
4. Restock inventory with return batch
5. Reverse COGS

**Methods:**
```java
SalesReturnPreviewDto previewReturn(SalesReturnRequest request)
JournalEntryDto processReturn(SalesReturnRequest request)
```

---

### DunningService

**Package:** `com.bigbrightpaints.erp.modules.sales.service`
**File:** `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/sales/service/DunningService.java`

**Purpose:** Automated dunning - places dealers on hold when overdue.

**Features:**
- Scheduled daily sweep at 3:15 AM
- Configurable overdue days threshold
- Configurable amount threshold
- Automated reminder emails

**Methods:**
```java
boolean evaluateDealerHold(Long dealerId, int overdueDaysThreshold, BigDecimal overdueAmountThreshold)
@Scheduled void dailyDunningSweep()
```

---

## Utility Services

### SalesDashboardService

**Package:** `com.bigbrightpaints.erp.modules.sales.service`
**File:** `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/sales/service/SalesDashboardService.java`

**Purpose:** Aggregates dashboard metrics.

**Metrics Returned:**
- Active dealer count
- Total orders
- Orders by status bucket (open, in_progress, dispatched, completed, cancelled)
- Pending credit requests

---

### SalesDispatchReconciliationService

**Package:** `com.bigbrightpaints.erp.modules.sales.service`
**File:** `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/sales/service/SalesDispatchReconciliationService.java`

**Purpose:** Dispatch confirmation and order-level marker reconciliation.

---

### OrderNumberService

**Package:** `com.bigbrightpaints.erp.modules.sales.service`
**File:** `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/sales/service/OrderNumberService.java`

**Purpose:** Generates sequential order numbers per company/fiscal year.

**Format:** `{COMPANY_CODE}-{FISCAL_YEAR}-{SEQUENCE}` (e.g., `BBP-2024-00001`)

**Features:**
- Retry logic for concurrent conflicts
- Audit logging
- Transactional isolation

---

### SalesIdempotencyService

**Package:** `com.bigbrightpaints.erp.modules.sales.service`
**File:** `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/sales/service/SalesIdempotencyService.java`

**Purpose:** Idempotency wrapper for order creation.

---

### SalesProformaBoundaryService

**Package:** `com.bigbrightpaints.erp.modules.sales.service`
**File:** `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/sales/service/SalesProformaBoundaryService.java`

**Purpose:** Proforma order processing with commercial assessment.

**Responsibilities:**
- Payment mode normalization
- Credit posture enforcement
- Commercial availability assessment
- Production requirement synchronization

---

### SalesOrderCreditExposurePolicy

**Package:** `com.bigbrightpaints.erp.modules.sales.service`
**File:** `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/sales/service/SalesOrderCreditExposurePolicy.java`

**Purpose:** Central policy for credit exposure calculation.

**Pending Credit Exposure Statuses:**
- BOOKED, RESERVED, PENDING_PRODUCTION, PENDING_INVENTORY
- PROCESSING, READY_TO_SHIP, CONFIRMED, ON_HOLD

---

### SalesDealerCrudService

**Package:** `com.bigbrightpaints.erp.modules.sales.service`
**File:** `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/sales/service/SalesDealerCrudService.java`

**Purpose:** Thin wrapper for dealer CRUD via SalesCoreEngine.

---

### DispatchMetadataValidator

**Package:** `com.bigbrightpaints.erp.modules.sales.service`
**File:** `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/sales/service/DispatchMetadataValidator.java`

**Purpose:** Validates dispatch confirmation request metadata.

---

### SalesAccountConfigurationValidator

**Package:** `com.bigbrightpaints.erp.modules.sales.config`
**File:** `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/sales/config/SalesAccountConfigurationValidator.java`

**Purpose:** Validates company accounting configuration for sales operations.
