package com.bigbrightpaints.erp.modules.accounting.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Response from payroll batch payment processing.
 * Contains breakdown of gross, deductions, net pay, and liabilities.
 */
public record PayrollBatchPaymentResponse(
    Long payrollRunId,
    LocalDate runDate,
    // Amount totals
    BigDecimal grossAmount, // Total gross wages
    BigDecimal totalTaxWithholding, // Employee tax withholding
    BigDecimal totalPfWithholding, // Employee PF withholding
    BigDecimal totalAdvances, // Advance deductions
    BigDecimal netPayAmount, // Cash paid out
    // Employer contributions
    BigDecimal employerTaxAmount, // Employer tax contribution
    BigDecimal employerPfAmount, // Employer PF contribution
    BigDecimal totalEmployerCost, // Gross + employer contributions
    // Journal entries created
    Long payrollJournalId, // Main payroll entry
    Long employerContribJournalId, // Employer contribution entry (may be null)
    List<LineTotal> lines) {
  public record LineTotal(
      String name,
      Integer days,
      BigDecimal dailyWage,
      BigDecimal grossPay,
      BigDecimal taxWithholding,
      BigDecimal pfWithholding,
      BigDecimal advances,
      BigDecimal netPay,
      String notes) {}
}
