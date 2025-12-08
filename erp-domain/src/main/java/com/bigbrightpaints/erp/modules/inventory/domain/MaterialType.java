package com.bigbrightpaints.erp.modules.inventory.domain;

/**
 * Distinguishes raw materials by their usage context.
 * 
 * PRODUCTION - Used in manufacturing (pigments, resins, solvents)
 * PACKAGING - Used in packing step (buckets, cans, cartons, lids)
 */
public enum MaterialType {
    PRODUCTION,  // Shows in production run "Materials consumed"
    PACKAGING    // Shows in packing step "Packaging materials"
}
