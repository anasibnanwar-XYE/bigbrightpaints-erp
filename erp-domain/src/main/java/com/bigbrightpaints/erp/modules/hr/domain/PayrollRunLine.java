package com.bigbrightpaints.erp.modules.hr.domain;

import java.math.BigDecimal;

import jakarta.persistence.*;

/**
 * Individual employee line item in a payroll run.
 * Contains calculated pay details for the period.
 */
@Entity
@Table(
    name = "payroll_run_lines",
    indexes = {
      @Index(name = "idx_payroll_line_run", columnList = "payroll_run_id"),
      @Index(name = "idx_payroll_line_employee", columnList = "employee_id")
    })
public class PayrollRunLine {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "payroll_run_id")
  private PayrollRun payrollRun;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "employee_id")
  private Employee employee;

  // Attendance summary for period
  @Column(name = "present_days", precision = 5, scale = 2)
  private BigDecimal presentDays = BigDecimal.ZERO;

  @Column(name = "half_days", precision = 5, scale = 2)
  private BigDecimal halfDays = BigDecimal.ZERO;

  @Column(name = "absent_days", precision = 5, scale = 2)
  private BigDecimal absentDays = BigDecimal.ZERO;

  @Column(name = "leave_days", precision = 5, scale = 2)
  private BigDecimal leaveDays = BigDecimal.ZERO;

  @Column(name = "holiday_days", precision = 5, scale = 2)
  private BigDecimal holidayDays = BigDecimal.ZERO;

  // Hours worked
  @Column(name = "regular_hours", precision = 10, scale = 2)
  private BigDecimal regularHours = BigDecimal.ZERO;

  @Column(name = "overtime_hours", precision = 10, scale = 2)
  private BigDecimal overtimeHours = BigDecimal.ZERO;

  @Column(name = "double_ot_hours", precision = 10, scale = 2)
  private BigDecimal doubleOtHours = BigDecimal.ZERO;

  // Rate information (captured at time of calculation)
  @Column(name = "daily_rate", precision = 19, scale = 2)
  private BigDecimal dailyRate;

  @Column(name = "hourly_rate", precision = 19, scale = 2)
  private BigDecimal hourlyRate;

  @Column(name = "ot_rate_multiplier", precision = 5, scale = 2)
  private BigDecimal otRateMultiplier = new BigDecimal("1.5"); // 1.5x for regular OT

  @Column(name = "double_ot_multiplier", precision = 5, scale = 2)
  private BigDecimal doubleOtMultiplier = new BigDecimal("2.0"); // 2x for holiday OT

  // Pay calculation
  @Column(name = "base_pay", precision = 19, scale = 2)
  private BigDecimal basePay = BigDecimal.ZERO;

  @Column(name = "overtime_pay", precision = 19, scale = 2)
  private BigDecimal overtimePay = BigDecimal.ZERO;

  @Column(name = "holiday_pay", precision = 19, scale = 2)
  private BigDecimal holidayPay = BigDecimal.ZERO;

  @Column(name = "gross_pay", precision = 19, scale = 2)
  private BigDecimal grossPay = BigDecimal.ZERO;

  @Column(name = "basic_salary_component", precision = 19, scale = 2)
  private BigDecimal basicSalaryComponent = BigDecimal.ZERO;

  @Column(name = "hra_component", precision = 19, scale = 2)
  private BigDecimal hraComponent = BigDecimal.ZERO;

  @Column(name = "da_component", precision = 19, scale = 2)
  private BigDecimal daComponent = BigDecimal.ZERO;

  @Column(name = "special_allowance_component", precision = 19, scale = 2)
  private BigDecimal specialAllowanceComponent = BigDecimal.ZERO;

  // Deductions
  @Column(name = "advance_deduction", precision = 19, scale = 2)
  private BigDecimal advanceDeduction = BigDecimal.ZERO;

  @Column(name = "pf_deduction", precision = 19, scale = 2)
  private BigDecimal pfDeduction = BigDecimal.ZERO;

  @Column(name = "tax_deduction", precision = 19, scale = 2)
  private BigDecimal taxDeduction = BigDecimal.ZERO;

  @Column(name = "esi_deduction", precision = 19, scale = 2)
  private BigDecimal esiDeduction = BigDecimal.ZERO;

  @Column(name = "professional_tax_deduction", precision = 19, scale = 2)
  private BigDecimal professionalTaxDeduction = BigDecimal.ZERO;

  @Column(name = "loan_deduction", precision = 19, scale = 2)
  private BigDecimal loanDeduction = BigDecimal.ZERO;

  @Column(name = "leave_without_pay_deduction", precision = 19, scale = 2)
  private BigDecimal leaveWithoutPayDeduction = BigDecimal.ZERO;

  @Column(name = "other_deductions", precision = 19, scale = 2)
  private BigDecimal otherDeductions = BigDecimal.ZERO;

  @Column(name = "total_deductions", precision = 19, scale = 2)
  private BigDecimal totalDeductions = BigDecimal.ZERO;

  // Net pay
  @Column(name = "net_pay", precision = 19, scale = 2)
  private BigDecimal netPay = BigDecimal.ZERO;

  // Payment status
  @Enumerated(EnumType.STRING)
  @Column(name = "payment_status")
  private PaymentStatus paymentStatus = PaymentStatus.PENDING;

  @Column(name = "payment_reference")
  private String paymentReference;

  private String remarks;

  // Backward compatibility columns (mapped to DB)
  @Column(name = "name")
  private String name;

  @Column(name = "days_worked")
  private Integer daysWorked;

  @Column(name = "daily_wage", precision = 19, scale = 2)
  private BigDecimal dailyWage;

  @Column(name = "advances", precision = 19, scale = 2)
  private BigDecimal advances;

  @Column(name = "line_total", precision = 19, scale = 2)
  private BigDecimal lineTotal;

  @Column(name = "notes")
  private String notes;

  // Getters and Setters
  public Long getId() {
    return id;
  }

  public PayrollRun getPayrollRun() {
    return payrollRun;
  }

  public void setPayrollRun(PayrollRun payrollRun) {
    this.payrollRun = payrollRun;
  }

  public Employee getEmployee() {
    return employee;
  }

  public void setEmployee(Employee employee) {
    this.employee = employee;
  }

  public BigDecimal getPresentDays() {
    return presentDays;
  }

  public void setPresentDays(BigDecimal presentDays) {
    this.presentDays = presentDays;
  }

  public BigDecimal getHalfDays() {
    return halfDays;
  }

  public void setHalfDays(BigDecimal halfDays) {
    this.halfDays = halfDays;
  }

  public BigDecimal getAbsentDays() {
    return absentDays;
  }

  public void setAbsentDays(BigDecimal absentDays) {
    this.absentDays = absentDays;
  }

  public BigDecimal getLeaveDays() {
    return leaveDays;
  }

  public void setLeaveDays(BigDecimal leaveDays) {
    this.leaveDays = leaveDays;
  }

  public BigDecimal getHolidayDays() {
    return holidayDays;
  }

  public void setHolidayDays(BigDecimal holidayDays) {
    this.holidayDays = holidayDays;
  }

  public BigDecimal getRegularHours() {
    return regularHours;
  }

  public void setRegularHours(BigDecimal regularHours) {
    this.regularHours = regularHours;
  }

  public BigDecimal getOvertimeHours() {
    return overtimeHours;
  }

  public void setOvertimeHours(BigDecimal overtimeHours) {
    this.overtimeHours = overtimeHours;
  }

  public BigDecimal getDoubleOtHours() {
    return doubleOtHours;
  }

  public void setDoubleOtHours(BigDecimal doubleOtHours) {
    this.doubleOtHours = doubleOtHours;
  }

  public BigDecimal getDailyRate() {
    return dailyRate;
  }

  public void setDailyRate(BigDecimal dailyRate) {
    this.dailyRate = dailyRate;
  }

  public BigDecimal getHourlyRate() {
    return hourlyRate;
  }

  public void setHourlyRate(BigDecimal hourlyRate) {
    this.hourlyRate = hourlyRate;
  }

  public BigDecimal getOtRateMultiplier() {
    return otRateMultiplier;
  }

  public void setOtRateMultiplier(BigDecimal otRateMultiplier) {
    this.otRateMultiplier = otRateMultiplier;
  }

  public BigDecimal getDoubleOtMultiplier() {
    return doubleOtMultiplier;
  }

  public void setDoubleOtMultiplier(BigDecimal doubleOtMultiplier) {
    this.doubleOtMultiplier = doubleOtMultiplier;
  }

  public BigDecimal getBasePay() {
    return basePay;
  }

  public void setBasePay(BigDecimal basePay) {
    this.basePay = basePay;
  }

  public BigDecimal getOvertimePay() {
    return overtimePay;
  }

  public void setOvertimePay(BigDecimal overtimePay) {
    this.overtimePay = overtimePay;
  }

  public BigDecimal getHolidayPay() {
    return holidayPay;
  }

  public void setHolidayPay(BigDecimal holidayPay) {
    this.holidayPay = holidayPay;
  }

  public BigDecimal getGrossPay() {
    return grossPay;
  }

  public void setGrossPay(BigDecimal grossPay) {
    this.grossPay = grossPay;
  }

  public BigDecimal getBasicSalaryComponent() {
    return basicSalaryComponent;
  }

  public void setBasicSalaryComponent(BigDecimal basicSalaryComponent) {
    this.basicSalaryComponent = basicSalaryComponent;
  }

  public BigDecimal getHraComponent() {
    return hraComponent;
  }

  public void setHraComponent(BigDecimal hraComponent) {
    this.hraComponent = hraComponent;
  }

  public BigDecimal getDaComponent() {
    return daComponent;
  }

  public void setDaComponent(BigDecimal daComponent) {
    this.daComponent = daComponent;
  }

  public BigDecimal getSpecialAllowanceComponent() {
    return specialAllowanceComponent;
  }

  public void setSpecialAllowanceComponent(BigDecimal specialAllowanceComponent) {
    this.specialAllowanceComponent = specialAllowanceComponent;
  }

  public BigDecimal getAdvanceDeduction() {
    return advanceDeduction;
  }

  public void setAdvanceDeduction(BigDecimal advanceDeduction) {
    this.advanceDeduction = advanceDeduction;
  }

  public BigDecimal getPfDeduction() {
    return pfDeduction;
  }

  public void setPfDeduction(BigDecimal pfDeduction) {
    this.pfDeduction = pfDeduction;
  }

  public BigDecimal getTaxDeduction() {
    return taxDeduction;
  }

  public void setTaxDeduction(BigDecimal taxDeduction) {
    this.taxDeduction = taxDeduction;
  }

  public BigDecimal getEsiDeduction() {
    return esiDeduction;
  }

  public void setEsiDeduction(BigDecimal esiDeduction) {
    this.esiDeduction = esiDeduction;
  }

  public BigDecimal getProfessionalTaxDeduction() {
    return professionalTaxDeduction;
  }

  public void setProfessionalTaxDeduction(BigDecimal professionalTaxDeduction) {
    this.professionalTaxDeduction = professionalTaxDeduction;
  }

  public BigDecimal getLoanDeduction() {
    return loanDeduction;
  }

  public void setLoanDeduction(BigDecimal loanDeduction) {
    this.loanDeduction = loanDeduction;
  }

  public BigDecimal getLeaveWithoutPayDeduction() {
    return leaveWithoutPayDeduction;
  }

  public void setLeaveWithoutPayDeduction(BigDecimal leaveWithoutPayDeduction) {
    this.leaveWithoutPayDeduction = leaveWithoutPayDeduction;
  }

  public BigDecimal getOtherDeductions() {
    return otherDeductions;
  }

  public void setOtherDeductions(BigDecimal otherDeductions) {
    this.otherDeductions = otherDeductions;
  }

  public BigDecimal getTotalDeductions() {
    return totalDeductions;
  }

  public void setTotalDeductions(BigDecimal totalDeductions) {
    this.totalDeductions = totalDeductions;
  }

  public BigDecimal getNetPay() {
    return netPay;
  }

  public void setNetPay(BigDecimal netPay) {
    this.netPay = netPay;
  }

  public PaymentStatus getPaymentStatus() {
    return paymentStatus;
  }

  public void setPaymentStatus(PaymentStatus paymentStatus) {
    this.paymentStatus = paymentStatus;
  }

  public String getPaymentReference() {
    return paymentReference;
  }

  public void setPaymentReference(String paymentReference) {
    this.paymentReference = paymentReference;
  }

  public String getRemarks() {
    return remarks;
  }

  public void setRemarks(String remarks) {
    this.remarks = remarks;
  }

  // Backward compatibility getters/setters
  public String getName() {
    if (name != null) return name;
    return employee != null ? employee.getFullName() : null;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Integer getDaysWorked() {
    if (daysWorked != null) return daysWorked;
    return presentDays != null ? presentDays.intValue() : 0;
  }

  public void setDaysWorked(Integer daysWorked) {
    this.daysWorked = daysWorked;
    if (daysWorked != null) {
      this.presentDays = new java.math.BigDecimal(daysWorked);
    }
  }

  public java.math.BigDecimal getDailyWage() {
    return dailyWage != null ? dailyWage : dailyRate;
  }

  public void setDailyWage(java.math.BigDecimal dailyWage) {
    this.dailyWage = dailyWage;
    this.dailyRate = dailyWage;
  }

  public java.math.BigDecimal getAdvances() {
    return advances != null ? advances : advanceDeduction;
  }

  public void setAdvances(java.math.BigDecimal advances) {
    this.advances = advances;
    this.advanceDeduction = advances;
  }

  public java.math.BigDecimal getLineTotal() {
    return lineTotal != null ? lineTotal : netPay;
  }

  public void setLineTotal(java.math.BigDecimal lineTotal) {
    this.lineTotal = lineTotal;
    this.netPay = lineTotal;
  }

  public String getNotes() {
    return notes != null ? notes : remarks;
  }

  public void setNotes(String notes) {
    this.notes = notes;
    this.remarks = notes;
  }

  public enum PaymentStatus {
    PENDING, // Not yet paid
    PROCESSING, // Payment in progress
    PAID, // Payment complete
    FAILED, // Payment failed
    CANCELLED // Cancelled
  }
}
