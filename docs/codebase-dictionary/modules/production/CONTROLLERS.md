# Production Controllers

## CatalogController
**Location:** `controller/CatalogController.java`
**Base Path:** `/api/v1/catalog`
**Read Roles:** `ROLE_ADMIN`, `ROLE_ACCOUNTING`, `ROLE_SALES`, `ROLE_FACTORY`
**Write Roles:** `ROLE_ADMIN`, `ROLE_ACCOUNTING`

### Brand Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/brands` | List brands (optional `active` filter) |
| POST | `/brands` | Create brand |
| GET | `/brands/{brandId}` | Get brand by ID |
| PUT | `/brands/{brandId}` | Update brand |
| DELETE | `/brands/{brandId}` | Deactivate brand |

### Catalog Import Endpoint

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/import` | Import catalog from CSV file |

Supports idempotency via `Idempotency-Key` header.

### Item Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/items` | Search items with pagination |
| POST | `/items` | Create item |
| GET | `/items/{itemId}` | Get item details |
| PUT | `/items/{itemId}` | Update item |
| DELETE | `/items/{itemId}` | Deactivate item |

### Search Parameters
- `q` - Search query (product name, SKU, brand name)
- `itemClass` - Filter by class (FINISHED_GOOD, RAW_MATERIAL, PACKAGING_RAW_MATERIAL)
- `includeStock` - Include stock quantities
- `includeReadiness` - Include SKU readiness status
- `page`, `pageSize` - Pagination

### Dependencies
- `CatalogService` - Main catalog operations
- `ProductionCatalogService` - Import and bulk operations

### Accounting Metadata Visibility
- Only visible to `ROLE_ADMIN` and `ROLE_ACCOUNTING`
- Hidden from `ROLE_SALES` and `ROLE_FACTORY`

---

## ProductionCatalogController
**Location:** `controller/ProductionCatalogController.java`

Internal controller for canonical product entry and bulk variant operations.

### Key Operations
- Canonical product entry (single request creates multiple variants)
- Bulk variant creation with SKU generation
- CSV catalog import processing

### Bulk Operations
- Generates all color × size combinations
- Creates missing products automatically
- Detects conflicts before creation
- Supports dry-run preview
