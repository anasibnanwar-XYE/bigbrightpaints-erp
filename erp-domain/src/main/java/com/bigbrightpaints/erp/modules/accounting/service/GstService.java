package com.bigbrightpaints.erp.modules.accounting.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.bigbrightpaints.erp.core.exception.ApplicationException;
import com.bigbrightpaints.erp.core.exception.ErrorCode;
import com.bigbrightpaints.erp.core.util.MoneyUtils;

@Service
public class GstService {

  private static final BigDecimal HUNDRED = new BigDecimal("100");

  public GstBreakdown calculateGst(
      BigDecimal amount, String sourceState, String destState, BigDecimal gstRate) {
    BigDecimal taxableAmount = normalizeAmount(amount, "amount");
    BigDecimal rate = normalizeRate(gstRate);

    if (taxableAmount.compareTo(BigDecimal.ZERO) == 0 || rate.compareTo(BigDecimal.ZERO) == 0) {
      TaxType taxType = resolveTaxType(sourceState, destState, false);
      return zeroBreakdown(taxableAmount, taxType);
    }

    TaxType taxType = resolveTaxType(sourceState, destState, true);
    BigDecimal totalTax =
        MoneyUtils.roundCurrency(
            taxableAmount.multiply(rate).divide(HUNDRED, 6, RoundingMode.HALF_UP));

    return splitTaxAmount(taxableAmount, totalTax, sourceState, destState, taxType);
  }

  public GstBreakdown splitTaxAmount(
      BigDecimal taxableAmount, BigDecimal totalTax, String sourceState, String destState) {
    BigDecimal taxable = normalizeAmount(taxableAmount, "taxableAmount");
    BigDecimal tax = normalizeAmount(totalTax, "totalTax");
    if (tax.compareTo(BigDecimal.ZERO) == 0) {
      TaxType taxType = resolveTaxType(sourceState, destState, false);
      return zeroBreakdown(taxable, taxType);
    }
    TaxType taxType = resolveTaxType(sourceState, destState, true);
    return splitTaxAmount(taxable, tax, sourceState, destState, taxType);
  }

  private GstBreakdown splitTaxAmount(
      BigDecimal taxableAmount,
      BigDecimal totalTax,
      String sourceState,
      String destState,
      TaxType taxType) {
    if (taxType == TaxType.INTER_STATE) {
      return new GstBreakdown(
          MoneyUtils.roundCurrency(taxableAmount),
          BigDecimal.ZERO,
          BigDecimal.ZERO,
          MoneyUtils.roundCurrency(totalTax),
          taxType);
    }

    BigDecimal roundedTax = MoneyUtils.roundCurrency(totalTax);
    BigDecimal cgst =
        MoneyUtils.roundCurrency(roundedTax.divide(new BigDecimal("2"), 6, RoundingMode.HALF_UP));
    BigDecimal sgst = MoneyUtils.roundCurrency(roundedTax.subtract(cgst));
    return new GstBreakdown(
        MoneyUtils.roundCurrency(taxableAmount), cgst, sgst, BigDecimal.ZERO, taxType);
  }

  public TaxType resolveTaxType(String sourceState, String destState, boolean failOnMissingStates) {
    String source = normalizeStateCode(sourceState);
    String destination = normalizeStateCode(destState);
    if (!StringUtils.hasText(source) || !StringUtils.hasText(destination)) {
      if (failOnMissingStates) {
        throw new ApplicationException(
                ErrorCode.VALIDATION_MISSING_REQUIRED_FIELD,
                "State codes are required for GST decisioning")
            .withDetail("sourceState", sourceState)
            .withDetail("destinationState", destState);
      }
      return TaxType.INTER_STATE;
    }
    return source.equals(destination) ? TaxType.INTRA_STATE : TaxType.INTER_STATE;
  }

  public String normalizeStateCode(String stateCode) {
    if (!StringUtils.hasText(stateCode)) {
      return null;
    }
    String normalized = stateCode.trim().toUpperCase();
    if (normalized.length() != 2) {
      throw new ApplicationException(
              ErrorCode.VALIDATION_INVALID_INPUT, "State code must be exactly 2 characters")
          .withDetail("stateCode", stateCode);
    }
    return normalized;
  }

  private BigDecimal normalizeAmount(BigDecimal amount, String fieldName) {
    if (amount == null) {
      return BigDecimal.ZERO;
    }
    if (amount.compareTo(BigDecimal.ZERO) < 0) {
      throw new ApplicationException(
              ErrorCode.VALIDATION_INVALID_INPUT, fieldName + " cannot be negative")
          .withDetail(fieldName, amount);
    }
    return MoneyUtils.roundCurrency(amount);
  }

  private BigDecimal normalizeRate(BigDecimal rate) {
    if (rate == null) {
      return BigDecimal.ZERO;
    }
    if (rate.compareTo(BigDecimal.ZERO) < 0 || rate.compareTo(HUNDRED) > 0) {
      throw new ApplicationException(
              ErrorCode.VALIDATION_INVALID_INPUT, "GST rate must be between 0 and 100")
          .withDetail("gstRate", rate);
    }
    return rate.setScale(4, RoundingMode.HALF_UP);
  }

  private GstBreakdown zeroBreakdown(BigDecimal taxableAmount, TaxType taxType) {
    return new GstBreakdown(
        MoneyUtils.roundCurrency(taxableAmount),
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        taxType);
  }

  public enum TaxType {
    INTRA_STATE,
    INTER_STATE
  }

  public record GstBreakdown(
      BigDecimal taxableAmount,
      BigDecimal cgst,
      BigDecimal sgst,
      BigDecimal igst,
      TaxType taxType) {
    public BigDecimal totalTax() {
      return MoneyUtils.roundCurrency(cgst.add(sgst).add(igst));
    }
  }
}
