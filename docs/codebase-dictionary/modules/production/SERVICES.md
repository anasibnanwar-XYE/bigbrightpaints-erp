# Production Services

## CatalogService
**Location:** `service/CatalogService.java`

### Purpose
Primary service for catalog CRUD operations with inventory sync.

### Key Dependencies
- `CompanyContextService` - Company context
- `CompanyEntityLookup` - Entity lookup
- `ProductionBrandRepository` - Brand data
- `ProductionProductRepository` - Product data
- `SizeVariantRepository` - Size variants
- `FinishedGoodRepository` - Finished goods sync
- `RawMaterialRepository` - Raw materials sync
- `SkuReadinessService` - Readiness checks
- `ProductionCatalogService` - Bulk operations

### Key Methods

| Method | Description |
|--------|-------------|
| `createBrand(CatalogBrandRequest)` | Create brand |
| `listBrands(Boolean)` | List brands |
| `getBrand(Long)` | Get brand |
| `updateBrand(Long, CatalogBrandRequest)` | Update brand |
| `deactivateBrand(Long)` | Deactivate brand |
| `createItem(CatalogItemRequest)` | Create item |
| `getItem(Long, boolean, boolean, boolean)` | Get item with options |
| `updateItem(Long, CatalogItemRequest)` | Update item |
| `deactivateItem(Long)` | Deactivate item |
| `searchItems(...)` | Search with pagination |
| `createProduct(CatalogProductRequest)` | Create product |
| `getProduct(Long)` | Get product |
| `updateProduct(Long, CatalogProductRequest)` | Update product |
| `deactivateProduct(Long)` | Deactivate product |
| `searchProducts(...)` | Search products |

### Inventory Sync Logic
1. On product create/update:
   - If RAW_MATERIAL category → sync to `RawMaterial`
   - Otherwise → sync to `FinishedGood`
2. Delete mirror entity if category changes
3. Sync size variants for carton quantities

### Accounting Metadata
- Validates required accounts for finished goods
- Uses company defaults if not specified
- Required: fgValuationAccountId, fgCogsAccountId, fgRevenueAccountId, fgTaxAccountId

---

## ProductionCatalogService
**Location:** `service/ProductionCatalogService.java`

### Purpose
Handle bulk operations, imports, and canonical product entry.

### Key Dependencies
- `CompanyContextService` - Company context
- `ProductionBrandRepository` - Brand data
- `ProductionProductRepository` - Product data
- `FinishedGoodRepository` - Finished goods
- `RawMaterialRepository` - Raw materials
- Various inventory and factory repositories
- `CompanyEntityLookup` - Entity lookup
- `CompanyDefaultAccountsService` - Account defaults
- `CatalogImportRepository` - Import tracking
- `AuditService` - Audit logging
- `SkuReadinessService` - Readiness
- `IdempotencyReservationService` - Idempotency

### Key Methods

| Method | Description |
|--------|-------------|
| `importCatalog(MultipartFile, String)` | Import CSV catalog |
| `createProduct(ProductCreateRequest)` | Create single product |
| `createOrPreviewCatalogProducts(CatalogProductEntryRequest, boolean)` | Canonical entry |
| `createVariants(BulkVariantRequest, boolean)` | Bulk variants |

### Canonical Product Entry
1. Validate request structure
2. Resolve brand and generate variant group ID
3. Generate all color × size SKU combinations
4. Check for existing SKUs (conflicts)
5. If preview, return generated items and conflicts
6. If not preview, create missing products

### CSV Import Format
Standard CSV with headers for:
- Brand (name, code)
- Product details (name, category)
- Variants (colors, sizes)
- Pricing (base price, GST rate)
- Accounting metadata

### Idempotency
- Uses file hash + idempotency key
- Prevents duplicate imports
- Returns existing result if matched

---

## SkuReadinessService
**Location:** `service/SkuReadinessService.java`

### Purpose
Evaluate SKU readiness across multiple stages.

### Key Dependencies
- `ProductionProductRepository` - Product data
- `FinishedGoodRepository` - Finished goods
- `FinishedGoodBatchRepository` - Batch stock
- `RawMaterialRepository` - Raw materials
- `PackagingSizeMappingRepository` - Packaging mappings

### Expected Stock Types
```java
enum ExpectedStockType {
    FINISHED_GOOD,
    RAW_MATERIAL,
    PACKAGING_RAW_MATERIAL
}
```

### Readiness Stages

| Stage | Description |
|-------|-------------|
| Catalog | Product exists and is active |
| Inventory | Mirror entity exists with accounts |
| Production | WIP accounts configured (for finished goods) |
| Packing | Packaging size mapping exists |
| Sales | Has sale-ready batch stock |
| Accounting | All accounting prerequisites met |

### Key Methods

| Method | Description |
|--------|-------------|
| `forProduct(Company, ProductionProduct)` | Check single product |
| `forProducts(Company, Collection)` | Batch check |
| `forSku(Company, String, ExpectedStockType)` | Check by SKU |
| `forPlannedProduct(...)` | Preview readiness |
| `sanitizeForCatalogViewer(...)` | Hide accounting details |

### Blocking Conditions

#### Finished Goods
- `PRODUCT_MASTER_MISSING` - Product not found
- `PRODUCT_INACTIVE` - Product deactivated
- `FINISHED_GOOD_MIRROR_MISSING` - No inventory entity
- `FINISHED_GOOD_VALUATION_ACCOUNT_MISSING`
- `FINISHED_GOOD_COGS_ACCOUNT_MISSING`
- `FINISHED_GOOD_REVENUE_ACCOUNT_MISSING`
- `FINISHED_GOOD_TAX_ACCOUNT_MISSING`
- `WIP_ACCOUNT_MISSING`
- `LABOR_APPLIED_ACCOUNT_MISSING`
- `OVERHEAD_APPLIED_ACCOUNT_MISSING`
- `PACKAGING_SIZE_MISSING`
- `PACKAGING_MAPPING_MISSING`
- `NO_FINISHED_GOOD_BATCH_STOCK`
- `DISCOUNT_ACCOUNT_MISSING`
- `GST_OUTPUT_ACCOUNT_MISSING`

#### Raw Materials
- `RAW_MATERIAL_MIRROR_MISSING`
- `RAW_MATERIAL_INVENTORY_ACCOUNT_MISSING`
- `RAW_MATERIAL_SKU_NOT_SALES_ORDERABLE`
