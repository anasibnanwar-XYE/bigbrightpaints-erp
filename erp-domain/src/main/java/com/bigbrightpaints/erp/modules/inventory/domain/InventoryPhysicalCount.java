package com.bigbrightpaints.erp.modules.inventory.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import com.bigbrightpaints.erp.core.domain.VersionedEntity;
import com.bigbrightpaints.erp.core.util.CompanyTime;
import com.bigbrightpaints.erp.modules.company.domain.Company;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "inventory_physical_counts")
public class InventoryPhysicalCount extends VersionedEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "company_id")
  private Company company;

  @Enumerated(EnumType.STRING)
  @Column(name = "count_target", nullable = false, length = 32)
  private InventoryPhysicalCountTarget target;

  @Column(name = "inventory_item_id", nullable = false)
  private Long inventoryItemId;

  @Column(name = "physical_quantity", nullable = false)
  private BigDecimal physicalQuantity = BigDecimal.ZERO;

  @Column(name = "count_date", nullable = false)
  private LocalDate countDate;

  @Column(name = "source_reference", length = 255)
  private String sourceReference;

  @Column(name = "note")
  private String note;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "created_by", length = 255)
  private String createdBy;

  @PrePersist
  public void prePersist() {
    if (createdAt == null) {
      createdAt = CompanyTime.now(company);
    }
    if (countDate == null) {
      countDate = CompanyTime.today(company);
    }
  }

  public Long getId() {
    return id;
  }

  public Company getCompany() {
    return company;
  }

  public void setCompany(Company company) {
    this.company = company;
  }

  public InventoryPhysicalCountTarget getTarget() {
    return target;
  }

  public void setTarget(InventoryPhysicalCountTarget target) {
    this.target = target;
  }

  public Long getInventoryItemId() {
    return inventoryItemId;
  }

  public void setInventoryItemId(Long inventoryItemId) {
    this.inventoryItemId = inventoryItemId;
  }

  public BigDecimal getPhysicalQuantity() {
    return physicalQuantity;
  }

  public void setPhysicalQuantity(BigDecimal physicalQuantity) {
    this.physicalQuantity = physicalQuantity;
  }

  public LocalDate getCountDate() {
    return countDate;
  }

  public void setCountDate(LocalDate countDate) {
    this.countDate = countDate;
  }

  public String getSourceReference() {
    return sourceReference;
  }

  public void setSourceReference(String sourceReference) {
    this.sourceReference = sourceReference;
  }

  public String getNote() {
    return note;
  }

  public void setNote(String note) {
    this.note = note;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public String getCreatedBy() {
    return createdBy;
  }

  public void setCreatedBy(String createdBy) {
    this.createdBy = createdBy;
  }
}
