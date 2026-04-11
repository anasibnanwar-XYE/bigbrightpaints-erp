package com.bigbrightpaints.erp.modules.inventory.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class InventoryMutationHelpersTest {

  @Test
  void inventoryAdjustment_setLinesAndAddLine_handleNullAndCopyInputs() {
    InventoryAdjustment adjustment = new InventoryAdjustment();
    InventoryAdjustmentLine line = new InventoryAdjustmentLine();

    adjustment.setLines(null);
    adjustment.addLine(null);
    assertThat(adjustment.getLines()).isEmpty();

    List<InventoryAdjustmentLine> source = new ArrayList<>(List.of(line));
    adjustment.setLines(source);
    source.clear();
    adjustment.addLine(line);

    assertThat(adjustment.getLines()).hasSize(2);
  }

  @Test
  void rawMaterialAdjustment_setLinesAndAddLine_handleNullAndCopyInputs() {
    RawMaterialAdjustment adjustment = new RawMaterialAdjustment();
    RawMaterialAdjustmentLine line = new RawMaterialAdjustmentLine();

    adjustment.setLines(null);
    adjustment.addLine(null);
    assertThat(adjustment.getLines()).isEmpty();

    List<RawMaterialAdjustmentLine> source = new ArrayList<>(List.of(line));
    adjustment.setLines(source);
    source.clear();
    adjustment.addLine(line);

    assertThat(adjustment.getLines()).hasSize(2);
  }
}
