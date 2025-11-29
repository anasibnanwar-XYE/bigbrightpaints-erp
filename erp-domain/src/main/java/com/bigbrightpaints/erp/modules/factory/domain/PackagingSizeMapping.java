package com.bigbrightpaints.erp.modules.factory.domain;

import com.bigbrightpaints.erp.core.domain.VersionedEntity;
import com.bigbrightpaints.erp.modules.company.domain.Company;
import com.bigbrightpaints.erp.modules.inventory.domain.RawMaterial;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Maps packaging sizes (e.g., "1L", "5L", "10L") to their corresponding
 * raw material (bucket) for automatic deduction during packing.
 */
@Entity
@Table(name = "packaging_size_mappings",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_packaging_size_company",
                columnNames = {"company_id", "packaging_size"}))
public class PackagingSizeMapping extends VersionedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false)
    private UUID publicId;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;

    @Column(name = "packaging_size", nullable = false, length = 50)
    private String packagingSize;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "raw_material_id")
    private RawMaterial rawMaterial;

    @Column(name = "units_per_pack", nullable = false)
    private Integer unitsPerPack = 1;

    @Column(name = "carton_size")
    private Integer cartonSize;

    @Column(name = "liters_per_unit", nullable = false)
    private java.math.BigDecimal litersPerUnit;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        if (publicId == null) {
            publicId = UUID.randomUUID();
        }
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public UUID getPublicId() {
        return publicId;
    }

    public Company getCompany() {
        return company;
    }

    public void setCompany(Company company) {
        this.company = company;
    }

    public String getPackagingSize() {
        return packagingSize;
    }

    public void setPackagingSize(String packagingSize) {
        this.packagingSize = packagingSize;
    }

    public RawMaterial getRawMaterial() {
        return rawMaterial;
    }

    public void setRawMaterial(RawMaterial rawMaterial) {
        this.rawMaterial = rawMaterial;
    }

    public Integer getUnitsPerPack() {
        return unitsPerPack;
    }

    public void setUnitsPerPack(Integer unitsPerPack) {
        this.unitsPerPack = unitsPerPack;
    }

    public Integer getCartonSize() {
        return cartonSize;
    }

    public void setCartonSize(Integer cartonSize) {
        this.cartonSize = cartonSize;
    }

    public java.math.BigDecimal getLitersPerUnit() {
        return litersPerUnit;
    }

    public void setLitersPerUnit(java.math.BigDecimal litersPerUnit) {
        this.litersPerUnit = litersPerUnit;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
