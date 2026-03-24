package com.bigbrightpaints.erp.modules.purchasing.domain;

import java.math.BigDecimal;

import com.bigbrightpaints.erp.core.domain.VersionedEntity;
import com.bigbrightpaints.erp.modules.inventory.domain.RawMaterial;

import jakarta.persistence.*;

@Entity
@Table(name = "purchase_order_items")
public class PurchaseOrderLine extends VersionedEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "purchase_order_id")
  private PurchaseOrder purchaseOrder;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "raw_material_id")
  private RawMaterial rawMaterial;

  @Column(nullable = false)
  private BigDecimal quantity;

  @Column(nullable = false)
  private String unit;

  @Column(name = "cost_per_unit", nullable = false)
  private BigDecimal costPerUnit;

  @Column(name = "line_total", nullable = false)
  private BigDecimal lineTotal;

  @Column(name = "notes")
  private String notes;

  public Long getId() {
    return id;
  }

  public PurchaseOrder getPurchaseOrder() {
    return purchaseOrder;
  }

  public void setPurchaseOrder(PurchaseOrder purchaseOrder) {
    this.purchaseOrder = purchaseOrder;
  }

  public RawMaterial getRawMaterial() {
    return rawMaterial;
  }

  public void setRawMaterial(RawMaterial rawMaterial) {
    this.rawMaterial = rawMaterial;
  }

  public BigDecimal getQuantity() {
    return quantity;
  }

  public void setQuantity(BigDecimal quantity) {
    this.quantity = quantity;
  }

  public String getUnit() {
    return unit;
  }

  public void setUnit(String unit) {
    this.unit = unit;
  }

  public BigDecimal getCostPerUnit() {
    return costPerUnit;
  }

  public void setCostPerUnit(BigDecimal costPerUnit) {
    this.costPerUnit = costPerUnit;
  }

  public BigDecimal getLineTotal() {
    return lineTotal;
  }

  public void setLineTotal(BigDecimal lineTotal) {
    this.lineTotal = lineTotal;
  }

  public String getNotes() {
    return notes;
  }

  public void setNotes(String notes) {
    this.notes = notes;
  }
}
