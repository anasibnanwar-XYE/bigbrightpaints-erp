package com.bigbrightpaints.erp.modules.sales.dto;

import java.math.BigDecimal;
import java.util.List;

import com.bigbrightpaints.erp.core.exception.ApplicationException;
import com.bigbrightpaints.erp.core.exception.ErrorCode;
import com.bigbrightpaints.erp.core.idempotency.IdempotencyUtils;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record SalesOrderRequest(
    Long dealerId,
    @NotNull BigDecimal totalAmount,
    String currency,
    String notes,
    @NotEmpty List<@Valid SalesOrderItemRequest> items,
    String gstTreatment,
    BigDecimal gstRate,
    Boolean gstInclusive,
    String idempotencyKey,
    String paymentMode,
    String paymentTerms) {
  private static final String DEFAULT_PAYMENT_MODE = "CREDIT";
  private static final String HYBRID_PAYMENT_MODE = "HYBRID";

  public SalesOrderRequest(
      Long dealerId,
      BigDecimal totalAmount,
      String currency,
      String notes,
      List<@Valid SalesOrderItemRequest> items,
      String gstTreatment,
      BigDecimal gstRate,
      Boolean gstInclusive,
      String idempotencyKey) {
    this(
        dealerId,
        totalAmount,
        currency,
        notes,
        items,
        gstTreatment,
        gstRate,
        gstInclusive,
        idempotencyKey,
        null,
        null);
  }

  public SalesOrderRequest(
      Long dealerId,
      BigDecimal totalAmount,
      String currency,
      String notes,
      List<@Valid SalesOrderItemRequest> items,
      String gstTreatment,
      BigDecimal gstRate,
      Boolean gstInclusive,
      String idempotencyKey,
      String paymentMode) {
    this(
        dealerId,
        totalAmount,
        currency,
        notes,
        items,
        gstTreatment,
        gstRate,
        gstInclusive,
        idempotencyKey,
        paymentMode,
        null);
  }

  public String normalizedPaymentMode() {
    String normalized = rawNormalizedPaymentMode();
    if (DEFAULT_PAYMENT_MODE.equals(normalized)) {
      return DEFAULT_PAYMENT_MODE;
    }
    return normalized;
  }

  public String resolveIdempotencyKey() {
    String normalized = IdempotencyUtils.normalizeKey(idempotencyKey);
    if (normalized != null) {
      return normalized;
    }
    throw new ApplicationException(
        ErrorCode.VALIDATION_MISSING_REQUIRED_FIELD,
        "Idempotency-Key header is required for sales orders");
  }

  private String rawNormalizedPaymentMode() {
    String normalized = IdempotencyUtils.normalizeUpperToken(paymentMode);
    if (normalized.isBlank()) {
      return DEFAULT_PAYMENT_MODE;
    }
    return normalized;
  }

  public boolean usesFinishedGoodSelection() {
    return items != null && items.stream().anyMatch(SalesOrderItemRequest::hasFinishedGoodId);
  }
}
