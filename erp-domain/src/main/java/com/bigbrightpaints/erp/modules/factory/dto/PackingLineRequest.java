package com.bigbrightpaints.erp.modules.factory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record PackingLineRequest(
        @NotBlank(message = "Packaging size is required")
        String packagingSize,
        @Positive(message = "Quantity must be positive")
        BigDecimal quantityLiters,
        @Positive(message = "Pieces count must be positive")
        Integer piecesCount,
        @Positive(message = "Boxes count must be positive")
        Integer boxesCount,
        @Positive(message = "Pieces per box must be positive")
        Integer piecesPerBox
) {}
