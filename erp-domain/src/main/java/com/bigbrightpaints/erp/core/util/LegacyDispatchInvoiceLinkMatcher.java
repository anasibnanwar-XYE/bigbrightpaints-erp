package com.bigbrightpaints.erp.core.util;

import com.bigbrightpaints.erp.modules.invoice.domain.Invoice;
import com.bigbrightpaints.erp.modules.inventory.domain.PackagingSlip;

import java.util.List;
import java.util.Objects;

public final class LegacyDispatchInvoiceLinkMatcher {

    private LegacyDispatchInvoiceLinkMatcher() {
    }

    public static boolean isSlipLinkedToInvoice(PackagingSlip slip,
                                                Invoice invoice,
                                                List<PackagingSlip> candidateSlips,
                                                int salesOrderInvoiceCount) {
        if (slip == null || invoice == null) {
            return false;
        }
        boolean hasExplicitInvoiceLinks = candidateSlips != null
                && candidateSlips.stream().anyMatch(candidate -> candidate != null && candidate.getInvoiceId() != null);
        if (hasExplicitInvoiceLinks) {
            return slip.getInvoiceId() != null
                    && invoice.getId() != null
                    && slip.getInvoiceId().equals(invoice.getId());
        }
        long candidateCount = candidateSlips == null
                ? 0
                : candidateSlips.stream().filter(Objects::nonNull).count();
        return candidateCount == 1 && salesOrderInvoiceCount == 1;
    }
}
