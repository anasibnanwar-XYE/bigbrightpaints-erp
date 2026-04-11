package com.bigbrightpaints.erp.modules.accounting.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.bigbrightpaints.erp.modules.accounting.dto.InventoryRevaluationRequest;
import com.bigbrightpaints.erp.modules.accounting.dto.JournalEntryDto;
import com.bigbrightpaints.erp.modules.accounting.dto.JournalLineDto;
import com.bigbrightpaints.erp.modules.accounting.dto.LandedCostRequest;
import com.bigbrightpaints.erp.modules.accounting.dto.WipAdjustmentRequest;
import com.bigbrightpaints.erp.modules.accounting.service.InventoryAccountingService;
import com.bigbrightpaints.erp.shared.dto.ApiResponse;

class InventoryAccountingControllerTest {

  @Test
  void recordLandedCost_delegates() {
    InventoryAccountingService service = mock(InventoryAccountingService.class);
    InventoryAccountingController controller = new InventoryAccountingController(service);
    LandedCostRequest request =
        new LandedCostRequest(
            40L,
            new BigDecimal("125.00"),
            10L,
            11L,
            LocalDate.of(2026, 3, 31),
            "freight",
            "LC-10",
            null,
            false);
    JournalEntryDto expected = journal(10L, "LC-10");
    when(service.recordLandedCost(request)).thenReturn(expected);

    ApiResponse<JournalEntryDto> body = controller.recordLandedCost(request).getBody();

    assertThat(body).isNotNull();
    assertThat(body.message()).isEqualTo("Landed cost posted");
    assertThat(body.data()).isEqualTo(expected);
    verify(service).recordLandedCost(request);
  }

  @Test
  void revalueInventory_delegates() {
    InventoryAccountingService service = mock(InventoryAccountingService.class);
    InventoryAccountingController controller = new InventoryAccountingController(service);
    InventoryRevaluationRequest request =
        new InventoryRevaluationRequest(
            10L,
            12L,
            new BigDecimal("50.00"),
            "revalue",
            LocalDate.of(2026, 3, 31),
            "REV-11",
            null,
            false);
    JournalEntryDto expected = journal(11L, "REV-11");
    when(service.revalueInventory(request)).thenReturn(expected);

    ApiResponse<JournalEntryDto> body = controller.revalueInventory(request).getBody();

    assertThat(body).isNotNull();
    assertThat(body.message()).isEqualTo("Inventory revaluation posted");
    assertThat(body.data()).isEqualTo(expected);
    verify(service).revalueInventory(request);
  }

  @Test
  void adjustWip_delegates() {
    InventoryAccountingService service = mock(InventoryAccountingService.class);
    InventoryAccountingController controller = new InventoryAccountingController(service);
    WipAdjustmentRequest request =
        new WipAdjustmentRequest(
            55L,
            new BigDecimal("75.00"),
            13L,
            14L,
            WipAdjustmentRequest.Direction.ISSUE,
            "issue materials",
            LocalDate.of(2026, 3, 31),
            "WIP-12",
            null,
            false);
    JournalEntryDto expected = journal(12L, "WIP-12");
    when(service.adjustWip(request)).thenReturn(expected);

    ApiResponse<JournalEntryDto> body = controller.adjustWip(request).getBody();

    assertThat(body).isNotNull();
    assertThat(body.message()).isEqualTo("WIP adjustment posted");
    assertThat(body.data()).isEqualTo(expected);
    verify(service).adjustWip(request);
  }

  private JournalEntryDto journal(Long id, String referenceNumber) {
    return new JournalEntryDto(
        id,
        null,
        referenceNumber,
        LocalDate.of(2026, 3, 31),
        "memo",
        "POSTED",
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        List.<JournalLineDto>of(),
        null,
        null,
        null,
        null,
        null,
        null);
  }
}
