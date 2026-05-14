package com.bigbrightpaints.erp.truthsuite.o2c;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.bigbrightpaints.erp.truthsuite.support.TruthSuiteFileAssert;

@Tag("critical")
class TS_PackagingSlipInvoiceLinkV2MigrationContractTest {

  private static final String V2_MIGRATION =
      "src/main/resources/db/migration_v2/V177__backfill_packaging_slip_invoice_links.sql";
  private static final String COGS_MIGRATION =
      "src/main/resources/db/migration_v2/V206__backfill_packaging_slip_cogs_journal_links.sql";
  private static final String SALES_ORDER_STATUS_MIGRATION =
      "src/main/resources/db/migration_v2/V207__canonicalize_sales_order_statuses.sql";

  @Test
  void packagingSlipInvoiceBackfillLivesOnCanonicalFlywayV2Track() {
    assertTrue(Files.exists(TruthSuiteFileAssert.resolve(V2_MIGRATION)));
    TruthSuiteFileAssert.assertContains(
        V2_MIGRATION,
        "UPDATE packaging_slips p",
        "SET invoice_id = COALESCE(fulfillment_invoice.id, current_invoices.invoice_id)",
        "AND p.invoice_id IS NULL",
        "RAISE NOTICE 'Explicit packaging slip invoice links available for % rows'");
  }

  @Test
  void packagingSlipCogsBackfillLivesOnCanonicalFlywayV2Track() {
    assertTrue(Files.exists(TruthSuiteFileAssert.resolve(COGS_MIGRATION)));
    TruthSuiteFileAssert.assertContains(
        COGS_MIGRATION,
        "UPDATE public.packaging_slips ps",
        "SET cogs_journal_entry_id = je.id",
        "AND je.reference_number = 'COGS-' || ps.slip_number",
        "WHERE linked.cogs_journal_entry_id = je.id");
  }

  @Test
  void salesOrderStatusHardCutCanonicalizationLivesOnCanonicalFlywayV2Track() {
    assertTrue(Files.exists(TruthSuiteFileAssert.resolve(SALES_ORDER_STATUS_MIGRATION)));
    TruthSuiteFileAssert.assertContains(
        SALES_ORDER_STATUS_MIGRATION,
        "UPDATE public.sales_orders",
        "WHEN 'BOOKED' THEN 'CONFIRMED'",
        "WHEN 'SHIPPED' THEN 'DISPATCHED'",
        "WHEN 'FULFILLED' THEN 'DISPATCHED'",
        "WHEN 'COMPLETED' THEN 'SETTLED'",
        "UPDATE public.sales_order_status_history");
  }
}
