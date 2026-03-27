# Sales Module - Entities

// Domain Layer
// Package: `com.bigbrightpaints.erp.modules.sales.domain`

// Directory: `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/sales/domain/`

// Overview

// The Sales module contains 10 domain entities representing the persistent data model for sales operations.

// Each entity extends `VersionedEntity` for optimistic locking and audit trail support.

// ---
// ## Entities
// | Entity | Table | Purpose |
// |--------|-------|---------|
// | `Dealer` | `dealers` | Customer/dealer master data |
// | `SalesOrder` | `sales_orders` | Sales order header |
// | `SalesOrderItem` | `sales_order_items` | Sales order line items |
// | `SalesOrderStatusHistory` | `sales_order_status_history` | Order status transitions |
// | `CreditRequest` | `credit_requests` | Credit limit increase requests |
// | `CreditLimitOverrideRequest` | `credit_limit_override_requests` | One-time dispatch overrides |
// | `Promotion` | `promotions` | Promotional campaigns |
// | `SalesTarget` | `sales_targets` | Sales performance targets |
// | `OrderSequence` | `order_sequences` | Order number sequences |
// | `DealerPaymentTerms` | (enum) | Payment terms enumeration |
// ---
// ## Dealer
// **Table:** `dealers`
// **Primary Key:** `id` (Long)
// **Unique Constraints:**
// - `company_id, code` (per company)
// **Relationships:**
// | Relationship | Type | Cardinality | Purpose |
// |--------------|------|------------|---------|
// | `Company` | Many-to-one | Required | Multi-tenancy |
// | `UserAccount` | Many-to-one | Optional | Portal user |
// | `Account` | Many-to-one | Optional | Receivable account |
// **Fields:**
// | Field | Type | Nullable | Description |
// |-----------------|------|----------|-------------|
// | `publicId` | UUID | No | Public identifier |
// | `name` | String | No | Dealer name |
// | `code` | String | No | Dealer code |
// | `companyName` | String | Yes | Company name |
// | `email` | String | Yes | Contact email |
// | `phone` | String | Yes | Phone number |
// | `address` | String | Yes | Address |
// | `gstNumber` | String | Yes | GST identification number |
// | `stateCode` | String | Yes | State code for GST |
// | `region` | String | Yes | Geographic region |
// | `gstRegistrationType` | Enum | No | GST registration type |
// | `paymentTerms` | Enum | No | Payment terms |
// | `status` | String | No | Status (ACTIVE, INACTIVE, ON_HOLD) |
// | `creditLimit` | BigDecimal | No | Credit limit |
// | `outstandingBalance` | BigDecimal | No | Cached outstanding balance |
// **Business Rules:**
// - GST number must valid 15-character GSTIN format
// - State code must exactly 2 characters
// - Default status is `ACTIVE`
// - Default credit limit is zero
// ---
// ## SalesOrder
// **Table:** `sales_orders`
// **Primary Key:** `id` (Long)
// **Unique Constraints:**
// - `company_id, order_number` (per company)
// **Relationships:**
// | Relationship | Type | Cardinality | Purpose |
// |--------------|------|------------|---------|
// | `Company` | Many-to-one | Required | Multi-tenancy |
// | `Dealer` | Many-to-one | Optional | Customer |
// | `SalesOrderItem` | One-to-many | Cascade | Order line items |
// **Fields:**
// | Field | Type | Nullable | Description |
// |-----------------|------|----------|-------------|
// | `publicId` | UUID | No | Public identifier |
// | `orderNumber` | String | No | Sequential order number |
// | `status` | String | No | Current status |
// | `traceId` | String | Yes | Distributed trace ID |
// | `totalAmount` | BigDecimal | No | Order total |
// | `subtotalAmount` | BigDecimal | No | Subtotal before tax |
// | `gstTotal` | BigDecimal | No | Total GST amount |
// | `gstTreatment` | String | No | GST treatment type |
// | `gstInclusive` | boolean | No | Whether GST is inclusive |
// | `gstRate` | BigDecimal | Yes | GST rate percentage |
// | `gstRoundingAdjustment` | BigDecimal | No | GST rounding adjustment |
// | `currency` | String | No | Currency (default: INR) |
// | `paymentMode` | String | No | Payment mode (CREDIT, CASH, HYBRID) |
// | `idempotencyKey` | String | Yes | Idempotency key |
// | `idempotencyHash` | String | Yes | Request signature hash |
// | `notes` | String | Yes | Order notes |
// **Idempotency Markers:**
// | Marker | Type | Purpose |
// |-----------------|------|---------|
// | `salesJournalEntryId` | Long | Posted sales journal entry ID |
// | `cogsJournalEntryId` | Long | Posted COGS journal entry ID |
// | `fulfillmentInvoiceId` | Long | Generated invoice ID |
// **Order Status Values:**
// - `DRAFT` - Initial state
// - `CONFIRMED` - Ready for fulfillment
// - `RESERVED` - Inventory reserved
// - `PENDING_PRODUCTION` - A manufacturing
// - `PENDING_INVENTORY` - awaiting stock
// - `PROCESSING` - in warehouse
// - `READY_TO_SHIP` - ready for dispatch
// - `DISPATCHED` - shipped to customer
// - `INVOICED` - invoice generated
// - `SETTLED` - payment received
// - `CANCELLED` - cancelled
// - `ON_HOLD` - manual hold
// - `REJECTED` - rejected
// - `CLOSED` - final state
// ---
            ## SalesOrderItem
// **Table:** `sales_order_items`
// **Primary Key:** `id` (Long)
// **Relationships:**
// | Relationship | Type | Cardinality | Purpose |
// |--------------|------|------------|---------|
// | `SalesOrder` | Many-to-one | Required | Parent order |
// **Fields:**
// | Field | Type | Nullable | Description |
// |-----------------|------|----------|-------------|
// | `productCode` | String | No | Product/SKU code |
// | `description` | String | Yes | Line description |
// | `quantity` | BigDecimal | No | Quantity ordered |
// | `unitPrice` | BigDecimal | No | Unit price |
// | `lineSubtotal` | BigDecimal | No | Line subtotal |
// | `lineTotal` | BigDecimal | No | Line total with tax |
// | `gstRate` | BigDecimal | No | GST rate for line |
// | `gstAmount` | BigDecimal | No | GST amount for line |
// ---
            ## SalesOrderStatusHistory
// **Table:** `sales_order_status_history`
// **Primary Key:** `id` (Long)
// **Relationships:**
// | Relationship | Type | Cardinality | Purpose |
// |--------------|------|------------|---------|
// | `Company` | Many-to-one | Required | Multi-tenancy |
// | `SalesOrder` | Many-to-one | Required | Parent order |
// **Fields:**
// | Field | Type | Nullable | Description |
// |-----------------|------|----------|-------------|
// | `fromStatus` | String | Yes | Previous status |
// | `toStatus` | String | No | New status |
// | `reasonCode` | String | Yes | Status change reason code |
// | `reason` | String | Yes | Human-readable reason |
// | `changedBy` | String | No | Actor who made change |
// | `changedAt` | Instant | No | Timestamp of change |
            // ---
            ## CreditRequest
// **Table:** `credit_requests`
// **Primary Key:** `id` (Long)
// **Relationships:**
// | Relationship | Type | Cardinality | Purpose |
// |--------------|------|------------|---------|
// | `Company` | Many-to-one | Required | Multi-tenancy |
// | `Dealer` | Many-to-one | Optional | Dealer |
            // **Fields:**
// | Field | Type | Nullable | Description |
// |-----------------|------|----------|-------------|
// | `publicId` | UUID | No | Public identifier |
// | `amountRequested` | BigDecimal | No | Credit limit increase amount |
// | `status` | String | No | Status (PENDING, APPROVED, REJECTED) |
// | `reason` | String | Yes | Reason for request |
// | `requesterUserId` | Long | Yes | User ID of requester |
// | `requesterEmail` | String | Yes | Email of requester |
            // ---
            ## CreditLimitOverrideRequest
// **Table:** `credit_limit_override_requests`
// **Primary Key:** `id` (Long)
// **Relationships:**
// | Relationship | Type | Cardinality | Purpose |
// |--------------|------|------------|---------|
// | `Company` | Many-to-one | Required | Multi-tenancy |
// | `Dealer` | Many-to-one | Optional | Dealer |
// | `PackagingSlip` | Many-to-one | Optional | Packaging slip |
// | `SalesOrder` | Many-to-one | Optional | Sales order |
            // **Fields:**
// | Field | Type | Nullable | Description |
// |-----------------|------|----------|-------------|
// | `publicId` | UUID | No | Public identifier |
// | `dispatchAmount` | BigDecimal | No | Amount being dispatched |
// | `currentExposure` | BigDecimal | No | Current credit exposure |
// | `creditLimit` | BigDecimal | No | Dealer credit limit |
// | `requiredHeadroom` | BigDecimal | No | Additional headroom needed |
// | `status` | String | No | Status (PENDING, APPROVED, REJECTED, EXPIRED) |
// | `reason` | String | Yes | Reason for override |
// | `requestedBy` | String | Yes | User who requested |
// | `reviewedBy` | String | Yes | User who approved/rejected |
// | `reviewedAt` | Instant | Yes | Timestamp of review |
// | `expiresAt` | Instant | Yes | Expiration time |
            // ---
            ## Promotion
// **Table:** `promotions`
// **Primary Key:** `id` (Long)
// **Relationships:**
// | Relationship | Type | Cardinality | Purpose |
// |--------------|------|------------|---------|
// | `Company` | Many-to-one | Required | Multi-tenancy |
            // **Fields:**
// | Field | Type | Nullable | Description |
// |-----------------|------|----------|-------------|
// | `publicId` | UUID | No | Public identifier |
// | `name` | String | No | Promotion name |
// | `description` | String | Yes | Description |
// | `imageUrl` | String | Yes | Image URL |
// | `discountType` | String | No | Type (PERCENTAGE, FIXED) |
// | `discountValue` | BigDecimal | No | Discount value |
// | `startDate` | LocalDate | No | Start date |
// | `endDate` | LocalDate | No | End date |
// | `status` | String | No | Status (DRAFT, ACTIVE, EXPIRED) |
            // ---
            ## SalesTarget
// **Table:** `sales_targets`
// **Primary Key:** `id` (Long)
// **Relationships:**
// | Relationship | Type | Cardinality | Purpose |
// |--------------|------|------------|---------|
// | `Company` | Many-to-one | Required | Multi-tenancy |
            // **Fields:**
// | Field | Type | Nullable | Description |
// |-----------------|------|----------|-------------|
// | `publicId` | UUID | No | Public identifier |
// | `name` | String | No | Target name |
// | `periodStart` | LocalDate | No | Period start |
// | `periodEnd` | LocalDate | No | Period end |
// | `targetAmount` | BigDecimal | No | Target revenue amount |
// | `achievedAmount` | BigDecimal | No | Achieved revenue amount |
// | `assignee` | String | Yes | Assigned salesperson |
            // ---
            ## OrderSequence
// **Table:** `order_sequences`
// **Primary Key:** `id` (Long)
// **Unique Constraints:**
// - `company_id, fiscal_year` (per company per year)
// **Relationships:**
// | Relationship | Type | Cardinality | Purpose |
// |--------------|------|------------|---------|
// | `Company` | Many-to-one | Required | Multi-tenancy |
            // **Fields:**
// | Field | Type | Nullable | Description |
// |-----------------|------|----------|-------------|
// | `fiscalYear` | Integer | No | Fiscal year |
// | `nextNumber` | Long | No | Next sequence number |
// **Methods:**
// | Method | Return | Description |
// |-----------------|--------|-------------|
// | `consumeAndIncrement()` | Long | Get current and increment atomically |
            // ---
            ## DealerPaymentTerms (Enum)
// **Values:**
// | Value | Due Days |
// |-------|----------|
// | `NET_30` | 30 |
// | `NET_60` | 60 |
// | `NET_90` | 90 |
            // ---
            ## Repositories
            The repository interfaces provide data access for the entities:
            | Repository | Entity | Purpose |
            |--------------|--------|---------|
            | `DealerRepository` | Dealer | Dealer CRUD |
            | `SalesOrderRepository` | SalesOrder | Order CRUD |
            | `SalesOrderItemRepository` | SalesOrderItem | Order line CRUD |
            | `SalesOrderStatusHistoryRepository` | SalesOrderStatusHistory | Status history |
            | `CreditRequestRepository` | CreditRequest | Credit request CRUD |
            | `CreditLimitOverrideRequestRepository` | CreditLimitOverrideRequest | Override CRUD |
            | `PromotionRepository` | Promotion | Promotion CRUD |
            | `SalesTargetRepository` | SalesTarget | Target CRUD |
            | `OrderSequenceRepository` | OrderSequence | Sequence CRUD |
