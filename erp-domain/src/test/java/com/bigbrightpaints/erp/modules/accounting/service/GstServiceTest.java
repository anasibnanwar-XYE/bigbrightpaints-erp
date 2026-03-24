package com.bigbrightpaints.erp.modules.accounting.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.bigbrightpaints.erp.core.exception.ApplicationException;

class GstServiceTest {

  private final GstService gstService = new GstService();

  @Test
  void calculateGst_intraState_splitsToCgstAndSgst() {
    GstService.GstBreakdown breakdown =
        gstService.calculateGst(new BigDecimal("1000.00"), "27", "27", new BigDecimal("18.00"));

    assertThat(breakdown.taxableAmount()).isEqualByComparingTo("1000.00");
    assertThat(breakdown.cgst()).isEqualByComparingTo("90.00");
    assertThat(breakdown.sgst()).isEqualByComparingTo("90.00");
    assertThat(breakdown.igst()).isEqualByComparingTo("0.00");
    assertThat(breakdown.totalTax()).isEqualByComparingTo("180.00");
    assertThat(breakdown.taxType()).isEqualTo(GstService.TaxType.INTRA_STATE);
  }

  @Test
  void calculateGst_interState_assignsIgstOnly() {
    GstService.GstBreakdown breakdown =
        gstService.calculateGst(new BigDecimal("1000.00"), "27", "29", new BigDecimal("18.00"));

    assertThat(breakdown.taxableAmount()).isEqualByComparingTo("1000.00");
    assertThat(breakdown.cgst()).isEqualByComparingTo("0.00");
    assertThat(breakdown.sgst()).isEqualByComparingTo("0.00");
    assertThat(breakdown.igst()).isEqualByComparingTo("180.00");
    assertThat(breakdown.totalTax()).isEqualByComparingTo("180.00");
    assertThat(breakdown.taxType()).isEqualTo(GstService.TaxType.INTER_STATE);
  }

  @Test
  void splitTaxAmount_requiresStateCodesWhenTaxable() {
    assertThatThrownBy(
            () ->
                gstService.splitTaxAmount(
                    new BigDecimal("1000.00"), new BigDecimal("180.00"), null, "29"))
        .isInstanceOf(ApplicationException.class)
        .hasMessageContaining("State codes are required");
  }
}
