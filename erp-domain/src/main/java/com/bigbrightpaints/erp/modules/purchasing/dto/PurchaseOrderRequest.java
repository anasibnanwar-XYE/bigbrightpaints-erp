package com.bigbrightpaints.erp.modules.purchasing.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record PurchaseOrderRequest(
        @NotNull Long supplierId,
        @NotBlank String orderNumber,
        @NotNull LocalDate orderDate,
        String memo,
        @NotEmpty List<@Valid PurchaseOrderLineRequest> lines
) {}
