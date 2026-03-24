package com.bigbrightpaints.erp.modules.purchasing.dto;

import java.math.BigDecimal;

import com.bigbrightpaints.erp.modules.accounting.domain.GstRegistrationType;
import com.bigbrightpaints.erp.modules.purchasing.domain.SupplierPaymentTerms;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SupplierRequest(
    @NotBlank @Size(max = 64) String name,
    @Size(max = 64) String code,
    @Email String contactEmail,
    @Size(max = 32) String contactPhone,
    @Size(max = 512) String address,
    @DecimalMin(value = "0.00") BigDecimal creditLimit,
    @Pattern(
            regexp = "^$|[0-9]{2}[A-Za-z0-9]{13}$",
            message = "GST number must be a valid 15-character GSTIN")
        String gstNumber,
    @Pattern(regexp = "^$|[A-Za-z0-9]{2}$", message = "State code must be exactly 2 characters")
        String stateCode,
    GstRegistrationType gstRegistrationType,
    SupplierPaymentTerms paymentTerms,
    @Size(max = 128) String bankAccountName,
    @Size(max = 64) String bankAccountNumber,
    @Size(max = 32) String bankIfsc,
    @Size(max = 128) String bankBranch) {

  public SupplierRequest(
      String name,
      String code,
      String contactEmail,
      String contactPhone,
      String address,
      BigDecimal creditLimit) {
    this(
        name,
        code,
        contactEmail,
        contactPhone,
        address,
        creditLimit,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null);
  }
}
