package com.bigbrightpaints.erp.modules.accounting.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bigbrightpaints.erp.core.security.SensitiveDisclosurePolicyOwner;
import com.bigbrightpaints.erp.core.util.IdempotencyHeaderUtils;
import com.bigbrightpaints.erp.modules.accounting.dto.AutoSettlementRequest;
import com.bigbrightpaints.erp.modules.accounting.dto.DealerReceiptRequest;
import com.bigbrightpaints.erp.modules.accounting.dto.DealerReceiptSplitRequest;
import com.bigbrightpaints.erp.modules.accounting.dto.JournalEntryDto;
import com.bigbrightpaints.erp.modules.accounting.dto.PartnerSettlementRequest;
import com.bigbrightpaints.erp.modules.accounting.dto.PartnerSettlementResponse;
import com.bigbrightpaints.erp.modules.accounting.service.AccountingFacade;
import com.bigbrightpaints.erp.shared.dto.ApiResponse;

import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/accounting")
public class SettlementController {

  private final AccountingFacade accountingFacade;

  public SettlementController(AccountingFacade accountingFacade) {
    this.accountingFacade = accountingFacade;
  }

  @PostMapping("/receipts/dealer")
  @PreAuthorize(SensitiveDisclosurePolicyOwner.REPORT_OR_ACCOUNTING_ONLY)
  public ResponseEntity<ApiResponse<JournalEntryDto>> recordDealerReceipt(
      @Valid @RequestBody DealerReceiptRequest request,
      @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
      @Parameter(hidden = true) @RequestHeader(value = "X-Idempotency-Key", required = false)
          String retiredIdempotencyKey) {
    return ResponseEntity.ok(
        ApiResponse.success(
            "Receipt recorded",
            accountingFacade.recordDealerReceipt(
                applyHeaderOnlyIdempotencyKey(
                    request,
                    SettlementRequestCopies::dealerReceipt,
                    idempotencyKey,
                    retiredIdempotencyKey,
                    "dealer receipts",
                    "/api/v1/accounting/receipts/dealer"))));
  }

  @PostMapping("/receipts/dealer/hybrid")
  @PreAuthorize(SensitiveDisclosurePolicyOwner.REPORT_OR_ACCOUNTING_ONLY)
  public ResponseEntity<ApiResponse<JournalEntryDto>> recordDealerHybridReceipt(
      @Valid @RequestBody DealerReceiptSplitRequest request,
      @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
      @Parameter(hidden = true) @RequestHeader(value = "X-Idempotency-Key", required = false)
          String retiredIdempotencyKey) {
    return ResponseEntity.ok(
        ApiResponse.success(
            "Receipt recorded",
            accountingFacade.recordDealerReceiptSplit(
                applyHeaderOnlyIdempotencyKey(
                    request,
                    SettlementRequestCopies::dealerReceiptSplit,
                    idempotencyKey,
                    retiredIdempotencyKey,
                    "dealer hybrid receipts",
                    "/api/v1/accounting/receipts/dealer/hybrid"))));
  }

  @PostMapping("/settlements/dealers")
  @PreAuthorize(SensitiveDisclosurePolicyOwner.REPORT_OR_ACCOUNTING_ONLY)
  public ResponseEntity<ApiResponse<PartnerSettlementResponse>> settleDealer(
      @Valid @RequestBody PartnerSettlementRequest request,
      @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
      @Parameter(hidden = true) @RequestHeader(value = "X-Idempotency-Key", required = false)
          String retiredIdempotencyKey) {
    return ResponseEntity.ok(
        ApiResponse.success(
            "Settlement recorded",
            accountingFacade.settleDealerInvoices(
                applyHeaderOnlyIdempotencyKey(
                    request,
                    SettlementRequestCopies::partnerSettlement,
                    idempotencyKey,
                    retiredIdempotencyKey,
                    "dealer settlements",
                    "/api/v1/accounting/settlements/dealers"))));
  }

  @PostMapping("/dealers/{dealerId}/auto-settle")
  @PreAuthorize(SensitiveDisclosurePolicyOwner.REPORT_OR_ACCOUNTING_ONLY)
  public ResponseEntity<ApiResponse<PartnerSettlementResponse>> autoSettleDealer(
      @PathVariable Long dealerId,
      @Valid @RequestBody AutoSettlementRequest request,
      @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
      @Parameter(hidden = true) @RequestHeader(value = "X-Idempotency-Key", required = false)
          String retiredIdempotencyKey) {
    return ResponseEntity.ok(
        ApiResponse.success(
            "Auto-settlement recorded",
            accountingFacade.autoSettleDealer(
                dealerId,
                applyHeaderOnlyIdempotencyKey(
                    request,
                    SettlementRequestCopies::autoSettlement,
                    idempotencyKey,
                    retiredIdempotencyKey,
                    "dealer auto-settlements",
                    "/api/v1/accounting/dealers/{dealerId}/auto-settle"))));
  }

  @PostMapping("/settlements/suppliers")
  @PreAuthorize(SensitiveDisclosurePolicyOwner.REPORT_OR_ACCOUNTING_ONLY)
  public ResponseEntity<ApiResponse<PartnerSettlementResponse>> settleSupplier(
      @Valid @RequestBody PartnerSettlementRequest request,
      @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
      @Parameter(hidden = true) @RequestHeader(value = "X-Idempotency-Key", required = false)
          String retiredIdempotencyKey) {
    return ResponseEntity.ok(
        ApiResponse.success(
            "Settlement recorded",
            accountingFacade.settleSupplierInvoices(
                applyHeaderOnlyIdempotencyKey(
                    request,
                    SettlementRequestCopies::partnerSettlement,
                    idempotencyKey,
                    retiredIdempotencyKey,
                    "supplier settlements",
                    "/api/v1/accounting/settlements/suppliers"))));
  }

  @PostMapping("/suppliers/{supplierId}/auto-settle")
  @PreAuthorize(SensitiveDisclosurePolicyOwner.REPORT_OR_ACCOUNTING_ONLY)
  public ResponseEntity<ApiResponse<PartnerSettlementResponse>> autoSettleSupplier(
      @PathVariable Long supplierId,
      @Valid @RequestBody AutoSettlementRequest request,
      @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
      @Parameter(hidden = true) @RequestHeader(value = "X-Idempotency-Key", required = false)
          String retiredIdempotencyKey) {
    return ResponseEntity.ok(
        ApiResponse.success(
            "Auto-settlement recorded",
            accountingFacade.autoSettleSupplier(
                supplierId,
                applyHeaderOnlyIdempotencyKey(
                    request,
                    SettlementRequestCopies::autoSettlement,
                    idempotencyKey,
                    retiredIdempotencyKey,
                    "supplier auto-settlements",
                    "/api/v1/accounting/suppliers/{supplierId}/auto-settle"))));
  }

  private <T> T applyHeaderOnlyIdempotencyKey(
      T request,
      java.util.function.BiFunction<T, String, T> requestWithIdempotencyKey,
      String idempotencyKeyHeader,
      String retiredIdempotencyKey,
      String resourceDescription,
      String canonicalPath) {
    rejectRetiredHeader(retiredIdempotencyKey, resourceDescription, canonicalPath);
    if (request == null) {
      return null;
    }
    String canonicalHeaderKey = IdempotencyHeaderUtils.resolveHeaderKey(idempotencyKeyHeader);
    if (canonicalHeaderKey == null) {
      return request;
    }
    return requestWithIdempotencyKey.apply(request, canonicalHeaderKey);
  }

  private void rejectRetiredHeader(
      String retiredIdempotencyKey, String resourceDescription, String canonicalPath) {
    if (IdempotencyHeaderUtils.resolveHeaderKey(retiredIdempotencyKey) != null) {
      throw IdempotencyHeaderUtils.unsupportedRetiredHeader(
          "X-Idempotency-Key", resourceDescription, canonicalPath);
    }
  }
}
