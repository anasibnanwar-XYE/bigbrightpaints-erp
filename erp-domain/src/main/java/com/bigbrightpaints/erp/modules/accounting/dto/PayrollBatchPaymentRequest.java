package com.bigbrightpaints.erp.modules.accounting.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/**
 * Request for processing payroll batch payment with proper accounting entries.
 *
 * Creates multiple journal entries:
 * 1. Payroll Expense Entry:
 *    Dr. Payroll Expense (gross wages)
 *    Cr. Cash (net pay)
 *    Cr. Tax Payable (employee tax withholding)
 *    Cr. PF/Pension Payable (employee contribution)
 *
 * 2. Employer Contribution Entry (if employer rates provided):
 *    Dr. Employer Tax Expense
 *    Cr. Tax Payable (employer portion)
 *    Dr. Employer PF Expense
 *    Cr. PF Payable (employer portion)
 */
public record PayrollBatchPaymentRequest(
    @NotNull @JsonFormat(pattern = "yyyy-MM-dd") LocalDate runDate,
    @NotNull Long cashAccountId,
    @NotNull Long expenseAccountId,
    // Liability accounts for withholdings
    Long taxPayableAccountId, // For TDS/income tax withholding
    Long pfPayableAccountId, // For PF/pension withholding
    // Employer contribution expense accounts
    Long employerTaxExpenseAccountId,
    Long employerPfExpenseAccountId,
    // Default rates (can be overridden per line)
    @DecimalMin("0.00") BigDecimal defaultTaxRate, // e.g., 0.10 for 10%
    @DecimalMin("0.00") BigDecimal defaultPfRate, // e.g., 0.12 for 12%
    @DecimalMin("0.00") BigDecimal employerTaxRate, // Employer tax contribution rate
    @DecimalMin("0.00") BigDecimal employerPfRate, // Employer PF contribution rate
    String referenceNumber,
    String memo,
    @NotEmpty List<@Valid PayrollLine> lines) {
  public record PayrollLine(
      @NotNull String name,
      @NotNull Integer days,
      @NotNull @DecimalMin("0.00") BigDecimal dailyWage,
      @DecimalMin("0.00") BigDecimal advances,
      // Per-line tax/PF amounts (overrides default rates if provided)
      @DecimalMin("0.00") BigDecimal taxWithholding, // Fixed tax amount
      @DecimalMin("0.00") BigDecimal pfWithholding, // Fixed PF amount
      String notes) {}
}
