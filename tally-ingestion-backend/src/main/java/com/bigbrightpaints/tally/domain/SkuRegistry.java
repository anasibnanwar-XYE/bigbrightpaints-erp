package com.bigbrightpaints.tally.domain;

import jakarta.persistence.*;
import lombok.*;

/**
 * Registry for tracking generated SKUs and preventing collisions
 */
@Entity
@Table(name = "tally_sku_registry",
        indexes = {
                @Index(name = "idx_sku_registry_variant", columnList = "variant_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "idx_unique_sku",
                        columnNames = {"company_id", "sku"})
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class SkuRegistry extends BaseEntity {

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "sku", nullable = false, length = 100)
    private String sku;

    @Column(name = "variant_id")
    private Long variantId;

    @Column(name = "brand")
    private String brand;

    @Column(name = "base_product")
    private String baseProduct;

    @Column(name = "color", length = 100)
    private String color;

    @Column(name = "size", length = 100)
    private String size;

    @Column(name = "pack", length = 100)
    private String pack;

    @Column(name = "counter_suffix")
    private Integer counterSuffix = 0;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    // Business methods
    public String generateSkuWithSuffix() {
        if (counterSuffix != null && counterSuffix > 0) {
            return sku + "-" + String.format("%02d", counterSuffix);
        }
        return sku;
    }

    public void incrementCounter() {
        this.counterSuffix = (this.counterSuffix != null ? this.counterSuffix : 0) + 1;
    }
}