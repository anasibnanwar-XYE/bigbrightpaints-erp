package com.bigbrightpaints.erp.modules.accounting.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

public class GstReturnDto {
  private YearMonth period;
  private LocalDate periodStart;
  private LocalDate periodEnd;
  private BigDecimal outputTax = BigDecimal.ZERO;
  private BigDecimal inputTax = BigDecimal.ZERO;
  private BigDecimal netPayable = BigDecimal.ZERO;

  public YearMonth getPeriod() {
    return period;
  }

  public void setPeriod(YearMonth period) {
    this.period = period;
  }

  public LocalDate getPeriodStart() {
    return periodStart;
  }

  public void setPeriodStart(LocalDate periodStart) {
    this.periodStart = periodStart;
  }

  public LocalDate getPeriodEnd() {
    return periodEnd;
  }

  public void setPeriodEnd(LocalDate periodEnd) {
    this.periodEnd = periodEnd;
  }

  public BigDecimal getOutputTax() {
    return outputTax;
  }

  public void setOutputTax(BigDecimal outputTax) {
    this.outputTax = outputTax;
  }

  public BigDecimal getInputTax() {
    return inputTax;
  }

  public void setInputTax(BigDecimal inputTax) {
    this.inputTax = inputTax;
  }

  public BigDecimal getNetPayable() {
    return netPayable;
  }

  public void setNetPayable(BigDecimal netPayable) {
    this.netPayable = netPayable;
  }
}
