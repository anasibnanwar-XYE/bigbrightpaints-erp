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
 * Staging table for Tally XML stock summary import
 */
@Entity
@Table(name = "stg_tally_stock_items")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class StagingStockItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "run_id", nullable = false)
    private IngestionRun run;

    @Column(name = "row_number", nullable = false)
    private Integer rowNumber;

    // Tally XML fields - Stock Summary
    @Column(name = "item_name", length = 500, nullable = false)
    private String itemName;

    @Column(name = "closing_quantity", precision = 19, scale = 4)
    private BigDecimal closingQuantity;

    @Column(name = "unit_of_measure", length = 50)
    private String unitOfMeasure;

    @Column(name = "closing_rate", precision = 19, scale = 4)
    private BigDecimal closingRate;

    @Column(name = "closing_amount", precision = 19, scale = 2)
    private BigDecimal closingAmount;

    // Classification (User-editable)
    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", length = 32)
    private ItemType itemType;

    @Column(name = "category", length = 100)
    private String category;

    @Column(name = "brand", length = 100)
    private String brand;

    @Column(name = "size_label", length = 50)
    private String sizeLabel;

    @Column(name = "color", length = 100)
    private String color;

    // User-editable fields
    @Column(name = "mapped_sku", length = 100)
    private String mappedSku;

    @Column(name = "mapped_product_code", length = 100)
    private String mappedProductCode;

    @Column(name = "base_product_name", length = 300)
    private String baseProductName;

    @Column(name = "gst_rate", precision = 5, scale = 2)
    private BigDecimal gstRate;

    @Column(name = "hsn_code", length = 20)
    private String hsnCode;

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

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    // Mapped IDs (after import)
    @Column(name = "mapped_product_id")
    private Long mappedProductId;

    @Column(name = "mapped_raw_material_id")
    private Long mappedRawMaterialId;

    @Column(name = "mapped_batch_id")
    private Long mappedBatchId;

    public enum ItemType {
        RAW_MATERIAL,       // Raw materials (pigments, chemicals, resins)
        FINISHED_PRODUCT,   // Finished goods (paints)
        PACKAGING,          // Packaging materials (buckets, tins, cartoons)
        ASSET,              // Assets (machinery, equipment)
        EXPENSE,            // Expense items (t-shirts, pens, stationery)
        UNKNOWN             // Not yet classified
    }

    public enum ValidationStatus {
        PENDING,
        VALID,
        INVALID,
        NEEDS_REVIEW
    }
}
