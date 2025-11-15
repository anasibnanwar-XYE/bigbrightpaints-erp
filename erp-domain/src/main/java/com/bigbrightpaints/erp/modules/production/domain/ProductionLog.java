package com.bigbrightpaints.erp.modules.production.domain;

import com.bigbrightpaints.erp.modules.company.domain.Company;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "production_logs",
        uniqueConstraints = @UniqueConstraint(columnNames = {"company_id", "production_code"}))
public class ProductionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false)
    private UUID publicId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id")
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "brand_id")
    private ProductionBrand brand;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id")
    private ProductionProduct product;

    @Column(name = "production_code", nullable = false)
    private String productionCode;

    @Column(name = "batch_colour")
    private String batchColour;

    @Column(name = "batch_size", nullable = false)
    private BigDecimal batchSize;

    @Column(name = "unit_of_measure", nullable = false)
    private String unitOfMeasure;

    @Column(name = "produced_quantity", nullable = false)
    private BigDecimal producedQuantity;

    @Column(name = "produced_at", nullable = false)
    private Instant producedAt;

    private String notes;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "log", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductionLogMaterial> materials = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (publicId == null) {
            publicId = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (updatedAt == null) {
            updatedAt = Instant.now();
        }
        if (producedAt == null) {
            producedAt = Instant.now();
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }

    public void addMaterial(ProductionLogMaterial material) {
        material.setLog(this);
        this.materials.add(material);
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

    public ProductionBrand getBrand() {
        return brand;
    }

    public void setBrand(ProductionBrand brand) {
        this.brand = brand;
    }

    public ProductionProduct getProduct() {
        return product;
    }

    public void setProduct(ProductionProduct product) {
        this.product = product;
    }

    public String getProductionCode() {
        return productionCode;
    }

    public void setProductionCode(String productionCode) {
        this.productionCode = productionCode;
    }

    public String getBatchColour() {
        return batchColour;
    }

    public void setBatchColour(String batchColour) {
        this.batchColour = batchColour;
    }

    public BigDecimal getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(BigDecimal batchSize) {
        this.batchSize = batchSize;
    }

    public String getUnitOfMeasure() {
        return unitOfMeasure;
    }

    public void setUnitOfMeasure(String unitOfMeasure) {
        this.unitOfMeasure = unitOfMeasure;
    }

    public BigDecimal getProducedQuantity() {
        return producedQuantity;
    }

    public void setProducedQuantity(BigDecimal producedQuantity) {
        this.producedQuantity = producedQuantity;
    }

    public Instant getProducedAt() {
        return producedAt;
    }

    public void setProducedAt(Instant producedAt) {
        this.producedAt = producedAt;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public List<ProductionLogMaterial> getMaterials() {
        return materials;
    }
}
