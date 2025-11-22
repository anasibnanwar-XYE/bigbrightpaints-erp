package com.bigbrightpaints.tally.domain.staging;

import com.bigbrightpaints.tally.domain.BaseEntity;
import com.bigbrightpaints.tally.domain.IngestionRun;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/**
 * Staging table for Tally product/stock item data
 */
@Entity
@Table(name = "stg_tally_products")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class StagingProduct extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "run_id", nullable = false)
    private IngestionRun run;

    @Column(name = "row_number", nullable = false)
    private Integer rowNumber;

    // Tally fields
    @Column(name = "stock_item_name", length = 500)
    private String stockItemName;

    @Column(name = "stock_group")
    private String stockGroup;

    @Column(name = "stock_category")
    private String stockCategory;

    @Column(name = "base_unit", length = 50)
    private String baseUnit;

    @Column(name = "alternate_unit", length = 50)
    private String alternateUnit;

    @Column(name = "conversion_factor", precision = 10, scale = 4)
    private BigDecimal conversionFactor;

    @Column(name = "gst_rate", precision = 5, scale = 2)
    private BigDecimal gstRate;

    @Column(name = "hsn_code", length = 20)
    private String hsnCode;

    @Column(name = "item_code", length = 100)
    private String itemCode;

    @Column(name = "barcode", length = 100)
    private String barcode;

    @Column(name = "brand")
    private String brand;

    // Variant attributes (parsed from name)
    @Column(name = "base_product_name")
    private String baseProductName;

    @Column(name = "color", length = 100)
    private String color;

    @Column(name = "size", length = 100)
    private String size;

    @Column(name = "pack_size", length = 100)
    private String packSize;

    // Processing fields
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_data", columnDefinition = "jsonb")
    private Map<String, Object> rawData;

    @Column(name = "source_hash", length = 64)
    private String sourceHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "validation_status", length = 32)
    private ValidationStatus validationStatus = ValidationStatus.PENDING;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "validation_errors", columnDefinition = "jsonb")
    private Map<String, String> validationErrors;

    @Column(name = "processed")
    private Boolean processed = false;

    @Column(name = "processed_at")
    private Instant processedAt;

    // Mapped IDs
    @Column(name = "mapped_brand_id")
    private Long mappedBrandId;

    @Column(name = "mapped_product_id")
    private Long mappedProductId;

    @Column(name = "mapped_variant_id")
    private Long mappedVariantId;

    @Column(name = "generated_sku", length = 100)
    private String generatedSku;

    public enum ValidationStatus {
        PENDING,
        VALID,
        INVALID
    }
}