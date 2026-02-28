package com.bigbrightpaints.erp.modules.accounting.service;

import com.bigbrightpaints.erp.core.exception.ApplicationException;
import com.bigbrightpaints.erp.core.exception.ErrorCode;
import com.bigbrightpaints.erp.core.util.CompanyClock;
import com.bigbrightpaints.erp.core.util.MoneyUtils;
import com.bigbrightpaints.erp.modules.accounting.domain.JournalLine;
import com.bigbrightpaints.erp.modules.accounting.domain.JournalLineRepository;
import com.bigbrightpaints.erp.modules.accounting.dto.GstReconciliationDto;
import com.bigbrightpaints.erp.modules.accounting.dto.GstReturnDto;
import com.bigbrightpaints.erp.modules.company.domain.Company;
import com.bigbrightpaints.erp.modules.company.service.CompanyContextService;
import com.bigbrightpaints.erp.modules.invoice.domain.Invoice;
import com.bigbrightpaints.erp.modules.invoice.domain.InvoiceLine;
import com.bigbrightpaints.erp.modules.invoice.domain.InvoiceRepository;
import com.bigbrightpaints.erp.modules.purchasing.domain.RawMaterialPurchase;
import com.bigbrightpaints.erp.modules.purchasing.domain.RawMaterialPurchaseLine;
import com.bigbrightpaints.erp.modules.purchasing.domain.RawMaterialPurchaseRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(Transactional.TxType.SUPPORTS)
public class TaxService {

    private final CompanyContextService companyContextService;
    private final CompanyAccountingSettingsService companyAccountingSettingsService;
    private final CompanyClock companyClock;
    private final JournalLineRepository journalLineRepository;
    private final GstService gstService;
    private final InvoiceRepository invoiceRepository;
    private final RawMaterialPurchaseRepository rawMaterialPurchaseRepository;

    public TaxService(CompanyContextService companyContextService,
                      CompanyAccountingSettingsService companyAccountingSettingsService,
                      CompanyClock companyClock,
                      JournalLineRepository journalLineRepository,
                      GstService gstService,
                      InvoiceRepository invoiceRepository,
                      RawMaterialPurchaseRepository rawMaterialPurchaseRepository) {
        this.companyContextService = companyContextService;
        this.companyAccountingSettingsService = companyAccountingSettingsService;
        this.companyClock = companyClock;
        this.journalLineRepository = journalLineRepository;
        this.gstService = gstService;
        this.invoiceRepository = invoiceRepository;
        this.rawMaterialPurchaseRepository = rawMaterialPurchaseRepository;
    }

    public GstReturnDto generateGstReturn(YearMonth period) {
        Company company = companyContextService.requireCurrentCompany();
        YearMonth target = resolvePeriod(company, period);
        LocalDate start = target.atDay(1);
        LocalDate end = target.atEndOfMonth();

        if (isNonGstMode(company)) {
            ensureNonGstCompanyDoesNotCarryGstAccounts(company);
            return buildGstReturn(target, start, end, BigDecimal.ZERO, BigDecimal.ZERO);
        }

        var taxConfig = companyAccountingSettingsService.requireTaxAccounts();

        BigDecimal outputTaxBalance = sumTax(company, taxConfig.outputTaxAccountId(), start, end, true);
        BigDecimal inputTaxBalance = sumTax(company, taxConfig.inputTaxAccountId(), start, end, false);

        BigDecimal outputTax = MoneyUtils.roundCurrency(
                positivePortion(outputTaxBalance).add(positivePortion(inputTaxBalance.negate())));
        BigDecimal inputTax = MoneyUtils.roundCurrency(
                positivePortion(inputTaxBalance).add(positivePortion(outputTaxBalance.negate())));

        return buildGstReturn(target, start, end, outputTax, inputTax);
    }

    public GstReconciliationDto generateGstReconciliation(YearMonth period) {
        Company company = companyContextService.requireCurrentCompany();
        YearMonth target = resolvePeriod(company, period);
        LocalDate start = target.atDay(1);
        LocalDate end = target.atEndOfMonth();

        GstReconciliationDto dto = new GstReconciliationDto();
        dto.setPeriod(target);
        dto.setPeriodStart(start);
        dto.setPeriodEnd(end);

        if (isNonGstMode(company)) {
            ensureNonGstCompanyDoesNotCarryGstAccounts(company);
            dto.setCollected(componentSummary(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));
            dto.setInputTaxCredit(componentSummary(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));
            dto.setNetLiability(componentSummary(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));
            return dto;
        }

        ComponentTotals collected = collectOutputTax(company, start, end);
        ComponentTotals inputCredit = collectInputTaxCredit(company, start, end);

        dto.setCollected(componentSummary(collected.cgst, collected.sgst, collected.igst));
        dto.setInputTaxCredit(componentSummary(inputCredit.cgst, inputCredit.sgst, inputCredit.igst));
        dto.setNetLiability(componentSummary(
                collected.cgst.subtract(inputCredit.cgst),
                collected.sgst.subtract(inputCredit.sgst),
                collected.igst.subtract(inputCredit.igst)));
        return dto;
    }

    private YearMonth resolvePeriod(Company company, YearMonth period) {
        YearMonth target = period != null ? period : YearMonth.from(companyClock.today(company));
        if (period != null) {
            YearMonth currentPeriod = YearMonth.from(companyClock.today(company));
            if (target.isAfter(currentPeriod)) {
                throw new ApplicationException(
                        ErrorCode.VALIDATION_INVALID_DATE,
                        "GST return period cannot be in the future")
                        .withDetail("requestedPeriod", target.toString())
                        .withDetail("currentPeriod", currentPeriod.toString());
            }
        }
        return target;
    }

    private BigDecimal sumTax(Company company, Long accountId, LocalDate start, LocalDate end, boolean outputTax) {
        if (accountId == null) {
            return BigDecimal.ZERO;
        }
        List<JournalLine> lines = journalLineRepository.findLinesForAccountBetween(company, accountId, start, end);
        if (lines == null || lines.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal total = BigDecimal.ZERO;
        for (JournalLine line : lines) {
            BigDecimal debit = safe(line.getDebit());
            BigDecimal credit = safe(line.getCredit());
            BigDecimal delta = outputTax ? credit.subtract(debit) : debit.subtract(credit);
            total = total.add(delta);
        }
        return total;
    }

    private BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal positivePortion(BigDecimal value) {
        return value.compareTo(BigDecimal.ZERO) > 0 ? value : BigDecimal.ZERO;
    }

    private ComponentTotals collectOutputTax(Company company, LocalDate start, LocalDate end) {
        List<Invoice> invoices = invoiceRepository.findByCompanyAndIssueDateBetweenOrderByIssueDateAsc(company, start, end);
        ComponentTotals totals = new ComponentTotals();
        for (Invoice invoice : invoices) {
            if (!isIncludedInvoiceStatus(invoice.getStatus())) {
                continue;
            }
            for (InvoiceLine line : invoice.getLines()) {
                GstService.GstBreakdown breakdown = resolveInvoiceLineBreakdown(company, invoice, line);
                totals.add(breakdown);
            }
        }
        return totals;
    }

    private ComponentTotals collectInputTaxCredit(Company company, LocalDate start, LocalDate end) {
        List<RawMaterialPurchase> purchases = rawMaterialPurchaseRepository
                .findByCompanyAndInvoiceDateBetweenOrderByInvoiceDateAsc(company, start, end);
        ComponentTotals totals = new ComponentTotals();
        for (RawMaterialPurchase purchase : purchases) {
            if (!isIncludedPurchaseStatus(purchase.getStatus())) {
                continue;
            }
            for (RawMaterialPurchaseLine line : purchase.getLines()) {
                GstService.GstBreakdown breakdown = resolvePurchaseLineBreakdown(company, purchase, line);
                totals.add(breakdown);
            }
        }
        return totals;
    }

    private GstService.GstBreakdown resolveInvoiceLineBreakdown(Company company, Invoice invoice, InvoiceLine line) {
        BigDecimal taxAmount = safe(line.getTaxAmount());
        if (taxAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return new GstService.GstBreakdown(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                    GstService.TaxType.INTER_STATE);
        }
        BigDecimal cgst = safe(line.getCgstAmount());
        BigDecimal sgst = safe(line.getSgstAmount());
        BigDecimal igst = safe(line.getIgstAmount());
        if (cgst.add(sgst).add(igst).compareTo(BigDecimal.ZERO) > 0) {
            return new GstService.GstBreakdown(
                    taxableAmount(line.getTaxableAmount(), line.getLineTotal(), taxAmount),
                    MoneyUtils.roundCurrency(cgst),
                    MoneyUtils.roundCurrency(sgst),
                    MoneyUtils.roundCurrency(igst),
                    gstService.resolveTaxType(company.getStateCode(), invoice.getDealer() != null ? invoice.getDealer().getStateCode() : null, false));
        }
        return gstService.splitTaxAmount(
                taxableAmount(line.getTaxableAmount(), line.getLineTotal(), taxAmount),
                taxAmount,
                company.getStateCode(),
                invoice.getDealer() != null ? invoice.getDealer().getStateCode() : null);
    }

    private GstService.GstBreakdown resolvePurchaseLineBreakdown(Company company,
                                                                 RawMaterialPurchase purchase,
                                                                 RawMaterialPurchaseLine line) {
        BigDecimal taxAmount = safe(line.getTaxAmount());
        if (taxAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return new GstService.GstBreakdown(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                    GstService.TaxType.INTER_STATE);
        }
        BigDecimal cgst = safe(line.getCgstAmount());
        BigDecimal sgst = safe(line.getSgstAmount());
        BigDecimal igst = safe(line.getIgstAmount());
        if (cgst.add(sgst).add(igst).compareTo(BigDecimal.ZERO) > 0) {
            return new GstService.GstBreakdown(
                    taxableAmount(null, line.getLineTotal(), taxAmount),
                    MoneyUtils.roundCurrency(cgst),
                    MoneyUtils.roundCurrency(sgst),
                    MoneyUtils.roundCurrency(igst),
                    gstService.resolveTaxType(company.getStateCode(), purchase.getSupplier() != null ? purchase.getSupplier().getStateCode() : null, false));
        }
        return gstService.splitTaxAmount(
                taxableAmount(null, line.getLineTotal(), taxAmount),
                taxAmount,
                company.getStateCode(),
                purchase.getSupplier() != null ? purchase.getSupplier().getStateCode() : null);
    }

    private BigDecimal taxableAmount(BigDecimal explicitTaxable, BigDecimal lineTotal, BigDecimal taxAmount) {
        if (explicitTaxable != null && explicitTaxable.compareTo(BigDecimal.ZERO) >= 0) {
            return MoneyUtils.roundCurrency(explicitTaxable);
        }
        BigDecimal fallback = safe(lineTotal).subtract(safe(taxAmount));
        if (fallback.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }
        return MoneyUtils.roundCurrency(fallback);
    }

    private boolean isIncludedInvoiceStatus(String status) {
        if (status == null) {
            return true;
        }
        String normalized = status.trim().toUpperCase();
        return !normalized.equals("DRAFT")
                && !normalized.equals("VOID")
                && !normalized.equals("REVERSED")
                && !normalized.equals("CANCELLED");
    }

    private boolean isIncludedPurchaseStatus(String status) {
        if (status == null) {
            return true;
        }
        String normalized = status.trim().toUpperCase();
        return !normalized.equals("VOID")
                && !normalized.equals("REVERSED")
                && !normalized.equals("CANCELLED");
    }

    private GstReconciliationDto.GstComponentSummary componentSummary(BigDecimal cgst,
                                                                      BigDecimal sgst,
                                                                      BigDecimal igst) {
        BigDecimal roundedCgst = MoneyUtils.roundCurrency(cgst == null ? BigDecimal.ZERO : cgst);
        BigDecimal roundedSgst = MoneyUtils.roundCurrency(sgst == null ? BigDecimal.ZERO : sgst);
        BigDecimal roundedIgst = MoneyUtils.roundCurrency(igst == null ? BigDecimal.ZERO : igst);
        BigDecimal total = MoneyUtils.roundCurrency(roundedCgst.add(roundedSgst).add(roundedIgst));
        return new GstReconciliationDto.GstComponentSummary(roundedCgst, roundedSgst, roundedIgst, total);
    }

    private boolean isNonGstMode(Company company) {
        BigDecimal defaultGstRate = company.getDefaultGstRate();
        return defaultGstRate != null && defaultGstRate.compareTo(BigDecimal.ZERO) == 0;
    }

    private void ensureNonGstCompanyDoesNotCarryGstAccounts(Company company) {
        List<String> configured = new ArrayList<>();
        if (company.getGstInputTaxAccountId() != null) {
            configured.add("gstInputTaxAccountId");
        }
        if (company.getGstOutputTaxAccountId() != null) {
            configured.add("gstOutputTaxAccountId");
        }
        if (company.getGstPayableAccountId() != null) {
            configured.add("gstPayableAccountId");
        }
        if (configured.isEmpty()) {
            return;
        }
        throw new ApplicationException(
                ErrorCode.VALIDATION_INVALID_INPUT,
                "Non-GST mode company cannot have GST tax accounts configured")
                .withDetail("configured", configured);
    }

    private GstReturnDto buildGstReturn(YearMonth period,
                                        LocalDate periodStart,
                                        LocalDate periodEnd,
                                        BigDecimal outputTax,
                                        BigDecimal inputTax) {
        GstReturnDto dto = new GstReturnDto();
        dto.setPeriod(period);
        dto.setPeriodStart(periodStart);
        dto.setPeriodEnd(periodEnd);
        dto.setOutputTax(outputTax);
        dto.setInputTax(inputTax);
        dto.setNetPayable(MoneyUtils.roundCurrency(outputTax.subtract(inputTax)));
        return dto;
    }

    private static final class ComponentTotals {
        private BigDecimal cgst = BigDecimal.ZERO;
        private BigDecimal sgst = BigDecimal.ZERO;
        private BigDecimal igst = BigDecimal.ZERO;

        private void add(GstService.GstBreakdown breakdown) {
            if (breakdown == null) {
                return;
            }
            cgst = cgst.add(safeAmount(breakdown.cgst()));
            sgst = sgst.add(safeAmount(breakdown.sgst()));
            igst = igst.add(safeAmount(breakdown.igst()));
        }

        private static BigDecimal safeAmount(BigDecimal value) {
            return value == null ? BigDecimal.ZERO : value;
        }
    }

}
