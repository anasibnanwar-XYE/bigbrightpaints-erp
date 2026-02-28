package com.bigbrightpaints.erp.modules.purchasing.service;

import org.springframework.stereotype.Component;

@Component
public class SupplierApprovalPolicy {

    public SupplierApprovalDecision requireSupplierExceptionApproval(SupplierApprovalDecision approval) {
        SupplierApprovalDecision resolved = SupplierApprovalDecision.requireApproved(approval, "Supplier exception");
        if (resolved.reasonCode() != SupplierApprovalReasonCode.SUPPLIER_EXCEPTION) {
            throw com.bigbrightpaints.erp.core.validation.ValidationUtils.invalidInput("Supplier exception approval must use SUPPLIER_EXCEPTION reason code");
        }
        return resolved;
    }

    public SupplierApprovalDecision requireSettlementOverrideApproval(SupplierApprovalDecision approval) {
        SupplierApprovalDecision resolved = SupplierApprovalDecision.requireApproved(approval, "Settlement override");
        if (resolved.reasonCode() != SupplierApprovalReasonCode.SETTLEMENT_OVERRIDE) {
            throw com.bigbrightpaints.erp.core.validation.ValidationUtils.invalidInput("Settlement override approval must use SETTLEMENT_OVERRIDE reason code");
        }
        return resolved;
    }
}
