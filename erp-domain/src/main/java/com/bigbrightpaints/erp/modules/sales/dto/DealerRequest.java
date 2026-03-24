package com.bigbrightpaints.erp.modules.sales.dto;

import java.math.BigDecimal;

import com.bigbrightpaints.erp.modules.accounting.domain.GstRegistrationType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record DealerRequest(
    @NotBlank String name,
    @NotBlank String code,
    String email,
    String phone,
    @NotNull BigDecimal creditLimit,
    @Pattern(
            regexp = "^$|[0-9]{2}[A-Za-z0-9]{13}$",
            message = "GST number must be a valid 15-character GSTIN")
        String gstNumber,
    @Pattern(regexp = "^$|[A-Za-z0-9]{2}$", message = "State code must be exactly 2 characters")
        String stateCode,
    GstRegistrationType gstRegistrationType) {

  public DealerRequest(
      String name, String code, String email, String phone, BigDecimal creditLimit) {
    this(name, code, email, phone, creditLimit, null, null, null);
  }
}
