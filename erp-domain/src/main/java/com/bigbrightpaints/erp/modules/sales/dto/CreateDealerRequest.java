package com.bigbrightpaints.erp.modules.sales.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record CreateDealerRequest(
        @NotBlank String name,
        @NotBlank String companyName,
        @Email(message = "Provide a valid contact email")
        @NotBlank String contactEmail,
        @NotBlank String contactPhone,
        String address,
        @PositiveOrZero BigDecimal creditLimit
) {}
