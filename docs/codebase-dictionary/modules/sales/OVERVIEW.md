# Sales Module Documentation

## Overview

The Sales module manages the complete lifecycle of sales orders from creation to fulfillment, dispatch confirmation, invoicing, and payment, and dealer management and credit management, dunning ( promotions, targets, and returns.

 and portal access for dealers.

 This module integrates with:
- **Inventory Module**: Stock reservations, dispatch
- **Invoice Module**: Invoice generation
- **Accounting Module**: Journal entries,- **Company Module**: Multi-tenancy
- **Auth Module**: Authentication (users, dealers)
- **RBAC Module**: Role-based access control

## Key Statistics
| Metric | Value |
|--------|-------|
| Main Source Files | 74 |
| Controllers | 5 |
| Services | 18 |
| Repositories | 11 |
| Entities | 10 |
| DTOs | 21 |
| Config/Utils/Events | 9 |

## Core Components

### SalesCoreEngine
**Type:** Service  
**Module:** sales  
**Package:** `com.bigbrightpaints.erp.modules.sales.service`  
**File:** `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/sales/service/SalesCoreEngine.java`  
**Responsibility:** Core orchestestrator for sales orders, dealers, inventory, invoices, and accounting operations. Contains the complete Order-to-Cash (O2C) flow logic including order creation, confirmation, inventory reservation/dispatch, dispatch confirmation, invoice generation, AR journal posting, COGS posting, dealer balance updates, payment collection, and more. Acts as the command-and control center for all sales-related operations.

  
**Public Methods:**
```java
public List<SalesOrderDto> listOrders(String status, int page, int size)
public List<SalesOrderDto> listOrders(String status, Long dealerId, int page, int size)
public List<SalesOrderDto> listOrders(String status)
public List<SalesOrderDto> listOrders(String status, Long dealerId)
public List<DealerDto> listDealers()
public DealerDto createDealer(DealerRequest request)
public DealerDto updateDealer(Long id, DealerRequest request)
public void deleteDealer(Long id)
public SalesOrderDto createOrder(SalesOrderRequest request)
public SalesOrderDto updateOrder(Long id, SalesOrderRequest request)
public void deleteOrder(Long id)
public SalesOrderDto confirmOrder(Long id)
public SalesOrderDto cancelOrder(Long id, String reason)
public SalesOrderDto updateStatus(Long id, String status)
public PageResponse<SalesOrderDto> searchOrders(SalesOrderSearchFilters filters)
public SalesDashboardDto getDashboard()
public List<PromotionDto> listPromotions()
public PromotionDto createPromotion(PromotionRequest request)
public PromotionDto updatePromotion(Long id, PromotionRequest request)
public void deletePromotion(Long id)
public List<SalesTargetDto> listTargets()
public SalesTargetDto createTarget(SalesTargetRequest request)
public SalesTargetDto updateTarget(Long id, SalesTargetRequest request)
public void deleteTarget(Long id, String reason)
public DispatchConfirmResponse confirmDispatch(DispatchConfirmRequest request)
public DispatchMarkerReconciliationResponse reconcileStaleOrderLevelMarkers(int limit)
```
**Callers:****
- `SalesController`, `SalesOrderCrudService`, `SalesOrderLifecycleService`, `SalesDashboardService`, `SalesFulfillmentService`
- `DealerController` (via `DealerService`)
- `DealerPortalController` (via `DealerPortalService`, `CreditLimitRequestController` (via `CreditLimitRequestService`)
- `CreditLimitOverrideController` (via `CreditLimitOverrideService`)
- `SalesReturnService` (for sales returns)
- `DunningService` (for dunning automation)
- `SalesDispatchReconciliationService` (for dispatch confirmation)
- `SalesService` (aggregates above services)
- `SalesCoreEngine` (primary orchestestrator)
- `SalesOrderCreditExposurePolicy` (credit exposure calculation)
- `SalesIdempotencyService` (idempotency wrapper)
- `SalesProformaBoundaryService` (proforma invoice generation)
- `DispatchMetadataValidator` (dispatch validation)

- `SalesDealerCrudService` (dealer CRUD operations)
- `OrderNumberService` (order number generation)

  
**Dependencies:**
- `AccountingService` / `AccountingFacade` (journal posting)
- `DealerLedgerService` (dealer balances)
- `FinishedGoodsService` (inventory operations)
- `InvoiceService` (invoice generation)
- `CompanyContextService` (company context)
- `DealerRepository`, `SalesOrderRepository`, `PromotionRepository`, `CreditRequestRepository`, etc.
  
**Side effects:**
- **Database writes:** Orders, dealers, promotions, targets, credit requests
- **Events published:** `SalesOrderCreatedEvent`
- **Journals created:** Sales journal, COGS journal
- **Invoices created:** Final invoice
- **Inventory updates:** Reservations, dispatches,- **Email sent:** Overdue reminders (dunning)
- **Dealer balance updates:** On credit limit increase approval
  
**Invariants:**
- Credit limit cannot be exceeded for CREDIT orders
- Order status transitions follow workflow rules
- Dealers must valid receivable account for orders
- Invoice required for invoice generation

- Inventory must to available for dispatch
- GST/Tax calculated according to configured rates

- Idempotency key ensures unique order creation
  
**Status:** Canonical
**Use when:** Creating/updating sales orders, dealers, promotions, targets
**Do not use when:** Retrieving historical data only, Credit limit management
**Callers:** SalesController, DealerController, CreditLimitRequestController, CreditLimitOverrideController
**Dependencies:**
- SalesOrderCrudService, SalesOrderLifecycleService, SalesDashboardService, SalesFulfillmentService, DealerService, DunningService, SalesReturnService
- `DispatchMetadataValidator` (static utility)
- `FinishedGoodsService` (inventory)
- `AccountingFacade` (journal entries)
- `CompanyContextService` (company)

- `DealerRepository` (dealer data)
- `SalesOrderRepository` (order data)
- `CreditLimitOverrideRequestRepository` (credit override requests)
