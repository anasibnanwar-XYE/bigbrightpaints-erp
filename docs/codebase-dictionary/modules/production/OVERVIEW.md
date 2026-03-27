# Production Module Overview

## Purpose
The Production module manages product catalog, SKU management, and inventory synchronization for BigBright ERP. It handles finished goods, raw materials, and packaging materials with automatic sync to inventory tracking.

## Module Location
`erp-domain/src/main/java/com/bigbrightpaints/erp/modules/production/`

## File Count
- **Total Java files:** 33
- **Controllers:** 2
- **Services:** 3
- **Entities:** 6 (including repositories)
- **DTOs:** 30

## Key Capabilities

### Catalog Management
- Brand management (create, update, deactivate)
- Product catalog with variants (colors, sizes)
- SKU generation with deterministic patterns
- CSV bulk import

### SKU Management
- Three item classes: FINISHED_GOOD, RAW_MATERIAL, PACKAGING_RAW_MATERIAL
- Automatic SKU generation from brand, name, color, size
- Variant group management

### Inventory Synchronization
- Automatic sync to `FinishedGood` or `RawMaterial` entities
- Size variant tracking with carton quantities
- Packaging size mapping integration

### SKU Readiness
- Multi-stage readiness checks (catalog, inventory, production, packing, sales, accounting)
- Blocking condition detection
- Required account validation

## Dependencies
- `modules/company` - Company context and multi-tenancy
- `modules/inventory` - FinishedGood and RawMaterial entities
- `modules/factory` - SizeVariant, PackagingSizeMapping
- `modules/accounting` - Account validation

## API Endpoints

### Catalog Controller (`/api/v1/catalog`)
- `GET /brands` - List brands
- `POST /brands` - Create brand
- `GET /brands/{brandId}` - Get brand
- `PUT /brands/{brandId}` - Update brand
- `DELETE /brands/{brandId}` - Deactivate brand
- `POST /import` - Import catalog from CSV
- `GET /items` - Search catalog items
- `POST /items` - Create item
- `GET /items/{itemId}` - Get item
- `PUT /items/{itemId}` - Update item
- `DELETE /items/{itemId}` - Deactivate item

### Production Catalog Controller
- Internal canonical product entry API
- Bulk variant generation

## Security
- Read access: `ROLE_ADMIN`, `ROLE_ACCOUNTING`, `ROLE_SALES`, `ROLE_FACTORY`
- Write access: `ROLE_ADMIN`, `ROLE_ACCOUNTING`
- Accounting metadata visible only to ADMIN/ACCOUNTING roles

## Item Classes

| Item Class | Description | Inventory Sync |
|------------|-------------|----------------|
| `FINISHED_GOOD` | Sellable products | → FinishedGood |
| `RAW_MATERIAL` | Production materials | → RawMaterial (PRODUCTION type) |
| `PACKAGING_RAW_MATERIAL` | Packaging materials | → RawMaterial (PACKAGING type) |

## SKU Pattern

### Finished Goods
```
FG-{BRAND_CODE}-{PRODUCT_NAME}-{COLOR}-{SIZE}
Example: FG-BBP-PREMIUM-EMULSION-RED-20L
```

### Raw Materials
```
RM-{BRAND_CODE}-{NAME}-{SPEC}-{UNIT}
Example: RM-BBP-TITANIUM-DIOXIDE-TECHNICAL-KG
```

### Packaging Raw Materials
```
PKG-{BRAND_CODE}-{TYPE}-{SIZE}-{UNIT}
Example: PKG-BBP-DRUM-20L-UNIT
```
