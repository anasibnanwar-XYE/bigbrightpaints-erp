package com.bigbrightpaints.erp.modules.accounting.service;

import org.springframework.util.StringUtils;

import com.bigbrightpaints.erp.modules.accounting.domain.PartnerSettlementAllocation;
import com.bigbrightpaints.erp.modules.accounting.dto.SettlementAllocationApplication;

final class SettlementAuditMemoDecoder {

  DecodedSettlementAuditMemo decode(PartnerSettlementAllocation allocation) {
    if (allocation == null || allocation.getInvoice() != null || allocation.getPurchase() != null) {
      return new DecodedSettlementAuditMemo(
          SettlementAllocationApplication.DOCUMENT,
          normalizeSettlementAuditMemo(allocation != null ? allocation.getMemo() : null));
    }
    String memo = normalizeSettlementAuditMemo(allocation.getMemo());
    if (memo == null || !memo.startsWith("[SETTLEMENT-APPLICATION:")) {
      return new DecodedSettlementAuditMemo(SettlementAllocationApplication.ON_ACCOUNT, memo);
    }
    int closingBracket = memo.indexOf(']');
    if (closingBracket <= 0) {
      return new DecodedSettlementAuditMemo(SettlementAllocationApplication.ON_ACCOUNT, memo);
    }
    String token = memo.substring("[SETTLEMENT-APPLICATION:".length(), closingBracket).trim();
    SettlementAllocationApplication applicationType;
    try {
      applicationType = SettlementAllocationApplication.valueOf(token);
    } catch (IllegalArgumentException ex) {
      applicationType = SettlementAllocationApplication.ON_ACCOUNT;
    }
    String visibleMemo = normalizeSettlementAuditMemo(memo.substring(closingBracket + 1));
    return new DecodedSettlementAuditMemo(applicationType, visibleMemo);
  }

  private String normalizeSettlementAuditMemo(String memo) {
    if (memo == null) {
      return null;
    }
    String trimmed = memo.trim();
    return StringUtils.hasText(trimmed) ? trimmed : null;
  }

  record DecodedSettlementAuditMemo(SettlementAllocationApplication applicationType, String memo) {}
}
