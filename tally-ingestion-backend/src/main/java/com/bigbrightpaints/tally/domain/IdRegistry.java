package com.bigbrightpaints.tally.domain;

import jakarta.persistence.*;
import lombok.*;

/**
 * Registry for tracking deterministically generated IDs
 */
@Entity
@Table(name = "tally_id_registry",
        indexes = {
                @Index(name = "idx_id_registry_company", columnList = "company_id"),
                @Index(name = "idx_id_registry_type", columnList = "entity_type"),
                @Index(name = "idx_id_registry_hash", columnList = "source_hash"),
                @Index(name = "idx_id_registry_generated", columnList = "generated_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "idx_unique_id_registry",
                        columnNames = {"company_id", "entity_type", "source_hash"})
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class IdRegistry extends BaseEntity {

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false, length = 50)
    private EntityType entityType;

    @Column(name = "source_key", nullable = false, length = 500)
    private String sourceKey;

    @Column(name = "source_hash", nullable = false, length = 64)
    private String sourceHash;

    @Column(name = "generated_id", nullable = false, length = 100)
    private String generatedId;

    @Column(name = "generated_code", length = 100)
    private String generatedCode;

    @Column(name = "generated_sku", length = 100)
    private String generatedSku;

    @Column(name = "mapped_entity_id")
    private Long mappedEntityId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "first_seen_run_id")
    private IngestionRun firstSeenRun;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_seen_run_id")
    private IngestionRun lastSeenRun;

    @Column(name = "occurrence_count")
    private Integer occurrenceCount = 1;

    public enum EntityType {
        DEALER,
        SUPPLIER,
        PRODUCT,
        VARIANT,
        ACCOUNT,
        BRAND,
        CATEGORY
    }

    // Business methods
    public void incrementOccurrence(IngestionRun run) {
        this.occurrenceCount = (this.occurrenceCount != null ? this.occurrenceCount : 0) + 1;
        this.lastSeenRun = run;
    }

    public boolean isNewEntity() {
        return this.mappedEntityId == null;
    }
}