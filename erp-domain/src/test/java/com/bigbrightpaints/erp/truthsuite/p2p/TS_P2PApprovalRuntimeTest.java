package com.bigbrightpaints.erp.truthsuite.p2p;

import com.bigbrightpaints.erp.modules.invoice.service.SettlementApprovalDecision;
import com.bigbrightpaints.erp.modules.invoice.service.SettlementApprovalReasonCode;
import com.bigbrightpaints.erp.modules.purchasing.service.SupplierApprovalDecision;
import com.bigbrightpaints.erp.modules.purchasing.service.SupplierApprovalPolicy;
import com.bigbrightpaints.erp.modules.purchasing.service.SupplierApprovalReasonCode;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Tag("critical")
@Tag("reconciliation")
class TS_P2PApprovalRuntimeTest {

    @Test
    void settlementApprovalDecisionFailsClosedWhenMetadataTicketMissing() {
        assertThrows(IllegalArgumentException.class, () -> new SettlementApprovalDecision(
                "APP-SET-001",
                "maker-a",
                "checker-b",
                SettlementApprovalReasonCode.SETTLEMENT_OVERRIDE,
                Instant.parse("2026-02-20T00:00:00Z"),
                Map.of("approvalSource", "workflow")));
    }

    @Test
    void settlementApprovalDecisionFailsClosedWhenMetadataSourceMissing() {
        assertThrows(IllegalArgumentException.class, () -> new SettlementApprovalDecision(
                "APP-SET-002",
                "maker-a",
                "checker-b",
                SettlementApprovalReasonCode.SUPPLIER_EXCEPTION,
                Instant.parse("2026-02-20T00:00:00Z"),
                Map.of("ticket", "TKT-ERP-STAGE-095")));
    }

    @Test
    void supplierApprovalDecisionEnforcesMakerCheckerSeparation() {
        assertThrows(IllegalArgumentException.class, () -> new SupplierApprovalDecision(
                "APP-SUP-001",
                "same-user",
                "same-user",
                SupplierApprovalReasonCode.SUPPLIER_EXCEPTION,
                Instant.parse("2026-02-20T00:00:00Z"),
                Map.of("ticket", "TKT-ERP-STAGE-095", "approvalSource", "workflow")));
    }

    @Test
    void supplierApprovalDecisionKeepsAuditMetadataImmutable() {
        SupplierApprovalDecision decision = new SupplierApprovalDecision(
                "APP-SUP-002",
                "maker-a",
                "checker-b",
                SupplierApprovalReasonCode.SETTLEMENT_OVERRIDE,
                Instant.parse("2026-02-20T00:00:00Z"),
                Map.of("ticket", "TKT-ERP-STAGE-095", "approvalSource", "workflow"));

        assertEquals("TKT-ERP-STAGE-095", decision.immutableAuditMetadata().get("ticket"));
        assertThrows(UnsupportedOperationException.class,
                () -> decision.immutableAuditMetadata().put("newKey", "newValue"));
    }

    @Test
    void supplierApprovalPolicyEnforcesReasonCodesForBothPaths() {
        SupplierApprovalPolicy policy = new SupplierApprovalPolicy();
        SupplierApprovalDecision supplierException = new SupplierApprovalDecision(
                "APP-SUP-003",
                "maker-a",
                "checker-b",
                SupplierApprovalReasonCode.SUPPLIER_EXCEPTION,
                Instant.parse("2026-02-20T00:00:00Z"),
                Map.of("ticket", "TKT-ERP-STAGE-095", "approvalSource", "workflow"));
        SupplierApprovalDecision settlementOverride = new SupplierApprovalDecision(
                "APP-SUP-004",
                "maker-c",
                "checker-d",
                SupplierApprovalReasonCode.SETTLEMENT_OVERRIDE,
                Instant.parse("2026-02-20T00:00:00Z"),
                Map.of("ticket", "TKT-ERP-STAGE-095", "approvalSource", "workflow"));

        assertEquals(SupplierApprovalReasonCode.SUPPLIER_EXCEPTION,
                policy.requireSupplierExceptionApproval(supplierException).reasonCode());
        assertEquals(SupplierApprovalReasonCode.SETTLEMENT_OVERRIDE,
                policy.requireSettlementOverrideApproval(settlementOverride).reasonCode());

        assertThrows(IllegalArgumentException.class,
                () -> policy.requireSupplierExceptionApproval(settlementOverride));
        assertThrows(IllegalArgumentException.class,
                () -> policy.requireSettlementOverrideApproval(supplierException));
    }
}
