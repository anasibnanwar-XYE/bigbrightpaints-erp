package com.bigbrightpaints.erp.modules.inventory.dto;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.constraints.NotNull;

public record DispatchConfirmationRequest(
    @NotNull Long packagingSlipId,
    @NotNull List<LineConfirmation> lines,
    String notes,
    String confirmedBy,
    Long overrideRequestId,
    String transporterName,
    String driverName,
    String vehicleNumber,
    String challanReference) {

  public DispatchConfirmationRequest(
      Long packagingSlipId,
      List<LineConfirmation> lines,
      String notes,
      String confirmedBy,
      Long overrideRequestId) {
    this(packagingSlipId, lines, notes, confirmedBy, overrideRequestId, null, null, null, null);
  }

  public record LineConfirmation(
      @NotNull Long lineId, @NotNull BigDecimal shippedQuantity, String notes) {}
}
