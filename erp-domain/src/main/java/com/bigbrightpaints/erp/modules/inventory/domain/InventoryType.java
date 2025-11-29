package com.bigbrightpaints.erp.modules.inventory.domain;

/**
 * Inventory type for GST/Non-GST classification.
 * 
 * STANDARD - GST applicable inventory (default)
 * PRIVATE - Non-GST / Private stock (for internal use, samples, etc.)
 */
public enum InventoryType {
    STANDARD,  // GST applicable
    PRIVATE    // Non-GST / Private
}
