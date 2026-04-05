package com.bigbrightpaints.erp.modules.accounting.controller;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bigbrightpaints.erp.core.audit.AuditEvent;
import com.bigbrightpaints.erp.core.audit.AuditService;
import com.bigbrightpaints.erp.core.exception.ApplicationException;
import com.bigbrightpaints.erp.core.exception.ErrorCode;
import com.bigbrightpaints.erp.core.util.CompanyClock;
import com.bigbrightpaints.erp.modules.accounting.domain.ReconciliationDiscrepancyStatus;
import com.bigbrightpaints.erp.modules.accounting.domain.ReconciliationDiscrepancyType;
import com.bigbrightpaints.erp.modules.accounting.dto.AgingSummaryResponse;
import com.bigbrightpaints.erp.modules.accounting.dto.BankReconciliationRequest;
import com.bigbrightpaints.erp.modules.accounting.dto.BankReconciliationSessionCompletionRequest;
import com.bigbrightpaints.erp.modules.accounting.dto.BankReconciliationSessionCreateRequest;
import com.bigbrightpaints.erp.modules.accounting.dto.BankReconciliationSessionDetailDto;
import com.bigbrightpaints.erp.modules.accounting.dto.BankReconciliationSessionItemsUpdateRequest;
import com.bigbrightpaints.erp.modules.accounting.dto.BankReconciliationSessionSummaryDto;
import com.bigbrightpaints.erp.modules.accounting.dto.BankReconciliationSummaryDto;
import com.bigbrightpaints.erp.modules.accounting.dto.GstReconciliationDto;
import com.bigbrightpaints.erp.modules.accounting.dto.GstReturnDto;
import com.bigbrightpaints.erp.modules.accounting.dto.InventoryRevaluationRequest;
import com.bigbrightpaints.erp.modules.accounting.dto.JournalEntryDto;
import com.bigbrightpaints.erp.modules.accounting.dto.LandedCostRequest;
import com.bigbrightpaints.erp.modules.accounting.dto.PartnerStatementResponse;
import com.bigbrightpaints.erp.modules.accounting.dto.ReconciliationDiscrepancyDto;
import com.bigbrightpaints.erp.modules.accounting.dto.ReconciliationDiscrepancyListResponse;
import com.bigbrightpaints.erp.modules.accounting.dto.ReconciliationDiscrepancyResolveRequest;
import com.bigbrightpaints.erp.modules.accounting.dto.SalesReturnPreviewDto;
import com.bigbrightpaints.erp.modules.accounting.dto.SalesReturnRequest;
import com.bigbrightpaints.erp.modules.accounting.dto.WipAdjustmentRequest;
import com.bigbrightpaints.erp.modules.accounting.service.AccountHierarchyService;
import com.bigbrightpaints.erp.modules.accounting.service.AccountingAuditService;
import com.bigbrightpaints.erp.modules.accounting.service.AccountingAuditTrailService;
import com.bigbrightpaints.erp.modules.accounting.service.AccountingFacade;
import com.bigbrightpaints.erp.modules.accounting.service.AccountingPeriodService;
import com.bigbrightpaints.erp.modules.accounting.service.AccountingService;
import com.bigbrightpaints.erp.modules.accounting.service.AgingReportService;
import com.bigbrightpaints.erp.modules.accounting.service.BankReconciliationSessionService;
import com.bigbrightpaints.erp.modules.accounting.service.CompanyDefaultAccountsService;
import com.bigbrightpaints.erp.modules.accounting.service.CreditDebitNoteService;
import com.bigbrightpaints.erp.modules.accounting.service.DealerReceiptService;
import com.bigbrightpaints.erp.modules.accounting.service.InventoryAccountingService;
import com.bigbrightpaints.erp.modules.accounting.service.JournalEntryService;
import com.bigbrightpaints.erp.modules.accounting.service.ReconciliationService;
import com.bigbrightpaints.erp.modules.accounting.service.SettlementService;
import com.bigbrightpaints.erp.modules.accounting.service.StatementService;
import com.bigbrightpaints.erp.modules.accounting.service.TaxService;
import com.bigbrightpaints.erp.modules.accounting.service.TemporalBalanceService;
import com.bigbrightpaints.erp.modules.company.domain.Company;
import com.bigbrightpaints.erp.modules.company.service.CompanyContextService;
import com.bigbrightpaints.erp.modules.sales.service.SalesReturnService;
import com.bigbrightpaints.erp.shared.dto.ApiResponse;
import com.bigbrightpaints.erp.shared.dto.PageResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/accounting")
public class AccountingController {
  private final AccountingService accountingService;
  private final JournalEntryService journalEntryService;
  private final DealerReceiptService dealerReceiptService;
  private final SettlementService settlementService;
  private final CreditDebitNoteService creditDebitNoteService;
  private final AccountingAuditService accountingAuditService;
  private final InventoryAccountingService inventoryAccountingService;
  private final AccountingFacade accountingFacade;
  private final SalesReturnService salesReturnService;
  private final AccountingPeriodService accountingPeriodService;
  private final ReconciliationService reconciliationService;
  private final StatementService statementService;
  private final TaxService taxService;
  private final TemporalBalanceService temporalBalanceService;
  private final AccountHierarchyService accountHierarchyService;
  private final AgingReportService agingReportService;
  private final CompanyDefaultAccountsService companyDefaultAccountsService;
  private final AccountingAuditTrailService accountingAuditTrailService;
  private final CompanyContextService companyContextService;
  private final CompanyClock companyClock;
  private final BankReconciliationSessionService bankReconciliationSessionService;
  private final AuditService auditService;

  @Autowired
  public AccountingController(
      AccountingService accountingService,
      JournalEntryService journalEntryService,
      DealerReceiptService dealerReceiptService,
      SettlementService settlementService,
      CreditDebitNoteService creditDebitNoteService,
      AccountingAuditService accountingAuditService,
      InventoryAccountingService inventoryAccountingService,
      AccountingFacade accountingFacade,
      SalesReturnService salesReturnService,
      AccountingPeriodService accountingPeriodService,
      ReconciliationService reconciliationService,
      StatementService statementService,
      TaxService taxService,
      TemporalBalanceService temporalBalanceService,
      AccountHierarchyService accountHierarchyService,
      AgingReportService agingReportService,
      CompanyDefaultAccountsService companyDefaultAccountsService,
      AccountingAuditTrailService accountingAuditTrailService,
      CompanyContextService companyContextService,
      CompanyClock companyClock,
      BankReconciliationSessionService bankReconciliationSessionService,
      AuditService auditService) {
    this.accountingService = accountingService;
    this.journalEntryService = journalEntryService;
    this.dealerReceiptService = dealerReceiptService;
    this.settlementService = settlementService;
    this.creditDebitNoteService = creditDebitNoteService;
    this.accountingAuditService = accountingAuditService;
    this.inventoryAccountingService = inventoryAccountingService;
    this.accountingFacade = accountingFacade;
    this.salesReturnService = salesReturnService;
    this.accountingPeriodService = accountingPeriodService;
    this.reconciliationService = reconciliationService;
    this.statementService = statementService;
    this.taxService = taxService;
    this.temporalBalanceService = temporalBalanceService;
    this.accountHierarchyService = accountHierarchyService;
    this.agingReportService = agingReportService;
    this.companyDefaultAccountsService = companyDefaultAccountsService;
    this.accountingAuditTrailService = accountingAuditTrailService;
    this.companyContextService = companyContextService;
    this.companyClock = companyClock;
    this.bankReconciliationSessionService = bankReconciliationSessionService;
    this.auditService = auditService;
  }

  /**
   * Keep accounting error payloads structured and explicit for UI diagnostics.
   */
  @ExceptionHandler(ApplicationException.class)
  public ResponseEntity<ApiResponse<Map<String, Object>>> handleApplicationException(
      ApplicationException ex, HttpServletRequest request) {
    if (usesMappedConcurrencyStatus(ex)) {
      return AccountingApplicationExceptionResponses.mappedStatus(ex, request);
    }
    return AccountingApplicationExceptionResponses.badRequest(ex, request);
  }

  private boolean usesMappedConcurrencyStatus(ApplicationException ex) {
    return ex != null
        && ex.getErrorCode() != null
        && ex.getErrorCode().getCode().startsWith("CONC_");
  }

  private ReconciliationDiscrepancyStatus parseDiscrepancyStatus(String rawStatus) {
    if (!StringUtils.hasText(rawStatus)) {
      return null;
    }
    try {
      return ReconciliationDiscrepancyStatus.valueOf(rawStatus.trim().toUpperCase());
    } catch (IllegalArgumentException ex) {
      throw new ApplicationException(
          ErrorCode.VALIDATION_INVALID_INPUT,
          "Invalid reconciliation discrepancy status: " + rawStatus,
          ex);
    }
  }

  private ReconciliationDiscrepancyType parseDiscrepancyType(String rawType) {
    if (!StringUtils.hasText(rawType)) {
      return null;
    }
    try {
      return ReconciliationDiscrepancyType.valueOf(rawType.trim().toUpperCase());
    } catch (IllegalArgumentException ex) {
      throw new ApplicationException(
          ErrorCode.VALIDATION_INVALID_INPUT,
          "Invalid reconciliation discrepancy type: " + rawType,
          ex);
    }
  }

  @GetMapping("/gst/return")
  @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')")
  public ResponseEntity<ApiResponse<GstReturnDto>> generateGstReturn(
      @RequestParam(required = false) String period) {
    YearMonth target = period != null && !period.isBlank() ? YearMonth.parse(period) : null;
    return ResponseEntity.ok(ApiResponse.success(taxService.generateGstReturn(target)));
  }

  @GetMapping("/gst/reconciliation")
  @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')")
  public ResponseEntity<ApiResponse<GstReconciliationDto>> getGstReconciliation(
      @RequestParam(required = false) String period) {
    YearMonth target = period != null && !period.isBlank() ? YearMonth.parse(period) : null;
    return ResponseEntity.ok(ApiResponse.success(taxService.generateGstReconciliation(target)));
  }

  @GetMapping("/sales/returns")
  @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING','ROLE_SALES')")
  public ResponseEntity<ApiResponse<List<JournalEntryDto>>> listSalesReturns() {
    List<JournalEntryDto> salesReturns =
        journalEntryService.listJournalEntriesByReferencePrefix("CRN-").stream()
            .filter(this::isSalesReturnCreditNote)
            .toList();
    return ResponseEntity.ok(ApiResponse.success("Sales returns", salesReturns));
  }

  @PostMapping("/sales/returns/preview")
  @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')")
  public ResponseEntity<ApiResponse<SalesReturnPreviewDto>> previewSalesReturn(
      @Valid @RequestBody SalesReturnRequest request) {
    return ResponseEntity.ok(
        ApiResponse.success("Sales return preview", salesReturnService.previewReturn(request)));
  }

  @PostMapping("/sales/returns")
  @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')")
  public ResponseEntity<ApiResponse<JournalEntryDto>> recordSalesReturn(
      @Valid @RequestBody SalesReturnRequest request) {
    return ResponseEntity.ok(
        ApiResponse.success("Credit note posted", salesReturnService.processReturn(request)));
  }

  private boolean isSalesReturnCreditNote(JournalEntryDto entry) {
    if (entry == null || !StringUtils.hasText(entry.referenceNumber())) {
      return false;
    }
    String normalizedReference = entry.referenceNumber().trim().toUpperCase();
    if (!normalizedReference.startsWith("CRN-") || normalizedReference.contains("-COGS-")) {
      return false;
    }
    return entry.dealerId() != null || "SALES_RETURN".equalsIgnoreCase(entry.correctionReason());
  }

  @PostMapping("/reconciliation/bank")
  @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')")
  public ResponseEntity<ApiResponse<BankReconciliationSummaryDto>> reconcileBank(
      @Valid @RequestBody BankReconciliationRequest request) {
    return ResponseEntity.ok(
        ApiResponse.success(bankReconciliationSessionService.reconcileLegacy(request)));
  }

  @PostMapping("/reconciliation/bank/sessions")
  @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')")
  public ResponseEntity<ApiResponse<BankReconciliationSessionSummaryDto>>
      startBankReconciliationSession(
          @Valid @RequestBody BankReconciliationSessionCreateRequest request) {
    return ResponseEntity.ok(
        ApiResponse.success(
            "Bank reconciliation session started",
            bankReconciliationSessionService.startSession(request)));
  }

  @PutMapping("/reconciliation/bank/sessions/{sessionId}/items")
  @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')")
  public ResponseEntity<ApiResponse<BankReconciliationSessionDetailDto>>
      updateBankReconciliationSessionItems(
          @PathVariable Long sessionId,
          @RequestBody BankReconciliationSessionItemsUpdateRequest request) {
    return ResponseEntity.ok(
        ApiResponse.success(
            "Bank reconciliation session updated",
            bankReconciliationSessionService.updateItems(sessionId, request)));
  }

  @PostMapping("/reconciliation/bank/sessions/{sessionId}/complete")
  @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')")
  public ResponseEntity<ApiResponse<BankReconciliationSessionDetailDto>>
      completeBankReconciliationSession(
          @PathVariable Long sessionId,
          @RequestBody(required = false) BankReconciliationSessionCompletionRequest request) {
    return ResponseEntity.ok(
        ApiResponse.success(
            "Bank reconciliation session completed",
            bankReconciliationSessionService.completeSession(sessionId, request)));
  }

  @GetMapping("/reconciliation/bank/sessions")
  @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')")
  public ResponseEntity<ApiResponse<PageResponse<BankReconciliationSessionSummaryDto>>>
      listBankReconciliationSessions(
          @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
    return ResponseEntity.ok(
        ApiResponse.success(bankReconciliationSessionService.listSessions(page, size)));
  }

  @GetMapping("/reconciliation/bank/sessions/{sessionId}")
  @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')")
  public ResponseEntity<ApiResponse<BankReconciliationSessionDetailDto>>
      getBankReconciliationSession(@PathVariable Long sessionId) {
    return ResponseEntity.ok(
        ApiResponse.success(bankReconciliationSessionService.getSessionDetail(sessionId)));
  }

  @GetMapping("/reconciliation/subledger")
  @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')")
  public ResponseEntity<ApiResponse<ReconciliationService.SubledgerReconciliationReport>>
      reconcileSubledger() {
    return ResponseEntity.ok(
        ApiResponse.success(reconciliationService.reconcileSubledgerBalances()));
  }

  @GetMapping("/reconciliation/discrepancies")
  @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')")
  public ResponseEntity<ApiResponse<ReconciliationDiscrepancyListResponse>>
      listReconciliationDiscrepancies(
          @RequestParam(required = false) String status,
          @RequestParam(required = false) String type) {
    ReconciliationDiscrepancyStatus statusFilter = parseDiscrepancyStatus(status);
    ReconciliationDiscrepancyType typeFilter = parseDiscrepancyType(type);
    return ResponseEntity.ok(
        ApiResponse.success(reconciliationService.listDiscrepancies(statusFilter, typeFilter)));
  }

  @PostMapping("/reconciliation/discrepancies/{discrepancyId}/resolve")
  @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')")
  public ResponseEntity<ApiResponse<ReconciliationDiscrepancyDto>> resolveReconciliationDiscrepancy(
      @PathVariable Long discrepancyId,
      @Valid @RequestBody ReconciliationDiscrepancyResolveRequest request) {
    return ResponseEntity.ok(
        ApiResponse.success(
            "Reconciliation discrepancy resolved",
            reconciliationService.resolveDiscrepancy(discrepancyId, request)));
  }

  @GetMapping("/reconciliation/inter-company")
  @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')")
  public ResponseEntity<ApiResponse<ReconciliationService.InterCompanyReconciliationReport>>
      reconcileInterCompany(
          @RequestParam("companyA") Long companyA, @RequestParam("companyB") Long companyB) {
    return ResponseEntity.ok(
        ApiResponse.success(reconciliationService.interCompanyReconcile(companyA, companyB)));
  }

  /* Statements & Aging */
  @GetMapping("/statements/suppliers/{supplierId}")
  @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')")
  public ResponseEntity<ApiResponse<PartnerStatementResponse>> supplierStatement(
      @PathVariable Long supplierId,
      @RequestParam(required = false) String from,
      @RequestParam(required = false) String to) {
    return ResponseEntity.ok(
        ApiResponse.success(
            statementService.supplierStatement(
                supplierId, parseOptionalDate(from, "from"), parseOptionalDate(to, "to"))));
  }

  @GetMapping("/aging/suppliers/{supplierId}")
  @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')")
  public ResponseEntity<ApiResponse<AgingSummaryResponse>> supplierAging(
      @PathVariable Long supplierId,
      @RequestParam(required = false) String asOf,
      @RequestParam(required = false) String buckets) {
    return ResponseEntity.ok(
        ApiResponse.success(
            statementService.supplierAging(supplierId, parseOptionalDate(asOf, "asOf"), buckets)));
  }

  @GetMapping(value = "/statements/suppliers/{supplierId}/pdf", produces = "application/pdf")
  @Operation(summary = "Download supplier statement PDF")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "200",
      description = "PDF document",
      content =
          @Content(
              mediaType = "application/pdf",
              schema = @Schema(type = "string", format = "binary")))
  @PreAuthorize("hasAuthority('ROLE_ADMIN')")
  public ResponseEntity<byte[]> supplierStatementPdf(
      @PathVariable Long supplierId,
      @RequestParam(required = false) String from,
      @RequestParam(required = false) String to) {
    byte[] pdf =
        statementService.supplierStatementPdf(
            supplierId, parseOptionalDate(from, "from"), parseOptionalDate(to, "to"));
    logAccountingExport("ACCOUNTING_SUPPLIER_STATEMENT", supplierId, "pdf");
    return ResponseEntity.ok()
        .header("Content-Disposition", "attachment; filename=supplier-statement.pdf")
        .body(pdf);
  }

  @GetMapping(value = "/aging/suppliers/{supplierId}/pdf", produces = "application/pdf")
  @Operation(summary = "Download supplier aging PDF")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "200",
      description = "PDF document",
      content =
          @Content(
              mediaType = "application/pdf",
              schema = @Schema(type = "string", format = "binary")))
  @PreAuthorize("hasAuthority('ROLE_ADMIN')")
  public ResponseEntity<byte[]> supplierAgingPdf(
      @PathVariable Long supplierId,
      @RequestParam(required = false) String asOf,
      @RequestParam(required = false) String buckets) {
    byte[] pdf =
        statementService.supplierAgingPdf(supplierId, parseOptionalDate(asOf, "asOf"), buckets);
    logAccountingExport("ACCOUNTING_SUPPLIER_AGING", supplierId, "pdf");
    return ResponseEntity.ok()
        .header("Content-Disposition", "attachment; filename=supplier-aging.pdf")
        .body(pdf);
  }

  /* Inventory valuation and WIP */
  @PostMapping("/inventory/landed-cost")
  @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')")
  public ResponseEntity<ApiResponse<JournalEntryDto>> recordLandedCost(
      @Valid @RequestBody LandedCostRequest request) {
    return ResponseEntity.ok(
        ApiResponse.success(
            "Landed cost posted", inventoryAccountingService.recordLandedCost(request)));
  }

  @PostMapping("/inventory/revaluation")
  @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')")
  public ResponseEntity<ApiResponse<JournalEntryDto>> revalueInventory(
      @Valid @RequestBody InventoryRevaluationRequest request) {
    return ResponseEntity.ok(
        ApiResponse.success(
            "Inventory revaluation posted", inventoryAccountingService.revalueInventory(request)));
  }

  @PostMapping("/inventory/wip-adjustment")
  @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')")
  public ResponseEntity<ApiResponse<JournalEntryDto>> adjustWip(
      @Valid @RequestBody WipAdjustmentRequest request) {
    return ResponseEntity.ok(
        ApiResponse.success(
            "WIP adjustment posted", inventoryAccountingService.adjustWip(request)));
  }

  // ==================== TEMPORAL QUERIES (Snapshots + Journal Lines) ====================

  @GetMapping("/accounts/{accountId}/balance/as-of")
  @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')")
  public ResponseEntity<ApiResponse<java.math.BigDecimal>> getBalanceAsOf(
      @PathVariable Long accountId, @RequestParam String date) {
    java.time.LocalDate asOfDate = parseRequiredDate(date, "date");
    return ResponseEntity.ok(
        ApiResponse.success(
            "Balance as of " + date,
            temporalBalanceService.getBalanceAsOfDate(accountId, asOfDate)));
  }

  @GetMapping("/trial-balance/as-of")
  @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')")
  public ResponseEntity<ApiResponse<TemporalBalanceService.TrialBalanceSnapshot>>
      getTrialBalanceAsOf(@RequestParam String date) {
    java.time.LocalDate asOfDate = parseRequiredDate(date, "date");
    return ResponseEntity.ok(
        ApiResponse.success(
            "Trial balance as of " + date, temporalBalanceService.getTrialBalanceAsOf(asOfDate)));
  }

  @GetMapping("/accounts/{accountId}/activity")
  @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')")
  public ResponseEntity<ApiResponse<TemporalBalanceService.AccountActivityReport>>
      getAccountActivity(
          @PathVariable Long accountId,
          @RequestParam(required = false) String startDate,
          @RequestParam(required = false) String endDate,
          @RequestParam(required = false) String from,
          @RequestParam(required = false) String to) {
    String resolvedStart = StringUtils.hasText(startDate) ? startDate : from;
    String resolvedEnd = StringUtils.hasText(endDate) ? endDate : to;
    if (!StringUtils.hasText(resolvedStart) || !StringUtils.hasText(resolvedEnd)) {
      throw new ApplicationException(
          ErrorCode.VALIDATION_MISSING_REQUIRED_FIELD,
          "Account activity requires startDate/endDate (or from/to) query parameters");
    }
    LocalDate start;
    LocalDate end;
    try {
      start = parseRequiredDate(resolvedStart, "startDate");
      end = parseRequiredDate(resolvedEnd, "endDate");
    } catch (ApplicationException ex) {
      throw new ApplicationException(
              ErrorCode.VALIDATION_INVALID_DATE,
              "Invalid account activity date format; expected ISO date yyyy-MM-dd")
          .withDetail("startDate", resolvedStart)
          .withDetail("endDate", resolvedEnd);
    }
    return ResponseEntity.ok(
        ApiResponse.success(
            "Account activity report",
            temporalBalanceService.getAccountActivity(accountId, start, end)));
  }

  @GetMapping("/date-context")
  @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')")
  public ResponseEntity<ApiResponse<Map<String, Object>>> getAccountingDateContext() {
    Company company = companyContextService.requireCurrentCompany();
    LocalDate today = companyClock.today(company);
    Instant now = companyClock.now(company);
    Map<String, Object> payload = new HashMap<>();
    payload.put("companyId", company != null ? company.getId() : null);
    payload.put("companyCode", company != null ? company.getCode() : null);
    payload.put("timezone", company != null ? company.getTimezone() : null);
    payload.put("today", today);
    payload.put("now", now);
    return ResponseEntity.ok(ApiResponse.success("Accounting date context", payload));
  }

  @GetMapping("/accounts/{accountId}/balance/compare")
  @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')")
  public ResponseEntity<ApiResponse<TemporalBalanceService.BalanceComparison>> compareBalances(
      @PathVariable Long accountId, @RequestParam String date1, @RequestParam String date2) {
    return ResponseEntity.ok(
        ApiResponse.success(
            "Balance comparison",
            temporalBalanceService.compareBalances(
                accountId, parseRequiredDate(date1, "date1"), parseRequiredDate(date2, "date2"))));
  }

  private LocalDate parseOptionalDate(String rawDate, String fieldName) {
    if (!StringUtils.hasText(rawDate)) {
      return null;
    }
    return parseRequiredDate(rawDate, fieldName);
  }

  private LocalDate parseRequiredDate(String rawDate, String fieldName) {
    try {
      return LocalDate.parse(rawDate.trim());
    } catch (DateTimeParseException ex) {
      throw new ApplicationException(
              ErrorCode.VALIDATION_INVALID_DATE,
              "Invalid " + fieldName + " date format; expected ISO date yyyy-MM-dd")
          .withDetail(fieldName, rawDate);
    }
  }

  private void logAccountingExport(String resourceType, Long resourceId, String format) {
    if (auditService == null) {
      return;
    }
    Map<String, String> metadata = new HashMap<>();
    metadata.put("resourceType", resourceType);
    metadata.put("resourceId", resourceId != null ? resourceId.toString() : "");
    metadata.put("operation", "EXPORT");
    metadata.put("format", format);
    auditService.logSuccess(AuditEvent.DATA_EXPORT, metadata);
  }
}
