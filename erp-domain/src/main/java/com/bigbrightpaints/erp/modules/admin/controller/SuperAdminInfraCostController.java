package com.bigbrightpaints.erp.modules.admin.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.bigbrightpaints.erp.core.security.PortalRoleActionMatrix;
import com.bigbrightpaints.erp.modules.admin.dto.SuperAdminInfraCostDto;
import com.bigbrightpaints.erp.modules.admin.service.SuperAdminInfraCostService;
import com.bigbrightpaints.erp.shared.dto.ApiResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/superadmin/infra/costs")
@PreAuthorize(PortalRoleActionMatrix.SUPER_ADMIN_ONLY)
public class SuperAdminInfraCostController {

  private final SuperAdminInfraCostService infraCostService;

  public SuperAdminInfraCostController(SuperAdminInfraCostService infraCostService) {
    this.infraCostService = infraCostService;
  }

  @GetMapping
  public ResponseEntity<ApiResponse<SuperAdminInfraCostDto.Dashboard>> getDashboard(
      @RequestParam(value = "currency", required = false) String currency) {
    return ResponseEntity.ok(
        ApiResponse.success(
            "Super Admin infra costs fetched", infraCostService.dashboard(currency)));
  }

  @GetMapping("/snapshots")
  public ResponseEntity<ApiResponse<List<SuperAdminInfraCostDto.SnapshotResponse>>> listSnapshots(
      @RequestParam(value = "currency", required = false) String currency,
      @RequestParam(value = "includeArchived", defaultValue = "false") boolean includeArchived) {
    return ResponseEntity.ok(
        ApiResponse.success(
            "Infra cost snapshots fetched",
            infraCostService.listSnapshots(currency, includeArchived)));
  }

  @PostMapping("/snapshots")
  public ResponseEntity<ApiResponse<SuperAdminInfraCostDto.SnapshotResponse>> createSnapshot(
      @Valid @RequestBody SuperAdminInfraCostDto.SnapshotRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            ApiResponse.success(
                "Infra cost snapshot created", infraCostService.createSnapshot(request)));
  }

  @PutMapping("/snapshots/{snapshotId}")
  public ResponseEntity<ApiResponse<SuperAdminInfraCostDto.SnapshotResponse>> correctSnapshot(
      @PathVariable Long snapshotId,
      @Valid @RequestBody SuperAdminInfraCostDto.SnapshotRequest request) {
    return ResponseEntity.ok(
        ApiResponse.success(
            "Infra cost snapshot corrected",
            infraCostService.correctSnapshot(snapshotId, request)));
  }

  @GetMapping("/snapshots/{snapshotId}/corrections")
  public ResponseEntity<ApiResponse<List<SuperAdminInfraCostDto.CorrectionResponse>>> corrections(
      @PathVariable Long snapshotId) {
    return ResponseEntity.ok(
        ApiResponse.success(
            "Infra cost snapshot corrections fetched", infraCostService.corrections(snapshotId)));
  }

  @PostMapping("/snapshots/{snapshotId}/archive")
  public ResponseEntity<ApiResponse<SuperAdminInfraCostDto.SnapshotResponse>> archiveSnapshot(
      @PathVariable Long snapshotId,
      @Valid @RequestBody SuperAdminInfraCostDto.ArchiveRequest request) {
    return ResponseEntity.ok(
        ApiResponse.success(
            "Infra cost snapshot archived", infraCostService.archiveSnapshot(snapshotId, request)));
  }
}
