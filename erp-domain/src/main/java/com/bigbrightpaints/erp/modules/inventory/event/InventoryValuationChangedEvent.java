package com.bigbrightpaints.erp.modules.inventory.event;

import com.bigbrightpaints.erp.core.util.CompanyTime;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Domain event published when inventory valuation changes.
 * Accounting module subscribes to automatically create adjustment journal entries.
 * 
 * Triggers:
 * - Cost method change (FIFO → Weighted Average)
 * - Landed cost adjustment
 * - Revaluation (market value adjustment)
 * - Inventory count adjustment
 * - Standard cost update
 */
public record InventoryValuationChangedEvent(
        Long companyId,
        InventoryType inventoryType,      // RAW_MATERIAL or FINISHED_GOOD
        Long itemId,                       // RawMaterial.id or FinishedGood.id
        String itemCode,
        String itemName,
        Long inventoryAccountId,           // GL account for inventory asset
        BigDecimal oldValue,               // Previous total inventory value
        BigDecimal newValue,               // New total inventory value
        BigDecimal quantity,               // Current quantity on hand
        BigDecimal oldUnitCost,            // Previous unit cost
        BigDecimal newUnitCost,            // New unit cost
        ValuationChangeReason reason,
        String referenceNumber,            // Source document (e.g., "ADJ-001", "LC-001")
        String memo,
        Instant timestamp
) {
    public BigDecimal getValueChange() {
        return newValue.subtract(oldValue);
    }
    
    public boolean isIncrease() {
        return newValue.compareTo(oldValue) > 0;
    }
    
    public enum InventoryType {
        RAW_MATERIAL,
        FINISHED_GOOD,
        WORK_IN_PROGRESS
    }
    
    public enum ValuationChangeReason {
        COST_METHOD_CHANGE,      // FIFO → LIFO → Weighted Average
        LANDED_COST_ADJUSTMENT,  // Freight, customs, insurance added
        MARKET_REVALUATION,      // Lower of cost or market (LCM)
        PHYSICAL_COUNT_ADJUSTMENT, // Inventory count variance
        STANDARD_COST_UPDATE,    // Standard cost revision
        PURCHASE_PRICE_VARIANCE, // Actual vs. standard cost
        SCRAP_WRITEOFF,          // Damaged/obsolete inventory
        INTERCOMPANY_TRANSFER    // Transfer pricing adjustment
    }
    
    // Builder for convenience
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private Long companyId;
        private InventoryType inventoryType;
        private Long itemId;
        private String itemCode;
        private String itemName;
        private Long inventoryAccountId;
        private BigDecimal oldValue = BigDecimal.ZERO;
        private BigDecimal newValue = BigDecimal.ZERO;
        private BigDecimal quantity = BigDecimal.ZERO;
        private BigDecimal oldUnitCost = BigDecimal.ZERO;
        private BigDecimal newUnitCost = BigDecimal.ZERO;
        private ValuationChangeReason reason;
        private String referenceNumber;
        private String memo;
        
        public Builder companyId(Long companyId) { this.companyId = companyId; return this; }
        public Builder inventoryType(InventoryType type) { this.inventoryType = type; return this; }
        public Builder itemId(Long itemId) { this.itemId = itemId; return this; }
        public Builder itemCode(String itemCode) { this.itemCode = itemCode; return this; }
        public Builder itemName(String itemName) { this.itemName = itemName; return this; }
        public Builder inventoryAccountId(Long id) { this.inventoryAccountId = id; return this; }
        public Builder oldValue(BigDecimal val) { this.oldValue = val; return this; }
        public Builder newValue(BigDecimal val) { this.newValue = val; return this; }
        public Builder quantity(BigDecimal qty) { this.quantity = qty; return this; }
        public Builder oldUnitCost(BigDecimal cost) { this.oldUnitCost = cost; return this; }
        public Builder newUnitCost(BigDecimal cost) { this.newUnitCost = cost; return this; }
        public Builder reason(ValuationChangeReason reason) { this.reason = reason; return this; }
        public Builder referenceNumber(String ref) { this.referenceNumber = ref; return this; }
        public Builder memo(String memo) { this.memo = memo; return this; }
        
        public InventoryValuationChangedEvent build() {
            return new InventoryValuationChangedEvent(
                    companyId, inventoryType, itemId, itemCode, itemName,
                    inventoryAccountId, oldValue, newValue, quantity,
                    oldUnitCost, newUnitCost, reason, referenceNumber, memo,
                    CompanyTime.now()
            );
        }
    }
}
