package com.bigbrightpaints.erp.truthsuite.p2p;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.bigbrightpaints.erp.truthsuite.support.TruthSuiteFileAssert;

@Tag("critical")
@Tag("reconciliation")
class TS_P2PGstDecisioningFailClosedTest {

  private static final String PURCHASE_INVOICE_ENGINE =
      "src/main/java/com/bigbrightpaints/erp/modules/purchasing/service/PurchaseInvoiceEngine.java";

  @Test
  void purchaseFlowRejectsMixedManualAndLineTaxSignals() {
    TruthSuiteFileAssert.assertContains(
        PURCHASE_INVOICE_ENGINE,
        "if (taxProvided && (lineRequest.taxRate() != null || lineRequest.taxInclusive() != null))",
        "taxAmount cannot be combined with line-level taxRate or taxInclusive");
  }

  @Test
  void purchaseFlowRejectsTaxInclusiveLinesWithoutPositiveRate() {
    TruthSuiteFileAssert.assertContains(
        PURCHASE_INVOICE_ENGINE,
        "if (Boolean.TRUE.equals(lineRequest.taxInclusive())",
        "Tax-inclusive purchase line requires a positive GST rate");
  }
}
