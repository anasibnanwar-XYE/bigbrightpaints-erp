package com.bigbrightpaints.erp.modules.factory.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.bigbrightpaints.erp.modules.inventory.domain.RawMaterial;
import com.bigbrightpaints.erp.modules.inventory.domain.RawMaterialBatch;
import com.bigbrightpaints.erp.modules.inventory.domain.RawMaterialBatchRepository;
import com.bigbrightpaints.erp.modules.inventory.domain.RawMaterialMovement;
import com.bigbrightpaints.erp.modules.inventory.domain.RawMaterialMovementRepository;
import com.bigbrightpaints.erp.modules.inventory.domain.RawMaterialRepository;

@ExtendWith(MockitoExtension.class)
class BulkPackingInventoryServiceTest {

  @Mock private RawMaterialBatchRepository rawMaterialBatchRepository;
  @Mock private RawMaterialRepository rawMaterialRepository;
  @Mock private RawMaterialMovementRepository rawMaterialMovementRepository;

  private BulkPackingInventoryService service;

  @BeforeEach
  void setUp() {
    service =
        new BulkPackingInventoryService(
            rawMaterialBatchRepository, rawMaterialRepository, rawMaterialMovementRepository);
    when(rawMaterialMovementRepository.save(any(RawMaterialMovement.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
  }

  @Test
  void consumeBulkInventory_reducesStockAndWritesIssueMovement() {
    RawMaterial material = new RawMaterial();
    material.setSku("PAINT-BULK");
    material.setCurrentStock(new BigDecimal("100"));

    RawMaterialBatch batch = new RawMaterialBatch();
    batch.setRawMaterial(material);
    batch.setBatchCode("BULK-100");
    batch.setQuantity(new BigDecimal("100"));
    batch.setCostPerUnit(new BigDecimal("18.75"));

    service.consumeBulkInventory(batch, new BigDecimal("25"), "PACK-BULK-100");

    assertThat(batch.getQuantity()).isEqualByComparingTo(new BigDecimal("75"));
    assertThat(material.getCurrentStock()).isEqualByComparingTo(new BigDecimal("75"));
    verify(rawMaterialBatchRepository).save(batch);
    verify(rawMaterialRepository).save(material);

    ArgumentCaptor<RawMaterialMovement> movementCaptor =
        ArgumentCaptor.forClass(RawMaterialMovement.class);
    verify(rawMaterialMovementRepository).save(movementCaptor.capture());

    RawMaterialMovement movement = movementCaptor.getValue();
    assertThat(movement.getRawMaterial()).isSameAs(material);
    assertThat(movement.getRawMaterialBatch()).isSameAs(batch);
    assertThat(movement.getMovementType()).isEqualTo("ISSUE");
    assertThat(movement.getReferenceId()).isEqualTo("PACK-BULK-100");
    assertThat(movement.getQuantity()).isEqualByComparingTo(new BigDecimal("25"));
    assertThat(movement.getUnitCost()).isEqualByComparingTo(new BigDecimal("18.75"));
  }

  @Test
  void consumeBulkInventory_usesZeroCostWhenBatchCostMissing() {
    RawMaterial material = new RawMaterial();
    material.setSku("PAINT-BULK");
    material.setCurrentStock(new BigDecimal("5"));

    RawMaterialBatch batch = new RawMaterialBatch();
    batch.setRawMaterial(material);
    batch.setBatchCode("BULK-NULL-COST");
    batch.setQuantity(new BigDecimal("5"));
    batch.setCostPerUnit(null);

    service.consumeBulkInventory(batch, new BigDecimal("2"), "PACK-BULK-NULL");

    ArgumentCaptor<RawMaterialMovement> movementCaptor =
        ArgumentCaptor.forClass(RawMaterialMovement.class);
    verify(rawMaterialMovementRepository).save(movementCaptor.capture());
    assertThat(movementCaptor.getValue().getUnitCost()).isEqualByComparingTo(BigDecimal.ZERO);
  }
}
