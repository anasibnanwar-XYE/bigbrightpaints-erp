package com.bigbrightpaints.erp.core.util;

import com.bigbrightpaints.erp.modules.sales.domain.SalesOrder;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BusinessDocumentTruthsTest {

    @Test
    void salesOrderLifecycle_staysNotEligibleBeforeInvoiceOrJournalTruthExists() {
        SalesOrder order = new SalesOrder();
        order.setStatus("READY_TO_SHIP");

        assertThat(BusinessDocumentTruths.salesOrderLifecycle(order).workflowStatus()).isEqualTo("READY_TO_SHIP");
        assertThat(BusinessDocumentTruths.salesOrderLifecycle(order).accountingStatus()).isEqualTo("NOT_ELIGIBLE");
    }

    @Test
    void salesOrderLifecycle_becomesPendingWhenInvoiceTruthExistsWithoutJournal() {
        SalesOrder order = new SalesOrder();
        order.setStatus("READY_TO_SHIP");
        order.setFulfillmentInvoiceId(123L);

        assertThat(BusinessDocumentTruths.salesOrderLifecycle(order).accountingStatus()).isEqualTo("PENDING");
    }

    @Test
    void salesOrderLifecycle_becomesPostedWhenSalesJournalTruthExists() {
        SalesOrder order = new SalesOrder();
        order.setStatus("INVOICED");
        order.setSalesJournalEntryId(456L);

        assertThat(BusinessDocumentTruths.salesOrderLifecycle(order).accountingStatus()).isEqualTo("POSTED");
    }
}
