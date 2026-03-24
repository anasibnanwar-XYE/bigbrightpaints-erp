package com.bigbrightpaints.erp.modules.purchasing.dto;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record PurchaseOrderRequest(
    @NotNull Long supplierId,
    @NotBlank String orderNumber,
    @NotNull LocalDate orderDate,
    String memo,
    @NotEmpty List<@Valid PurchaseOrderLineRequest> lines) {}
