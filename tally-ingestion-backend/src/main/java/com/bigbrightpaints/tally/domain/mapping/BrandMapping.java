package com.bigbrightpaints.tally.domain.mapping;

import com.bigbrightpaints.tally.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * Brand name mapping from Tally to canonical names
 */
@Entity
@Table(name = "tally_brand_mappings",
        uniqueConstraints = {
                @UniqueConstraint(name = "idx_unique_brand_mapping",
                        columnNames = {"company_id", "tally_brand_name"})
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class BrandMapping extends BaseEntity {

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "tally_brand_name", nullable = false)
    private String tallyBrandName;

    @Column(name = "canonical_brand_name", nullable = false)
    private String canonicalBrandName;

    @Column(name = "brand_code", length = 50)
    private String brandCode;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_by")
    private Long createdBy;
}