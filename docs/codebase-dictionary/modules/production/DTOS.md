# Production DTOs

## Brand DTOs

### CatalogBrandDto
```java
record CatalogBrandDto(
    Long id,
    UUID publicId,
    String name,
    String code,
    String logoUrl,
    String description,
    boolean active
);
```

### CatalogBrandRequest
```java
record CatalogBrandRequest(
    String name,
    String logoUrl,
    String description,
    Boolean active
);
```

---

## Product DTOs

### CatalogItemDto
```java
record CatalogItemDto(
    Long id,
    UUID publicId,
    Long rawMaterialId,
    Long brandId,
    String brandName,
    String brandCode,
    String name,
    String sku,
    String itemClass,         // FINISHED_GOOD/RAW_MATERIAL/PACKAGING_RAW_MATERIAL
    String color,
    String size,
    String unitOfMeasure,
    String hsnCode,
    BigDecimal basePrice,
    BigDecimal gstRate,
    BigDecimal minDiscountPercent,
    BigDecimal minSellingPrice,
    Map<String, Object> metadata,
    boolean active,
    CatalogItemStockDto stock,
    SkuReadinessDto readiness
);
```

### CatalogItemRequest
```java
record CatalogItemRequest(
    Long brandId,
    String name,
    String itemClass,
    String color,
    String size,
    String unitOfMeasure,
    String hsnCode,
    BigDecimal basePrice,
    BigDecimal gstRate,
    BigDecimal minDiscountPercent,
    BigDecimal minSellingPrice,
    Map<String, Object> metadata,
    Boolean active
);
```

### CatalogItemStockDto
```java
record CatalogItemStockDto(
    BigDecimal onHand,
    BigDecimal reserved,
    BigDecimal available,
    String unit
);
```

---

### CatalogProductDto
```java
record CatalogProductDto(
    Long id,
    UUID publicId,
    Long rawMaterialId,
    Long brandId,
    String brandName,
    String brandCode,
    String productName,
    String skuCode,
    String category,
    String itemClass,
    UUID variantGroupId,
    String productFamilyName,
    List<String> colors,
    List<String> sizes,
    List<CatalogProductCartonSizeDto> cartonSizes,
    String unitOfMeasure,
    String hsnCode,
    BigDecimal basePrice,
    BigDecimal gstRate,
    BigDecimal minDiscountPercent,
    BigDecimal minSellingPrice,
    Map<String, Object> metadata,
    boolean active,
    SkuReadinessDto readiness
);
```

### CatalogProductRequest
```java
record CatalogProductRequest(
    Long brandId,
    String name,
    String itemClass,
    List<String> colors,
    List<String> sizes,
    List<CatalogProductCartonSizeRequest> cartonSizes,
    String unitOfMeasure,
    String hsnCode,
    BigDecimal basePrice,
    BigDecimal gstRate,
    BigDecimal minDiscountPercent,
    BigDecimal minSellingPrice,
    Map<String, Object> metadata,
    Boolean active
);
```

### CatalogProductCartonSizeDto
```java
record CatalogProductCartonSizeDto(
    String size,
    int piecesPerCarton
);
```

### CatalogProductCartonSizeRequest
```java
record CatalogProductCartonSizeRequest(
    String size,
    Integer piecesPerCarton
);
```

---

## Bulk Operations DTOs

### BulkVariantRequest
```java
record BulkVariantRequest(
    Long brandId,
    String brandName,
    String brandCode,
    String baseProductName,
    String category,
    List<String> colors,
    List<String> sizes,
    String unitOfMeasure,
    BigDecimal basePrice,
    BigDecimal gstRate,
    BigDecimal minDiscountPercent,
    BigDecimal minSellingPrice,
    Map<String, Object> metadata
);
```

### BulkVariantResponse
```java
record BulkVariantResponse(
    boolean dryRun,
    String brandName,
    String brandCode,
    String baseProductName,
    String category,
    int generatedCount,
    int wouldCreateCount,
    int createdCount,
    List<VariantItem> generated,
    List<VariantItem> wouldCreate,
    List<VariantItem> created,
    List<VariantItem> conflicts
);
```

### VariantItem
```java
record VariantItem(
    String sku,
    String color,
    String size,
    String productName,
    String reason,        // GENERATED/WOULD_CREATE/CREATED/SKU_ALREADY_EXISTS/...
    Long productId,
    UUID productPublicId
);
```

---

### CatalogProductEntryRequest
Canonical product entry with all variants in single request.

Key fields:
- `getBrandId()` - Required brand ID
- `getBaseProductName()` - Product family name
- `getUnitOfMeasure()` - Unit
- `getHsnCode()` - HSN code
- `getColors()` - List of colors
- `getSizes()` - List of sizes
- `getBasePrice()`, `getGstRate()`, etc.
- `getMetadata()` - Accounting metadata

### CatalogProductEntryResponse
```java
record CatalogProductEntryResponse(
    boolean preview,
    UUID variantGroupId,
    String productFamilyName,
    Long brandId,
    String brandName,
    String brandCode,
    String category,
    String itemClass,
    String unitOfMeasure,
    String hsnCode,
    BigDecimal basePrice,
    BigDecimal gstRate,
    BigDecimal minDiscountPercent,
    BigDecimal minSellingPrice,
    Map<String, Object> metadata,
    int generatedCount,
    DownstreamEffects downstreamEffects,
    List<Member> members,
    List<Conflict> conflicts
);
```

---

### CatalogImportResponse
```java
record CatalogImportResponse(
    int rowsProcessed,
    int brandsCreated,
    int productsCreated,
    int productsUpdated,
    int rawMaterialsSeeded,
    List<ImportError> errors
);
```

---

## Readiness DTOs

### SkuReadinessDto
```java
record SkuReadinessDto(
    String sku,
    Stage catalog,
    Stage inventory,
    Stage production,
    Stage packing,
    Stage sales,
    Stage accounting
);
```

### Stage
```java
record Stage(
    boolean ready,
    List<String> blockers
);
```

---

## Create/Update DTOs

### ProductCreateRequest
```java
record ProductCreateRequest(
    Long brandId,
    String brandName,
    String brandCode,
    String productName,
    String category,
    String itemClass,
    String defaultColour,
    String sizeLabel,
    String unitOfMeasure,
    String hsnCode,
    String customSkuCode,
    BigDecimal basePrice,
    BigDecimal gstRate,
    BigDecimal minDiscountPercent,
    BigDecimal minSellingPrice,
    Map<String, Object> metadata,
    Boolean active
);
```

### ProductUpdateRequest
```java
record ProductUpdateRequest(
    String productName,
    String category,
    String itemClass,
    String defaultColour,
    String sizeLabel,
    String unitOfMeasure,
    String hsnCode,
    BigDecimal basePrice,
    BigDecimal gstRate,
    BigDecimal minDiscountPercent,
    BigDecimal minSellingPrice,
    Map<String, Object> metadata,
    Boolean active
);
```
