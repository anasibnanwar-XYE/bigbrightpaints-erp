package com.bigbrightpaints.erp.modules.factory.service;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;

import com.bigbrightpaints.erp.modules.inventory.domain.InventoryReference;
import com.bigbrightpaints.erp.modules.inventory.domain.RawMaterial;
import com.bigbrightpaints.erp.modules.inventory.domain.RawMaterialBatch;
import com.bigbrightpaints.erp.modules.inventory.domain.RawMaterialBatchRepository;
import com.bigbrightpaints.erp.modules.inventory.domain.RawMaterialMovement;
import com.bigbrightpaints.erp.modules.inventory.domain.RawMaterialMovementRepository;
import com.bigbrightpaints.erp.modules.inventory.domain.RawMaterialRepository;

@Service
public class BulkPackingInventoryService {

  private final RawMaterialBatchRepository rawMaterialBatchRepository;
  private final RawMaterialRepository rawMaterialRepository;
  private final RawMaterialMovementRepository rawMaterialMovementRepository;

  public BulkPackingInventoryService(
      RawMaterialBatchRepository rawMaterialBatchRepository,
      RawMaterialRepository rawMaterialRepository,
      RawMaterialMovementRepository rawMaterialMovementRepository) {
    this.rawMaterialBatchRepository = rawMaterialBatchRepository;
    this.rawMaterialRepository = rawMaterialRepository;
    this.rawMaterialMovementRepository = rawMaterialMovementRepository;
  }

  public void consumeBulkInventory(
      RawMaterialBatch bulkBatch, BigDecimal totalVolume, String packReference) {
    bulkBatch.setQuantity(bulkBatch.getQuantity().subtract(totalVolume));
    rawMaterialBatchRepository.save(bulkBatch);

    RawMaterial bulkMaterial = bulkBatch.getRawMaterial();
    bulkMaterial.setCurrentStock(bulkMaterial.getCurrentStock().subtract(totalVolume));
    rawMaterialRepository.save(bulkMaterial);

    RawMaterialMovement bulkIssue = new RawMaterialMovement();
    bulkIssue.setRawMaterial(bulkMaterial);
    bulkIssue.setRawMaterialBatch(bulkBatch);
    bulkIssue.setReferenceType(InventoryReference.PACKING_RECORD);
    bulkIssue.setReferenceId(packReference);
    bulkIssue.setMovementType("ISSUE");
    bulkIssue.setQuantity(totalVolume);
    bulkIssue.setUnitCost(
        bulkBatch.getCostPerUnit() != null ? bulkBatch.getCostPerUnit() : BigDecimal.ZERO);
    rawMaterialMovementRepository.save(bulkIssue);
  }
}
