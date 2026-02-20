package com.bigbrightpaints.erp.truthsuite.p2p;

import com.bigbrightpaints.erp.modules.invoice.domain.Invoice;
import com.bigbrightpaints.erp.modules.invoice.service.InvoiceSettlementPolicy;
import com.bigbrightpaints.erp.modules.invoice.service.SettlementApprovalDecision;
import com.bigbrightpaints.erp.modules.invoice.service.SettlementApprovalReasonCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Tag("critical")
@Tag("reconciliation")
class TS_P2PSettlementRuntimeTest {

    private InvoiceSettlementPolicy policy;
    private Invoice invoice;

    @BeforeEach
    void setUp() {
        policy = new InvoiceSettlementPolicy();
        invoice = new Invoice();
        invoice.setStatus(InvoiceSettlementPolicy.InvoiceStatus.DRAFT.name());
        invoice.setTotalAmount(BigDecimal.valueOf(100));
        invoice.setOutstandingAmount(BigDecimal.valueOf(100));
        policy.ensureIssuable(invoice);
    }

    @Test
    void settlementOverrideFailsClosedWhenReferenceMissing() {
        SettlementApprovalDecision approval = approvedOverride("APP-SET-101");
        assertThrows(IllegalArgumentException.class, () ->
                policy.applySettlementWithOverride(
                        invoice,
                        BigDecimal.valueOf(40),
                        BigDecimal.TEN,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        " ",
                        approval));
    }

    @Test
    void settlementOverrideFailsClosedWhenReasonCodeDoesNotMatch() {
        SettlementApprovalDecision wrongReason = new SettlementApprovalDecision(
                "APP-SET-102",
                "maker-a",
                "checker-b",
                SettlementApprovalReasonCode.SUPPLIER_EXCEPTION,
                Instant.parse("2026-02-20T00:00:00Z"),
                Map.of("ticket", "TKT-ERP-STAGE-095", "approvalSource", "workflow"));

        assertThrows(IllegalArgumentException.class, () ->
                policy.applySettlementWithOverride(
                        invoice,
                        BigDecimal.valueOf(40),
                        BigDecimal.TEN,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        "SETTLE-OVERRIDE-102",
                        wrongReason));
    }

    @Test
    void settlementOverrideSucceedsWithValidApproval() {
        policy.applySettlementWithOverride(
                invoice,
                BigDecimal.valueOf(40),
                BigDecimal.TEN,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                "SETTLE-OVERRIDE-103",
                approvedOverride("APP-SET-103"));

        assertEquals(BigDecimal.valueOf(50), invoice.getOutstandingAmount());
        assertEquals(InvoiceSettlementPolicy.InvoiceStatus.PARTIAL.name(), invoice.getStatus());
    }

    @Test
    void supplierExceptionApprovalEnforcesReasonCode() {
        SettlementApprovalDecision supplierException = new SettlementApprovalDecision(
                "APP-SET-104",
                "maker-a",
                "checker-b",
                SettlementApprovalReasonCode.SUPPLIER_EXCEPTION,
                Instant.parse("2026-02-20T00:00:00Z"),
                Map.of("ticket", "TKT-ERP-STAGE-095", "approvalSource", "workflow"));
        SettlementApprovalDecision settlementOverride = approvedOverride("APP-SET-105");

        assertEquals(SettlementApprovalReasonCode.SUPPLIER_EXCEPTION,
                policy.requireSupplierExceptionApproval(supplierException).reasonCode());
        assertThrows(IllegalArgumentException.class,
                () -> policy.requireSupplierExceptionApproval(settlementOverride));
    }

    private SettlementApprovalDecision approvedOverride(String approvalId) {
        return new SettlementApprovalDecision(
                approvalId,
                "maker-a",
                "checker-b",
                SettlementApprovalReasonCode.SETTLEMENT_OVERRIDE,
                Instant.parse("2026-02-20T00:00:00Z"),
                Map.of("ticket", "TKT-ERP-STAGE-095", "approvalSource", "workflow"));
    }
}
