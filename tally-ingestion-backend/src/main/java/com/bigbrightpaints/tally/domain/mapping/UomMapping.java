package com.bigbrightpaints.tally.domain.mapping;

import com.bigbrightpaints.tally.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Unit of Measure mapping from Tally to internal UOM
 */
@Entity
@Table(name = "tally_uom_mappings",
        uniqueConstraints = {
                @UniqueConstraint(name = "idx_unique_uom_mapping",
                        columnNames = {"company_id", "tally_uom"})
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class UomMapping extends BaseEntity {

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "tally_uom", nullable = false, length = 100)
    private String tallyUom;

    @Column(name = "internal_uom", nullable = false, length = 50)
    private String internalUom;

    @Column(name = "conversion_factor", precision = 10, scale = 6)
    private BigDecimal conversionFactor = BigDecimal.ONE;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
}