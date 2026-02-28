package com.bigbrightpaints.erp.modules.accounting.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

public class GstReconciliationDto {

    private YearMonth period;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private GstComponentSummary collected = new GstComponentSummary();
    private GstComponentSummary inputTaxCredit = new GstComponentSummary();
    private GstComponentSummary netLiability = new GstComponentSummary();

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

    public GstComponentSummary getCollected() {
        return collected;
    }

    public void setCollected(GstComponentSummary collected) {
        this.collected = collected;
    }

    public GstComponentSummary getInputTaxCredit() {
        return inputTaxCredit;
    }

    public void setInputTaxCredit(GstComponentSummary inputTaxCredit) {
        this.inputTaxCredit = inputTaxCredit;
    }

    public GstComponentSummary getNetLiability() {
        return netLiability;
    }

    public void setNetLiability(GstComponentSummary netLiability) {
        this.netLiability = netLiability;
    }

    public static class GstComponentSummary {
        private BigDecimal cgst = BigDecimal.ZERO;
        private BigDecimal sgst = BigDecimal.ZERO;
        private BigDecimal igst = BigDecimal.ZERO;
        private BigDecimal total = BigDecimal.ZERO;

        public GstComponentSummary() {
        }

        public GstComponentSummary(BigDecimal cgst, BigDecimal sgst, BigDecimal igst, BigDecimal total) {
            this.cgst = cgst;
            this.sgst = sgst;
            this.igst = igst;
            this.total = total;
        }

        public BigDecimal getCgst() {
            return cgst;
        }

        public void setCgst(BigDecimal cgst) {
            this.cgst = cgst;
        }

        public BigDecimal getSgst() {
            return sgst;
        }

        public void setSgst(BigDecimal sgst) {
            this.sgst = sgst;
        }

        public BigDecimal getIgst() {
            return igst;
        }

        public void setIgst(BigDecimal igst) {
            this.igst = igst;
        }

        public BigDecimal getTotal() {
            return total;
        }

        public void setTotal(BigDecimal total) {
            this.total = total;
        }
    }
}
