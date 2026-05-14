package com.bigbrightpaints.erp.truthsuite.p2p;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.bigbrightpaints.erp.truthsuite.support.TruthSuiteFileAssert;

@Tag("critical")
@Tag("reconciliation")
class TS_P2PPurchaseJournalLinkageTest {

  private static final String PURCHASE_INVOICE_ENGINE =
      "src/main/java/com/bigbrightpaints/erp/modules/purchasing/service/PurchaseInvoiceEngine.java";
  private static final String PURCHASE_TAX_POLICY =
      "src/main/java/com/bigbrightpaints/erp/modules/purchasing/service/PurchaseTaxPolicy.java";

  @Test
  void purchaseInvoicePostsJournalBeforePersistence() {
    TruthSuiteFileAssert.assertContainsInOrder(
        PURCHASE_INVOICE_ENGINE,
        "JournalEntryDto entry =",
        "postPurchaseEntry(",
        "request,",
        "supplier,",
        "inventoryDebits,",
        "taxAmount,",
        "totalAmount,",
        "referenceNumber,",
        "gstBreakdown);",
        "purchase.setJournalEntry(linkedJournal);",
        "purchase = purchaseRepository.save(purchase);");
  }

  @Test
  void purchaseFlowLinksInventoryMovementsAndClosesGrn() {
    TruthSuiteFileAssert.assertContains(
        PURCHASE_INVOICE_ENGINE,
        "movement.setJournalEntryId(journalEntryId);",
        "goodsReceipt.setStatus(GoodsReceiptStatus.INVOICED);",
        "goodsReceiptRepository.save(goodsReceipt);",
        "PurchaseOrderStatus.INVOICED");
  }

  @Test
  void purchaseTaxComputationUsesDeterministicHalfUpRounding() {
    TruthSuiteFileAssert.assertContains(
        PURCHASE_INVOICE_ENGINE,
        "lineTax = currency(lineNet.multiply(effectiveTaxRate)",
        ".divide(new BigDecimal(\"100\"), 6, RoundingMode.HALF_UP));",
        "BigDecimal allocatedTax = (i == computedLines.size() - 1)",
        ".divide(inventoryTotal, 6, RoundingMode.HALF_UP));");
  }

  @Test
  void purchaseFlowEnforcesSingleTaxModeContractForDownstreamSettlement() {
    TruthSuiteFileAssert.assertContains(
        PURCHASE_INVOICE_ENGINE,
        "purchaseTaxPolicy.resolvePurchaseTaxMode(sortedLines, lockedMaterials);",
        "purchaseTaxPolicy.resolveLineTaxRateForMode(",
        "purchaseTaxPolicy.enforcePurchaseTaxContract(");
    TruthSuiteFileAssert.assertContains(
        PURCHASE_TAX_POLICY, "\"Purchase invoice cannot mix GST and non-GST materials\"");
  }
}
