package com.bigbrightpaints.erp.modules.accounting.service;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;

import com.bigbrightpaints.erp.core.idempotency.IdempotencyUtils;
import com.bigbrightpaints.erp.core.validation.ValidationUtils;
import com.bigbrightpaints.erp.modules.accounting.domain.PartnerType;
import com.bigbrightpaints.erp.modules.accounting.dto.AutoSettlementRequest;
import com.bigbrightpaints.erp.modules.accounting.dto.JournalEntryDto;
import com.bigbrightpaints.erp.modules.accounting.dto.PartnerSettlementRequest;
import com.bigbrightpaints.erp.modules.accounting.dto.PartnerSettlementResponse;
import com.bigbrightpaints.erp.modules.accounting.dto.SupplierPaymentRequest;

@Service
public class SettlementService {

  private final SupplierPaymentService supplierPaymentService;
  private final DealerSettlementService dealerSettlementService;
  private final SupplierSettlementService supplierSettlementService;

  public SettlementService(
      SupplierPaymentService supplierPaymentService,
      DealerSettlementService dealerSettlementService,
      SupplierSettlementService supplierSettlementService) {
    this.supplierPaymentService = supplierPaymentService;
    this.dealerSettlementService = dealerSettlementService;
    this.supplierSettlementService = supplierSettlementService;
  }

  public JournalEntryDto recordSupplierPayment(SupplierPaymentRequest request) {
    return supplierPaymentService.recordSupplierPayment(normalizeSupplierPaymentRequest(request));
  }

  public PartnerSettlementResponse settleDealerInvoices(PartnerSettlementRequest request) {
    return dealerSettlementService.settleDealerInvoices(normalizeDealerSettlementRequest(request));
  }

  public PartnerSettlementResponse autoSettleDealer(Long dealerId, AutoSettlementRequest request) {
    return dealerSettlementService.autoSettleDealer(
        dealerId, normalizeAutoSettlementRequest(dealerId, request));
  }

  public PartnerSettlementResponse settleSupplierInvoices(PartnerSettlementRequest request) {
    return supplierSettlementService.settleSupplierInvoices(
        normalizeSupplierSettlementRequest(request));
  }

  public PartnerSettlementResponse autoSettleSupplier(
      Long supplierId, AutoSettlementRequest request) {
    return supplierSettlementService.autoSettleSupplier(
        supplierId, normalizeAutoSettlementRequest(supplierId, request));
  }

  private SupplierPaymentRequest normalizeSupplierPaymentRequest(SupplierPaymentRequest request) {
    ValidationUtils.requireNotNull(request, "request");
    ValidationUtils.requireNotNull(request.supplierId(), "supplierId");
    ValidationUtils.requireNotNull(request.cashAccountId(), "cashAccountId");
    ValidationUtils.requirePositive(request.amount(), "amount");
    return new SupplierPaymentRequest(
        request.supplierId(),
        request.cashAccountId(),
        request.amount().abs(),
        normalizeText(request.referenceNumber()),
        normalizeText(request.memo()),
        normalizeText(request.idempotencyKey()),
        request.allocations());
  }

  private PartnerSettlementRequest normalizeDealerSettlementRequest(
      PartnerSettlementRequest request) {
    ValidationUtils.requireNotNull(request, "request");
    ValidationUtils.requireNotNull(request.partnerType(), "partnerType");
    if (request.partnerType() != PartnerType.DEALER) {
      throw ValidationUtils.invalidState("Dealer settlements require partnerType DEALER");
    }
    ValidationUtils.requireNotNull(request.partnerId(), "partnerId");
    return new PartnerSettlementRequest(
        request.partnerType(),
        request.partnerId(),
        request.cashAccountId(),
        request.discountAccountId(),
        request.writeOffAccountId(),
        request.fxGainAccountId(),
        request.fxLossAccountId(),
        positiveAmountOrNull(request.amount()),
        request.unappliedAmountApplication(),
        request.settlementDate(),
        normalizeText(request.referenceNumber()),
        normalizeText(request.memo()),
        normalizeText(request.idempotencyKey()),
        Boolean.TRUE.equals(request.adminOverride()),
        request.allocations(),
        request.payments());
  }

  private PartnerSettlementRequest normalizeSupplierSettlementRequest(
      PartnerSettlementRequest request) {
    ValidationUtils.requireNotNull(request, "request");
    ValidationUtils.requireNotNull(request.partnerType(), "partnerType");
    if (request.partnerType() != PartnerType.SUPPLIER) {
      throw ValidationUtils.invalidState("Supplier settlements require partnerType SUPPLIER");
    }
    ValidationUtils.requireNotNull(request.partnerId(), "partnerId");
    return new PartnerSettlementRequest(
        request.partnerType(),
        request.partnerId(),
        request.cashAccountId(),
        request.discountAccountId(),
        request.writeOffAccountId(),
        request.fxGainAccountId(),
        request.fxLossAccountId(),
        positiveAmountOrNull(request.amount()),
        request.unappliedAmountApplication(),
        request.settlementDate(),
        normalizeText(request.referenceNumber()),
        normalizeText(request.memo()),
        normalizeText(request.idempotencyKey()),
        Boolean.TRUE.equals(request.adminOverride()),
        request.allocations(),
        request.payments());
  }

  private AutoSettlementRequest normalizeAutoSettlementRequest(
      Long partnerId, AutoSettlementRequest request) {
    ValidationUtils.requireNotNull(partnerId, "partnerId");
    ValidationUtils.requireNotNull(request, "request");
    ValidationUtils.requirePositive(request.amount(), "amount");
    return new AutoSettlementRequest(
        request.cashAccountId(),
        request.amount().abs(),
        normalizeText(request.referenceNumber()),
        normalizeText(request.memo()),
        normalizeText(request.idempotencyKey()));
  }

  private BigDecimal positiveAmountOrNull(BigDecimal value) {
    if (value == null) {
      return null;
    }
    ValidationUtils.requirePositive(value, "amount");
    return value.abs();
  }

  private String normalizeText(String value) {
    String normalized = IdempotencyUtils.normalizeToken(value);
    return normalized.isBlank() ? null : normalized;
  }
}
