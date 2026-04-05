package com.bigbrightpaints.erp.modules.accounting.controller;

import java.util.function.BiFunction;
import java.util.function.Function;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bigbrightpaints.erp.core.util.IdempotencyHeaderUtils;
import com.bigbrightpaints.erp.modules.accounting.dto.AutoSettlementRequest;
import com.bigbrightpaints.erp.modules.accounting.dto.DealerReceiptRequest;
import com.bigbrightpaints.erp.modules.accounting.dto.DealerReceiptSplitRequest;
import com.bigbrightpaints.erp.modules.accounting.dto.DealerSettlementRequest;
import com.bigbrightpaints.erp.modules.accounting.dto.JournalEntryDto;
import com.bigbrightpaints.erp.modules.accounting.dto.PartnerSettlementResponse;
import com.bigbrightpaints.erp.modules.accounting.dto.SupplierSettlementRequest;
import com.bigbrightpaints.erp.modules.accounting.service.DealerReceiptService;
import com.bigbrightpaints.erp.modules.accounting.service.SettlementService;
import com.bigbrightpaints.erp.shared.dto.ApiResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/accounting")
public class SettlementController {

  private final DealerReceiptService dealerReceiptService;
  private final SettlementService settlementService;

  public SettlementController(
      DealerReceiptService dealerReceiptService, SettlementService settlementService) {
    this.dealerReceiptService = dealerReceiptService;
    this.settlementService = settlementService;
  }

  @PostMapping("/receipts/dealer")
  @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')")
  public ResponseEntity<ApiResponse<JournalEntryDto>> recordDealerReceipt(
      @Valid @RequestBody DealerReceiptRequest request,
      @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
    return ResponseEntity.ok(
        ApiResponse.success(
            "Receipt recorded",
            dealerReceiptService.recordDealerReceipt(
                applyIdempotencyKey(
                    request,
                    DealerReceiptRequest::idempotencyKey,
                    SettlementRequestCopies::dealerReceipt,
                    idempotencyKey))));
  }

  @PostMapping("/receipts/dealer/hybrid")
  @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')")
  public ResponseEntity<ApiResponse<JournalEntryDto>> recordDealerHybridReceipt(
      @Valid @RequestBody DealerReceiptSplitRequest request,
      @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
    return ResponseEntity.ok(
        ApiResponse.success(
            "Receipt recorded",
            dealerReceiptService.recordDealerReceiptSplit(
                applyIdempotencyKey(
                    request,
                    DealerReceiptSplitRequest::idempotencyKey,
                    SettlementRequestCopies::dealerReceiptSplit,
                    idempotencyKey))));
  }

  @PostMapping("/settlements/dealers")
  @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')")
  public ResponseEntity<ApiResponse<PartnerSettlementResponse>> settleDealer(
      @Valid @RequestBody DealerSettlementRequest request,
      @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
    return ResponseEntity.ok(
        ApiResponse.success(
            "Settlement recorded",
            settlementService.settleDealerInvoices(
                applyIdempotencyKey(
                    request,
                    DealerSettlementRequest::idempotencyKey,
                    SettlementRequestCopies::dealerSettlement,
                    idempotencyKey))));
  }

  @PostMapping("/dealers/{dealerId}/auto-settle")
  @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')")
  public ResponseEntity<ApiResponse<PartnerSettlementResponse>> autoSettleDealer(
      @PathVariable Long dealerId,
      @Valid @RequestBody AutoSettlementRequest request,
      @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
    return ResponseEntity.ok(
        ApiResponse.success(
            "Auto-settlement recorded",
            settlementService.autoSettleDealer(
                dealerId,
                applyIdempotencyKey(
                    request,
                    AutoSettlementRequest::idempotencyKey,
                    SettlementRequestCopies::autoSettlement,
                    idempotencyKey))));
  }

  @PostMapping("/settlements/suppliers")
  @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')")
  public ResponseEntity<ApiResponse<PartnerSettlementResponse>> settleSupplier(
      @Valid @RequestBody SupplierSettlementRequest request,
      @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
    return ResponseEntity.ok(
        ApiResponse.success(
            "Settlement recorded",
            settlementService.settleSupplierInvoices(
                applyIdempotencyKey(
                    request,
                    SupplierSettlementRequest::idempotencyKey,
                    SettlementRequestCopies::supplierSettlement,
                    idempotencyKey))));
  }

  @PostMapping("/suppliers/{supplierId}/auto-settle")
  @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')")
  public ResponseEntity<ApiResponse<PartnerSettlementResponse>> autoSettleSupplier(
      @PathVariable Long supplierId,
      @Valid @RequestBody AutoSettlementRequest request,
      @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
    return ResponseEntity.ok(
        ApiResponse.success(
            "Auto-settlement recorded",
            settlementService.autoSettleSupplier(
                supplierId,
                applyIdempotencyKey(
                    request,
                    AutoSettlementRequest::idempotencyKey,
                    SettlementRequestCopies::autoSettlement,
                    idempotencyKey))));
  }

  private <T> T applyIdempotencyKey(
      T request,
      Function<T, String> requestIdempotencyKeyExtractor,
      BiFunction<T, String, T> requestWithIdempotencyKey,
      String idempotencyKeyHeader) {
    if (request == null) {
      return null;
    }
    String currentRequestIdempotencyKey = requestIdempotencyKeyExtractor.apply(request);
    String resolvedKey =
        IdempotencyHeaderUtils.resolveBodyOrHeaderKey(
            currentRequestIdempotencyKey, idempotencyKeyHeader);
    if (!StringUtils.hasText(resolvedKey) || StringUtils.hasText(currentRequestIdempotencyKey)) {
      return request;
    }
    return requestWithIdempotencyKey.apply(request, resolvedKey);
  }
}
