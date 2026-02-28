package com.bigbrightpaints.erp.modules.production.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CatalogProductCartonSizeRequest(
        @NotBlank(message = "Carton size label is required")
        String size,
        @NotNull(message = "Pieces per carton is required")
        @Positive(message = "Pieces per carton must be greater than zero")
        Integer piecesPerCarton
) {
}
