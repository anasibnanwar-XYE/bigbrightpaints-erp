package com.bigbrightpaints.erp.modules.inventory.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import com.bigbrightpaints.erp.core.domain.VersionedEntity;

@Entity
@Table(name = "packaging_slip_lines")
public class PackagingSlipLine extends VersionedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "packaging_slip_id")
    private PackagingSlip packagingSlip;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "finished_good_batch_id")
    private FinishedGoodBatch finishedGoodBatch;

    @Column(name = "ordered_quantity", nullable = false)
    private BigDecimal orderedQuantity;

    @Column(name = "shipped_quantity")
    private BigDecimal shippedQuantity;

    @Column(nullable = false)
    private BigDecimal quantity;

    @Column(name = "unit_cost", nullable = false)
    private BigDecimal unitCost = BigDecimal.ZERO;

    @Column(name = "backorder_quantity")
    private BigDecimal backorderQuantity;

    @Column(length = 500)
    private String notes;

    public Long getId() { return id; }
    public PackagingSlip getPackagingSlip() { return packagingSlip; }
    public void setPackagingSlip(PackagingSlip packagingSlip) { this.packagingSlip = packagingSlip; }
    public FinishedGoodBatch getFinishedGoodBatch() { return finishedGoodBatch; }
    public void setFinishedGoodBatch(FinishedGoodBatch finishedGoodBatch) { this.finishedGoodBatch = finishedGoodBatch; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public BigDecimal getUnitCost() { return unitCost; }
    public void setUnitCost(BigDecimal unitCost) { this.unitCost = unitCost; }
    public BigDecimal getOrderedQuantity() { return orderedQuantity; }
    public void setOrderedQuantity(BigDecimal orderedQuantity) { this.orderedQuantity = orderedQuantity; }
    public BigDecimal getShippedQuantity() { return shippedQuantity; }
    public void setShippedQuantity(BigDecimal shippedQuantity) { this.shippedQuantity = shippedQuantity; }
    public BigDecimal getBackorderQuantity() { return backorderQuantity; }
    public void setBackorderQuantity(BigDecimal backorderQuantity) { this.backorderQuantity = backorderQuantity; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
