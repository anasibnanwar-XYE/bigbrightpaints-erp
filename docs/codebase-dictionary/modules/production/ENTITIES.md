# Production Entities

## ProductionBrand
**Location:** `domain/ProductionBrand.java`
**Table:** `production_brands`

### Purpose
Product brand for catalog organization.

### Key Fields
| Field | Type | Description |
|-------|------|-------------|
| `id` | Long | Primary key |
| `publicId` | UUID | Public identifier |
| `company` | Company | Tenant |
| `name` | String | Brand name (unique per company) |
| `code` | String | Brand code (unique per company) |
| `logoUrl` | String | Logo URL |
| `description` | String | Description |
| `active` | boolean | Is active |
| `createdAt` | Instant | Created |
| `updatedAt` | Instant | Last updated |

### Unique Constraints
- (company_id, code)
- (company_id, name)

---

## ProductionProduct
**Location:** `domain/ProductionProduct.java`
**Table:** `production_products`

### Purpose
Product catalog entry with variants.

### Key Fields
| Field | Type | Description |
|-------|------|-------------|
| `id` | Long | Primary key |
| `publicId` | UUID | Public identifier |
| `company` | Company | Tenant |
| `brand` | ProductionBrand | Parent brand |
| `productName` | String | Product name |
| `category` | String | FINISHED_GOOD/RAW_MATERIAL |
| `skuCode` | String | Unique SKU |
| `variantGroupId` | UUID | Group ID for variants |
| `productFamilyName` | String | Family name |
| `defaultColour` | String | Default color |
| `sizeLabel` | String | Default size |
| `colors` | Set\<String\> | Available colors |
| `sizes` | Set\<String\> | Available sizes |
| `cartonSizes` | Map\<String, Integer\> | Size → pieces per carton |
| `unitOfMeasure` | String | Unit (e.g., UNIT, KG, L) |
| `hsnCode` | String | HSN code |
| `basePrice` | BigDecimal | Base price |
| `gstRate` | BigDecimal | GST percentage |
| `minDiscountPercent` | BigDecimal | Minimum discount |
| `minSellingPrice` | BigDecimal | Minimum selling price |
| `metadata` | Map\<String, Object\> | JSONB metadata |
| `active` | boolean | Is active |
| `createdAt` | Instant | Created |
| `updatedAt` | Instant | Last updated |

### Unique Constraints
- (company_id, sku_code)
- (brand_id, product_name)

### Metadata Keys (JSONB)
- `fgValuationAccountId` - Finished goods valuation account
- `fgCogsAccountId` - COGS account
- `fgRevenueAccountId` - Revenue account
- `fgDiscountAccountId` - Discount account
- `fgTaxAccountId` - Tax account
- `wipAccountId` - WIP account
- `laborAppliedAccountId` - Labor account
- `overheadAppliedAccountId` - Overhead account
- `inventoryAccountId` - For raw materials
- `rawMaterialInventoryAccountId` - Alternative key

---

## CatalogImport
**Location:** `domain/CatalogImport.java`
**Table:** `catalog_imports`

### Purpose
Track CSV import history with idempotency.

### Key Fields
| Field | Type | Description |
|-------|------|-------------|
| `id` | Long | Primary key |
| `company` | Company | Tenant |
| `idempotencyKey` | String | Idempotency key |
| `idempotencyHash` | String | Key hash |
| `fileHash` | String | File content hash |
| `fileName` | String | Original filename |
| `rowsProcessed` | int | Rows processed |
| `brandsCreated` | int | Brands created |
| `productsCreated` | int | Products created |
| `productsUpdated` | int | Products updated |
| `rawMaterialsSeeded` | int | Raw materials synced |
| `errorsJson` | String | Errors (JSON) |
| `createdAt` | Instant | Import time |

### Unique Constraint
- (company_id, idempotency_key)

---

## Repositories

### ProductionBrandRepository
- `findByCompanyOrderByNameAsc(Company)`
- `findByCompanyAndActiveOrderByNameAsc(Company, boolean)`
- `findByCompanyAndId(Company, Long)`
- `findByCompanyAndNameIgnoreCase(Company, String)`
- `findByCompanyAndCodeIgnoreCase(Company, String)`

### ProductionProductRepository
- `findByCompanyAndId(Company, Long)`
- `findByCompanyAndSkuCode(Company, String)`
- `findByCompanyAndSkuCodeIn(Company, Collection)`
- `findByBrandAndProductNameIgnoreCase(ProductionBrand, String)`
- `findTopByCompanyAndSkuCodeStartingWithOrderBySkuCodeDesc(Company, String)`
- `findAll(Specification, Pageable)`

### CatalogImportRepository
- `findByCompanyAndIdempotencyKey(Company, String)`
