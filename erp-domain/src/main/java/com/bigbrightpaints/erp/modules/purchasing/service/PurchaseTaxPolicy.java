package com.bigbrightpaints.erp.modules.purchasing.service;

import com.bigbrightpaints.erp.core.exception.ApplicationException;
import com.bigbrightpaints.erp.core.exception.ErrorCode;
import com.bigbrightpaints.erp.modules.company.domain.Company;
import com.bigbrightpaints.erp.modules.inventory.domain.RawMaterial;
import com.bigbrightpaints.erp.modules.purchasing.dto.RawMaterialPurchaseLineRequest;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

@Component
public class PurchaseTaxPolicy {

    private static final BigDecimal MAX_GST_RATE = new BigDecimal("28.00");

    public PurchaseTaxMode resolvePurchaseTaxMode(List<RawMaterialPurchaseLineRequest> lineRequests,
                                                  Map<Long, RawMaterial> lockedMaterials) {
        PurchaseTaxMode resolved = null;
        for (RawMaterialPurchaseLineRequest lineRequest : lineRequests) {
            RawMaterial rawMaterial = lockedMaterials.get(lineRequest.rawMaterialId());
            if (rawMaterial == null) {
                continue;
            }
            PurchaseTaxMode lineMode = rawMaterial.isGstApplicable()
                    ? PurchaseTaxMode.GST
                    : PurchaseTaxMode.NON_GST;
            if (resolved == null) {
                resolved = lineMode;
                continue;
            }
            if (resolved != lineMode) {
                throw new ApplicationException(ErrorCode.VALIDATION_INVALID_INPUT,
                        "Purchase invoice cannot mix GST and non-GST materials")
                        .withDetail("rawMaterialId", rawMaterial.getId())
                        .withDetail("expectedTaxMode", taxModeLabel(resolved))
                        .withDetail("lineTaxMode", taxModeLabel(lineMode));
            }
        }
        return resolved != null ? resolved : PurchaseTaxMode.GST;
    }

    public BigDecimal resolveLineTaxRateForMode(RawMaterialPurchaseLineRequest lineRequest,
                                                RawMaterial rawMaterial,
                                                Company company,
                                                PurchaseTaxMode purchaseTaxMode) {
        if (purchaseTaxMode == PurchaseTaxMode.NON_GST) {
            enforceNonGstLineContract(lineRequest, rawMaterial);
            return BigDecimal.ZERO;
        }
        return resolveLineTaxRate(lineRequest, rawMaterial, company);
    }

    public void enforcePurchaseTaxContract(PurchaseTaxMode purchaseTaxMode,
                                           BigDecimal providedTaxAmount,
                                           boolean hasTaxableLines) {
        if (providedTaxAmount == null) {
            return;
        }
        if (purchaseTaxMode == PurchaseTaxMode.NON_GST
                && providedTaxAmount.compareTo(BigDecimal.ZERO) > 0) {
            throw new ApplicationException(ErrorCode.VALIDATION_INVALID_INPUT,
                    "Non-GST purchase invoice cannot carry GST tax amount")
                    .withDetail("taxMode", taxModeLabel(purchaseTaxMode))
                    .withDetail("taxAmount", providedTaxAmount);
        }
        if (purchaseTaxMode == PurchaseTaxMode.GST
                && providedTaxAmount.compareTo(BigDecimal.ZERO) == 0
                && hasTaxableLines) {
            throw new ApplicationException(ErrorCode.VALIDATION_INVALID_INPUT,
                    "GST purchase invoice with taxable lines requires non-zero taxAmount or tax auto-computation")
                    .withDetail("taxMode", taxModeLabel(purchaseTaxMode))
                    .withDetail("taxAmount", providedTaxAmount);
        }
    }

    private void enforceNonGstLineContract(RawMaterialPurchaseLineRequest lineRequest,
                                           RawMaterial rawMaterial) {
        BigDecimal requestedTaxRate = lineRequest != null ? lineRequest.taxRate() : null;
        if (requestedTaxRate != null && normalizePercent(requestedTaxRate).compareTo(BigDecimal.ZERO) > 0) {
            throw new ApplicationException(ErrorCode.VALIDATION_INVALID_INPUT,
                    "Non-GST purchase line cannot declare a positive GST rate")
                    .withDetail("rawMaterialId", rawMaterial != null ? rawMaterial.getId() : null)
                    .withDetail("taxRate", requestedTaxRate);
        }
        if (lineRequest != null && Boolean.TRUE.equals(lineRequest.taxInclusive())) {
            throw new ApplicationException(ErrorCode.VALIDATION_INVALID_INPUT,
                    "Non-GST purchase line cannot be tax-inclusive")
                    .withDetail("rawMaterialId", rawMaterial != null ? rawMaterial.getId() : null);
        }
        BigDecimal materialTaxRate = rawMaterial != null ? normalizePercent(rawMaterial.getGstRate()) : BigDecimal.ZERO;
        if (materialTaxRate.compareTo(BigDecimal.ZERO) > 0) {
            throw new ApplicationException(ErrorCode.VALIDATION_INVALID_INPUT,
                    "Non-GST raw material cannot carry a positive GST rate")
                    .withDetail("rawMaterialId", rawMaterial.getId())
                    .withDetail("materialGstRate", materialTaxRate);
        }
    }

    private BigDecimal resolveLineTaxRate(RawMaterialPurchaseLineRequest lineRequest,
                                          RawMaterial rawMaterial,
                                          Company company) {
        if (lineRequest != null && lineRequest.taxRate() != null) {
            return normalizePercent(lineRequest.taxRate());
        }
        if (rawMaterial != null && rawMaterial.getGstRate() != null) {
            return normalizePercent(rawMaterial.getGstRate());
        }
        if (company != null && company.getDefaultGstRate() != null) {
            return normalizePercent(company.getDefaultGstRate());
        }
        return BigDecimal.ZERO;
    }

    private BigDecimal normalizePercent(BigDecimal rate) {
        if (rate == null) {
            return BigDecimal.ZERO;
        }
        if (rate.compareTo(BigDecimal.ZERO) < 0) {
            throw com.bigbrightpaints.erp.core.validation.ValidationUtils.invalidInput("GST rate must be zero or positive");
        }
        BigDecimal sanitized = rate.setScale(2, RoundingMode.HALF_UP);
        if (sanitized.compareTo(MAX_GST_RATE) > 0) {
            throw com.bigbrightpaints.erp.core.validation.ValidationUtils.invalidInput(
                    "Unsupported GST rate " + sanitized + "%. Max allowed is " + MAX_GST_RATE);
        }
        return sanitized.setScale(4, RoundingMode.HALF_UP);
    }

    private String taxModeLabel(PurchaseTaxMode purchaseTaxMode) {
        return purchaseTaxMode == PurchaseTaxMode.NON_GST ? "NON_GST" : "GST";
    }

    enum PurchaseTaxMode {
        GST,
        NON_GST
    }
}
