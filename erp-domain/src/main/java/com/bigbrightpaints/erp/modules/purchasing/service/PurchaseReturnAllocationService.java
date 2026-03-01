package com.bigbrightpaints.erp.modules.purchasing.service;

import com.bigbrightpaints.erp.core.exception.ApplicationException;
import com.bigbrightpaints.erp.core.exception.ErrorCode;
import com.bigbrightpaints.erp.core.util.MoneyUtils;
import com.bigbrightpaints.erp.modules.inventory.domain.RawMaterial;
import com.bigbrightpaints.erp.modules.purchasing.domain.RawMaterialPurchase;
import com.bigbrightpaints.erp.modules.purchasing.domain.RawMaterialPurchaseLine;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class PurchaseReturnAllocationService {

    private static final BigDecimal QUANTITY_TOLERANCE = new BigDecimal("0.0001");

    public BigDecimal remainingReturnableQuantity(RawMaterialPurchase purchase, RawMaterial material) {
        if (purchase == null || material == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal purchased = purchase.getLines().stream()
                .filter(line -> line.getRawMaterial() != null
                        && line.getRawMaterial().getId().equals(material.getId()))
                .map(line -> quantityValue(line.getQuantity()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal returned = purchase.getLines().stream()
                .filter(line -> line.getRawMaterial() != null
                        && line.getRawMaterial().getId().equals(material.getId()))
                .map(line -> quantityValue(line.getReturnedQuantity()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal remaining = purchased.subtract(returned);
        if (remaining.compareTo(BigDecimal.ZERO) < 0 && remaining.abs().compareTo(QUANTITY_TOLERANCE) <= 0) {
            return BigDecimal.ZERO;
        }
        return remaining.max(BigDecimal.ZERO);
    }

    public void applyPurchaseReturnQuantity(RawMaterialPurchase purchase, RawMaterial material, BigDecimal quantity) {
        if (purchase == null || material == null || quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        BigDecimal remaining = quantity;
        for (RawMaterialPurchaseLine line : purchase.getLines()) {
            if (line.getRawMaterial() == null || !line.getRawMaterial().getId().equals(material.getId())) {
                continue;
            }
            BigDecimal lineQty = quantityValue(line.getQuantity());
            BigDecimal alreadyReturned = quantityValue(line.getReturnedQuantity());
            BigDecimal available = lineQty.subtract(alreadyReturned);
            if (available.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            BigDecimal applied = available.min(remaining);
            line.setReturnedQuantity(alreadyReturned.add(applied));
            remaining = remaining.subtract(applied);
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }
        }
        if (remaining.compareTo(QUANTITY_TOLERANCE) > 0) {
            throw new ApplicationException(ErrorCode.BUSINESS_CONSTRAINT_VIOLATION,
                    "Purchase return quantity exceeds available returnable quantity after allocation")
                    .withDetail("purchaseId", purchase.getId())
                    .withDetail("rawMaterialId", material.getId())
                    .withDetail("remainingQuantity", remaining);
        }
    }

    public void applyPurchaseReturnToOutstanding(RawMaterialPurchase purchase, BigDecimal totalAmount) {
        if (purchase == null) {
            return;
        }
        BigDecimal amount = currency(totalAmount != null ? totalAmount : BigDecimal.ZERO);
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        BigDecimal currentOutstanding = currency(MoneyUtils.zeroIfNull(purchase.getOutstandingAmount()));
        BigDecimal newOutstanding = currency(currentOutstanding.subtract(amount));
        if (newOutstanding.compareTo(BigDecimal.ZERO) < 0) {
            throw new ApplicationException(ErrorCode.RETURN_EXCEEDS_OUTSTANDING,
                    "Purchase return amount exceeds outstanding payable")
                    .withDetail("purchaseId", purchase.getId())
                    .withDetail("currentOutstanding", currentOutstanding)
                    .withDetail("returnAmount", amount);
        }
        purchase.setOutstandingAmount(newOutstanding);
        if (purchase.getOutstandingAmount().compareTo(BigDecimal.ZERO) == 0 && isPurchaseFullyReturned(purchase)) {
            purchase.setStatus("VOID");
        } else {
            updatePurchaseStatus(purchase);
        }
    }

    private boolean isPurchaseFullyReturned(RawMaterialPurchase purchase) {
        if (purchase == null || purchase.getLines() == null || purchase.getLines().isEmpty()) {
            return false;
        }
        for (RawMaterialPurchaseLine line : purchase.getLines()) {
            BigDecimal lineQty = quantityValue(line.getQuantity());
            if (lineQty.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            BigDecimal returnedQty = quantityValue(line.getReturnedQuantity());
            BigDecimal remaining = lineQty.subtract(returnedQty);
            if (remaining.compareTo(QUANTITY_TOLERANCE) > 0) {
                return false;
            }
        }
        return true;
    }

    private void updatePurchaseStatus(RawMaterialPurchase purchase) {
        String status = purchase.getStatus();
        if (status != null && ("VOID".equalsIgnoreCase(status) || "REVERSED".equalsIgnoreCase(status))) {
            return;
        }
        BigDecimal total = MoneyUtils.zeroIfNull(purchase.getTotalAmount());
        BigDecimal outstanding = MoneyUtils.zeroIfNull(purchase.getOutstandingAmount());
        if (outstanding.compareTo(BigDecimal.ZERO) <= 0) {
            purchase.setStatus("PAID");
        } else if (total.compareTo(BigDecimal.ZERO) > 0 && outstanding.compareTo(total) < 0) {
            purchase.setStatus("PARTIAL");
        } else {
            purchase.setStatus("POSTED");
        }
    }

    private BigDecimal quantityValue(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private BigDecimal currency(BigDecimal value) {
        return MoneyUtils.roundCurrency(value);
    }
}
