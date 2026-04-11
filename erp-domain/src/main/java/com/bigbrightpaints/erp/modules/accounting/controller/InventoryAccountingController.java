package com.bigbrightpaints.erp.modules.accounting.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bigbrightpaints.erp.modules.accounting.dto.InventoryRevaluationRequest;
import com.bigbrightpaints.erp.modules.accounting.dto.JournalEntryDto;
import com.bigbrightpaints.erp.modules.accounting.dto.LandedCostRequest;
import com.bigbrightpaints.erp.modules.accounting.dto.WipAdjustmentRequest;
import com.bigbrightpaints.erp.modules.accounting.service.InventoryAccountingService;
import com.bigbrightpaints.erp.shared.dto.ApiResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/accounting")
public class InventoryAccountingController {

  private final InventoryAccountingService inventoryAccountingService;

  public InventoryAccountingController(InventoryAccountingService inventoryAccountingService) {
    this.inventoryAccountingService = inventoryAccountingService;
  }

  @PostMapping("/inventory/landed-cost")
  @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')")
  public ResponseEntity<ApiResponse<JournalEntryDto>> recordLandedCost(
      @Valid @RequestBody LandedCostRequest request) {
    return ResponseEntity.ok(
        ApiResponse.success(
            "Landed cost posted", inventoryAccountingService.recordLandedCost(request)));
  }

  @PostMapping("/inventory/revaluation")
  @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')")
  public ResponseEntity<ApiResponse<JournalEntryDto>> revalueInventory(
      @Valid @RequestBody InventoryRevaluationRequest request) {
    return ResponseEntity.ok(
        ApiResponse.success(
            "Inventory revaluation posted", inventoryAccountingService.revalueInventory(request)));
  }

  @PostMapping("/inventory/wip-adjustment")
  @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')")
  public ResponseEntity<ApiResponse<JournalEntryDto>> adjustWip(
      @Valid @RequestBody WipAdjustmentRequest request) {
    return ResponseEntity.ok(
        ApiResponse.success(
            "WIP adjustment posted", inventoryAccountingService.adjustWip(request)));
  }
}
