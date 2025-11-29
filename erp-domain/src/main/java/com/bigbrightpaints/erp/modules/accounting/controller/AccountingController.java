package com.bigbrightpaints.erp.modules.accounting.controller;

import com.bigbrightpaints.erp.modules.accounting.dto.*;
import com.bigbrightpaints.erp.modules.accounting.service.AccountingPeriodService;
import com.bigbrightpaints.erp.modules.accounting.service.AccountingService;
import com.bigbrightpaints.erp.modules.accounting.service.ReconciliationService;
import com.bigbrightpaints.erp.modules.accounting.service.TaxService;
import com.bigbrightpaints.erp.modules.accounting.service.StatementService;
import com.bigbrightpaints.erp.core.exception.ApplicationException;
import com.bigbrightpaints.erp.modules.sales.service.SalesReturnService;
import com.bigbrightpaints.erp.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.List;
import java.time.YearMonth;

@RestController
@RequestMapping("/api/v1/accounting")
public class AccountingController {

    private final AccountingService accountingService;
    private final SalesReturnService salesReturnService;
    private final AccountingPeriodService accountingPeriodService;
    private final ReconciliationService reconciliationService;
    private final StatementService statementService;
    private final TaxService taxService;

    public AccountingController(AccountingService accountingService,
                                SalesReturnService salesReturnService,
                                AccountingPeriodService accountingPeriodService,
                                ReconciliationService reconciliationService,
                                StatementService statementService,
                                TaxService taxService) {
        this.accountingService = accountingService;
        this.salesReturnService = salesReturnService;
        this.accountingPeriodService = accountingPeriodService;
        this.reconciliationService = reconciliationService;
        this.statementService = statementService;
        this.taxService = taxService;
    }

    /**
     * Translate business exceptions to 400 for API clients (prevents 500 on validation/state errors).
     */
    @ExceptionHandler(ApplicationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleApplicationException(ApplicationException ex) {
        return ApiResponse.failure(ex.getUserMessage(), null);
    }

    @GetMapping("/accounts")\n@PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')")
    public ResponseEntity<ApiResponse<List<AccountDto>>> accounts() {
        return ResponseEntity.ok(ApiResponse.success(accountingService.listAccounts()));
    }

    @PostMapping("/accounts")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')")
    public ResponseEntity<ApiResponse<AccountDto>> createAccount(@Valid @RequestBody AccountRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Account created", accountingService.createAccount(request)));
    }

    @GetMapping("/journal-entries")\n@PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')")
    public ResponseEntity<ApiResponse<List<JournalEntryDto>>> journalEntries(@RequestParam(required = false) Long dealerId,
                                                                             @RequestParam(defaultValue = "0") int page,
                                                                             @RequestParam(defaultValue = "100") int size) {
        return ResponseEntity.ok(ApiResponse.success(accountingService.listJournalEntries(dealerId, page, size)));
    }

    @PostMapping("/journal-entries")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')")
    public ResponseEntity<ApiResponse<JournalEntryDto>> createJournalEntry(@Valid @RequestBody JournalEntryRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Journal entry posted", accountingService.createJournalEntry(request)));
    }

    @PostMapping("/journal-entries/{entryId}/reverse")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')")
    public ResponseEntity<ApiResponse<JournalEntryDto>> reverseJournalEntry(@PathVariable Long entryId,
                                                                            @RequestBody(required = false) JournalEntryReversalRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Journal entry corrected", accountingService.reverseJournalEntry(entryId, request)));
    }

    @PostMapping("/receipts/dealer")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')")
    public ResponseEntity<ApiResponse<JournalEntryDto>> recordDealerReceipt(@Valid @RequestBody DealerReceiptRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Receipt recorded", accountingService.recordDealerReceipt(request)));
    }

    @PostMapping("/settlements/dealers")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')")
    public ResponseEntity<ApiResponse<PartnerSettlementResponse>> settleDealer(@Valid @RequestBody DealerSettlementRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Settlement recorded", accountingService.settleDealerInvoices(request)));
    }

    @PostMapping("/payroll/payments")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')")
    public ResponseEntity<ApiResponse<JournalEntryDto>> recordPayrollPayment(@Valid @RequestBody PayrollPaymentRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Payroll payment recorded", accountingService.recordPayrollPayment(request)));
    }

    @PostMapping("/suppliers/payments")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')")
    public ResponseEntity<ApiResponse<JournalEntryDto>> recordSupplierPayment(@Valid @RequestBody SupplierPaymentRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Supplier payment recorded", accountingService.recordSupplierPayment(request)));
    }

    @PostMapping("/settlements/suppliers")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')")
    public ResponseEntity<ApiResponse<PartnerSettlementResponse>> settleSupplier(@Valid @RequestBody SupplierSettlementRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Settlement recorded", accountingService.settleSupplierInvoices(request)));
    }

    @PostMapping("/credit-notes")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')")
    public ResponseEntity<ApiResponse<JournalEntryDto>> postCreditNote(@Valid @RequestBody CreditNoteRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Credit note posted", accountingService.postCreditNote(request)));
    }

    @PostMapping("/debit-notes")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')")
    public ResponseEntity<ApiResponse<JournalEntryDto>> postDebitNote(@Valid @RequestBody DebitNoteRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Debit note posted", accountingService.postDebitNote(request)));
    }

    @PostMapping("/accruals")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')")
    public ResponseEntity<ApiResponse<JournalEntryDto>> postAccrual(@Valid @RequestBody AccrualRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Accrual posted", accountingService.postAccrual(request)));
    }

    @GetMapping("/gst/return")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')")
    public ResponseEntity<ApiResponse<GstReturnDto>> generateGstReturn(@RequestParam(required = false) String period) {
        YearMonth target = period != null && !period.isBlank() ? YearMonth.parse(period) : null;
        return ResponseEntity.ok(ApiResponse.success(taxService.generateGstReturn(target)));
    }

    @PostMapping("/bad-debts/write-off")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')")
    public ResponseEntity<ApiResponse<JournalEntryDto>> writeOffBadDebt(@Valid @RequestBody BadDebtWriteOffRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Bad debt written off", accountingService.writeOffBadDebt(request)));
    }

    @PostMapping("/sales/returns")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')")
    public ResponseEntity<ApiResponse<JournalEntryDto>> recordSalesReturn(@Valid @RequestBody SalesReturnRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Credit note posted", salesReturnService.processReturn(request)));
    }

    @GetMapping("/periods")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')")
    public ResponseEntity<ApiResponse<List<AccountingPeriodDto>>> listPeriods() {
        return ResponseEntity.ok(ApiResponse.success(accountingPeriodService.listPeriods()));
    }

    @PostMapping("/periods/{periodId}/close")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')")
    public ResponseEntity<ApiResponse<AccountingPeriodDto>> closePeriod(@PathVariable Long periodId,
                                                                        @RequestBody(required = false) AccountingPeriodCloseRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Accounting period closed", accountingPeriodService.closePeriod(periodId, request)));
    }

    @PostMapping("/periods/{periodId}/lock")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')")
    public ResponseEntity<ApiResponse<AccountingPeriodDto>> lockPeriod(@PathVariable Long periodId,
                                                                       @RequestBody(required = false) AccountingPeriodLockRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Accounting period locked", accountingPeriodService.lockPeriod(periodId, request)));
    }

    @PostMapping("/periods/{periodId}/reopen")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')")
    public ResponseEntity<ApiResponse<AccountingPeriodDto>> reopenPeriod(@PathVariable Long periodId,
                                                                         @RequestBody(required = false) AccountingPeriodReopenRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Accounting period reopened", accountingPeriodService.reopenPeriod(periodId, request)));
    }

    @PostMapping("/bank-reconciliation")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')")
    public ResponseEntity<ApiResponse<BankReconciliationSummaryDto>> reconcileBank(@Valid @RequestBody BankReconciliationRequest request) {
        return ResponseEntity.ok(ApiResponse.success(reconciliationService.reconcileBank(request)));
    }

    @PostMapping("/inventory/physical-count")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')")
    public ResponseEntity<ApiResponse<InventoryCountResponse>> recordInventoryCount(@Valid @RequestBody InventoryCountRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Inventory count recorded", reconciliationService.recordInventoryCount(request)));
    }

    @GetMapping("/month-end/checklist")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')")
    public ResponseEntity<ApiResponse<MonthEndChecklistDto>> checklist(@RequestParam(required = false) Long periodId) {
        return ResponseEntity.ok(ApiResponse.success(accountingPeriodService.getMonthEndChecklist(periodId)));
    }

    @PostMapping("/month-end/checklist/{periodId}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')")
    public ResponseEntity<ApiResponse<MonthEndChecklistDto>> updateChecklist(@PathVariable Long periodId,
                                                                             @RequestBody MonthEndChecklistUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Checklist updated", accountingPeriodService.updateMonthEndChecklist(periodId, request)));
    }

    /* Statements & Aging */
    @GetMapping("/statements/dealers/{dealerId}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')")
    public ResponseEntity<ApiResponse<PartnerStatementResponse>> dealerStatement(@PathVariable Long dealerId,
                                                                                 @RequestParam(required = false) String from,
                                                                                 @RequestParam(required = false) String to) {
        return ResponseEntity.ok(ApiResponse.success(
                statementService.dealerStatement(dealerId,
                        from != null ? java.time.LocalDate.parse(from) : null,
                        to != null ? java.time.LocalDate.parse(to) : null)));
    }

    @GetMapping("/statements/suppliers/{supplierId}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')")
    public ResponseEntity<ApiResponse<PartnerStatementResponse>> supplierStatement(@PathVariable Long supplierId,
                                                                                   @RequestParam(required = false) String from,
                                                                                   @RequestParam(required = false) String to) {
        return ResponseEntity.ok(ApiResponse.success(
                statementService.supplierStatement(supplierId,
                        from != null ? java.time.LocalDate.parse(from) : null,
                        to != null ? java.time.LocalDate.parse(to) : null)));
    }

    @GetMapping("/aging/dealers/{dealerId}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')")
    public ResponseEntity<ApiResponse<AgingSummaryResponse>> dealerAging(@PathVariable Long dealerId,
                                                                         @RequestParam(required = false) String asOf,
                                                                         @RequestParam(required = false) String buckets) {
        return ResponseEntity.ok(ApiResponse.success(
                statementService.dealerAging(dealerId,
                        asOf != null ? java.time.LocalDate.parse(asOf) : null,
                        buckets)));
    }

    @GetMapping("/aging/suppliers/{supplierId}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')")
    public ResponseEntity<ApiResponse<AgingSummaryResponse>> supplierAging(@PathVariable Long supplierId,
                                                                           @RequestParam(required = false) String asOf,
                                                                           @RequestParam(required = false) String buckets) {
        return ResponseEntity.ok(ApiResponse.success(
                statementService.supplierAging(supplierId,
                        asOf != null ? java.time.LocalDate.parse(asOf) : null,
                        buckets)));
    }

    @GetMapping(value = "/statements/dealers/{dealerId}/pdf", produces = "application/pdf")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')")
    public ResponseEntity<byte[]> dealerStatementPdf(@PathVariable Long dealerId,
                                                     @RequestParam(required = false) String from,
                                                     @RequestParam(required = false) String to) {
        byte[] pdf = statementService.dealerStatementPdf(dealerId,
                from != null ? java.time.LocalDate.parse(from) : null,
                to != null ? java.time.LocalDate.parse(to) : null);
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=dealer-statement.pdf")
                .body(pdf);
    }

    @GetMapping(value = "/statements/suppliers/{supplierId}/pdf", produces = "application/pdf")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')")
    public ResponseEntity<byte[]> supplierStatementPdf(@PathVariable Long supplierId,
                                                       @RequestParam(required = false) String from,
                                                       @RequestParam(required = false) String to) {
        byte[] pdf = statementService.supplierStatementPdf(supplierId,
                from != null ? java.time.LocalDate.parse(from) : null,
                to != null ? java.time.LocalDate.parse(to) : null);
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=supplier-statement.pdf")
                .body(pdf);
    }

    @GetMapping(value = "/aging/dealers/{dealerId}/pdf", produces = "application/pdf")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')")
    public ResponseEntity<byte[]> dealerAgingPdf(@PathVariable Long dealerId,
                                                 @RequestParam(required = false) String asOf,
                                                 @RequestParam(required = false) String buckets) {
        byte[] pdf = statementService.dealerAgingPdf(dealerId,
                asOf != null ? java.time.LocalDate.parse(asOf) : null,
                buckets);
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=dealer-aging.pdf")
                .body(pdf);
    }

    @GetMapping(value = "/aging/suppliers/{supplierId}/pdf", produces = "application/pdf")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')")
    public ResponseEntity<byte[]> supplierAgingPdf(@PathVariable Long supplierId,
                                                   @RequestParam(required = false) String asOf,
                                                   @RequestParam(required = false) String buckets) {
        byte[] pdf = statementService.supplierAgingPdf(supplierId,
                asOf != null ? java.time.LocalDate.parse(asOf) : null,
                buckets);
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=supplier-aging.pdf")
                .body(pdf);
    }

    /* Inventory valuation and WIP */
    @PostMapping("/inventory/landed-cost")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')")
    public ResponseEntity<ApiResponse<JournalEntryDto>> recordLandedCost(@Valid @RequestBody LandedCostRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Landed cost posted", accountingService.recordLandedCost(request)));
    }

    @PostMapping("/inventory/revaluation")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')")
    public ResponseEntity<ApiResponse<JournalEntryDto>> revalueInventory(@Valid @RequestBody InventoryRevaluationRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Inventory revaluation posted", accountingService.revalueInventory(request)));
    }

    @PostMapping("/inventory/wip-adjustment")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')")
    public ResponseEntity<ApiResponse<JournalEntryDto>> adjustWip(@Valid @RequestBody WipAdjustmentRequest request) {
        return ResponseEntity.ok(ApiResponse.success("WIP adjustment posted", accountingService.adjustWip(request)));
    }

    /* Audit digest */
    @GetMapping("/audit/digest")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')")
    public ResponseEntity<ApiResponse<AuditDigestResponse>> auditDigest(@RequestParam(required = false) String from,
                                                                        @RequestParam(required = false) String to) {
        return ResponseEntity.ok(ApiResponse.success(
                accountingService.auditDigest(
                        from != null ? java.time.LocalDate.parse(from) : null,
                        to != null ? java.time.LocalDate.parse(to) : null)));
    }

    @GetMapping(value = "/audit/digest.csv", produces = "text/csv")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')")
    public ResponseEntity<String> auditDigestCsv(@RequestParam(required = false) String from,
                                                 @RequestParam(required = false) String to) {
        String csv = accountingService.auditDigestCsv(
                from != null ? java.time.LocalDate.parse(from) : null,
                to != null ? java.time.LocalDate.parse(to) : null);
        return ResponseEntity.ok(csv);
    }
}
