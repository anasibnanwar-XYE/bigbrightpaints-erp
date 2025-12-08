package com.bigbrightpaints.tally.dto;

import com.bigbrightpaints.tally.domain.staging.StagingStockItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockItemDto {
    private Long id;
    private Integer rowNumber;
    private String itemName;
    private BigDecimal closingQuantity;
    private String unitOfMeasure;
    private BigDecimal closingRate;
    private BigDecimal closingAmount;

    // Classification fields (user-editable)
    private StagingStockItem.ItemType itemType;
    private String category;
    private String brand;
    private String sizeLabel;
    private String color;

    // Mapping fields (user-editable)
    private String mappedSku;
    private String mappedProductCode;
    private String baseProductName;
    private BigDecimal gstRate;
    private String hsnCode;
    private String notes;

    // Status
    private StagingStockItem.ValidationStatus validationStatus;
    private Map<String, String> validationErrors;
    private Boolean processed;

    // Raw data
    private Map<String, Object> rawData;
}
