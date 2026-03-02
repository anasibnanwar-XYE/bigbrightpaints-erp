package com.bigbrightpaints.erp.modules.hr.service;

import com.bigbrightpaints.erp.core.audit.AuditEvent;
import com.bigbrightpaints.erp.core.audit.AuditService;
import com.bigbrightpaints.erp.core.exception.ApplicationException;
import com.bigbrightpaints.erp.core.exception.ErrorCode;
import com.bigbrightpaints.erp.core.util.CompanyClock;
import com.bigbrightpaints.erp.core.util.CompanyEntityLookup;
import com.bigbrightpaints.erp.core.util.CompanyTime;
import com.bigbrightpaints.erp.modules.accounting.domain.Account;
import com.bigbrightpaints.erp.modules.accounting.domain.AccountRepository;
import com.bigbrightpaints.erp.modules.accounting.domain.AccountType;
import com.bigbrightpaints.erp.modules.accounting.dto.JournalCreationRequest;
import com.bigbrightpaints.erp.modules.accounting.dto.JournalEntryDto;
import com.bigbrightpaints.erp.modules.accounting.dto.JournalEntryRequest.JournalLineRequest;
import com.bigbrightpaints.erp.modules.accounting.service.AccountingFacade;
import com.bigbrightpaints.erp.modules.company.domain.Company;
import com.bigbrightpaints.erp.modules.company.service.CompanyContextService;
import com.bigbrightpaints.erp.modules.hr.domain.Attendance;
import com.bigbrightpaints.erp.modules.hr.domain.AttendanceRepository;
import com.bigbrightpaints.erp.modules.hr.domain.Employee;
import com.bigbrightpaints.erp.modules.hr.domain.EmployeeRepository;
import com.bigbrightpaints.erp.modules.hr.domain.PayrollRun;
import com.bigbrightpaints.erp.modules.hr.domain.PayrollRunLine;
import com.bigbrightpaints.erp.modules.hr.domain.PayrollRunLineRepository;
import com.bigbrightpaints.erp.modules.hr.domain.PayrollRunRepository;
import com.bigbrightpaints.erp.modules.hr.service.PayrollService.PayrollRunDto;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class PayrollPostingService {

    private static final String PAYROLL_ACCOUNTS_CANONICAL_PATH = "/api/v1/accounting/accounts";
    private static final String PAYROLL_PAYMENTS_CANONICAL_PATH = "/api/v1/accounting/payroll/payments";
    private static final String PAYROLL_MIGRATION_SET = "v2";
    private static final Map<String, AccountType> REQUIRED_PAYROLL_ACCOUNT_TYPES = Map.of(
            "SALARY-EXP", AccountType.EXPENSE,
            "WAGE-EXP", AccountType.EXPENSE,
            "SALARY-PAYABLE", AccountType.LIABILITY,
            "EMP-ADV", AccountType.ASSET
    );
    private static final List<String> REQUIRED_PAYROLL_ACCOUNTS = List.of(
            "SALARY-EXP",
            "WAGE-EXP",
            "SALARY-PAYABLE",
            "EMP-ADV"
    );

    private final PayrollRunRepository payrollRunRepository;
    private final PayrollRunLineRepository payrollRunLineRepository;
    private final EmployeeRepository employeeRepository;
    private final AttendanceRepository attendanceRepository;
    private final AccountingFacade accountingFacade;
    private final AccountRepository accountRepository;
    private final CompanyContextService companyContextService;
    private final CompanyEntityLookup companyEntityLookup;
    private final CompanyClock companyClock;
    private final AuditService auditService;

    public PayrollPostingService(PayrollRunRepository payrollRunRepository,
                                 PayrollRunLineRepository payrollRunLineRepository,
                                 EmployeeRepository employeeRepository,
                                 AttendanceRepository attendanceRepository,
                                 AccountingFacade accountingFacade,
                                 AccountRepository accountRepository,
                                 CompanyContextService companyContextService,
                                 CompanyEntityLookup companyEntityLookup,
                                 CompanyClock companyClock,
                                 AuditService auditService) {
        this.payrollRunRepository = payrollRunRepository;
        this.payrollRunLineRepository = payrollRunLineRepository;
        this.employeeRepository = employeeRepository;
        this.attendanceRepository = attendanceRepository;
        this.accountingFacade = accountingFacade;
        this.accountRepository = accountRepository;
        this.companyContextService = companyContextService;
        this.companyEntityLookup = companyEntityLookup;
        this.companyClock = companyClock;
        this.auditService = auditService;
    }

    @Transactional
    public PayrollRunDto approvePayroll(Long payrollRunId) {
        Company company = companyContextService.requireCurrentCompany();
        PayrollRun run = payrollRunRepository.findByCompanyAndId(company, payrollRunId)
                .orElseThrow(() -> com.bigbrightpaints.erp.core.validation.ValidationUtils
                        .invalidInput("Payroll run not found"));

        if (run.getStatus() != PayrollRun.PayrollStatus.CALCULATED) {
            throw new ApplicationException(ErrorCode.BUSINESS_INVALID_STATE,
                    "Can only approve payroll in CALCULATED status")
                    .withDetail("payrollRunId", payrollRunId)
                    .withDetail("currentStatus", run.getStatus().name());
        }
        if (payrollRunLineRepository.findByPayrollRun(run).isEmpty()) {
            throw new ApplicationException(ErrorCode.BUSINESS_INVALID_STATE,
                    "Cannot approve payroll run with no calculated lines")
                    .withDetail("payrollRunId", payrollRunId);
        }

        run.setStatus(PayrollRun.PayrollStatus.APPROVED);
        run.setApprovedBy(getCurrentUser());
        run.setApprovedAt(CompanyTime.now(company));
        run.setProcessedBy(getCurrentUser());

        payrollRunRepository.save(run);
        return PayrollService.toDto(run);
    }

    @Transactional
    public PayrollRunDto postPayrollToAccounting(Long payrollRunId) {
        Company company = companyContextService.requireCurrentCompany();
        PayrollRun run = companyEntityLookup.lockPayrollRun(company, payrollRunId);
        boolean hasPostingJournalLink = hasPostingJournalLink(run);
        boolean statusPosted = run.getStatus() == PayrollRun.PayrollStatus.POSTED;

        if (statusPosted && !hasPostingJournalLink) {
            throw new ApplicationException(ErrorCode.BUSINESS_INVALID_STATE,
                    "Payroll run is POSTED but missing posting journal linkage")
                    .withDetail("payrollRunId", payrollRunId)
                    .withDetail("currentStatus", run.getStatus().name())
                    .withDetail("invariant", "posted_requires_journal_link");
        }

        if (!statusPosted && !hasPostingJournalLink && run.getStatus() != PayrollRun.PayrollStatus.APPROVED) {
            throw new ApplicationException(ErrorCode.BUSINESS_INVALID_STATE,
                    "Can only post approved payroll")
                    .withDetail("payrollRunId", payrollRunId)
                    .withDetail("currentStatus", run.getStatus().name());
        }

        Account salaryExpenseAccount = findAccountByCode(company, "SALARY-EXP");
        Account wageExpenseAccount = findAccountByCode(company, "WAGE-EXP");
        Account salaryPayableAccount = findAccountByCode(company, "SALARY-PAYABLE");

        List<PayrollRunLine> runLines = payrollRunLineRepository.findByPayrollRun(run);
        if (runLines.isEmpty()) {
            throw new ApplicationException(ErrorCode.BUSINESS_INVALID_STATE,
                    "Payroll run has no calculated lines; run calculate before posting")
                    .withDetail("payrollRunId", payrollRunId);
        }

        boolean hasUnsupportedDeductions = runLines.stream().anyMatch(line ->
                hasPositive(line.getPfDeduction())
                        || hasPositive(line.getTaxDeduction())
                        || hasPositive(line.getOtherDeductions()));
        if (hasUnsupportedDeductions) {
            throw new ApplicationException(ErrorCode.BUSINESS_CONSTRAINT_VIOLATION,
                    "Payroll statutory deductions (PF/tax/other) are not supported in runs. "
                            + "Only advance deductions are applied; use accounting payroll payments for statutory withholdings.");
        }

        BigDecimal totalGrossPay = runLines.stream()
                .map(PayrollRunLine::getGrossPay)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalGrossPay.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ApplicationException(ErrorCode.BUSINESS_INVALID_STATE,
                    "Payroll run total gross pay is zero; nothing to post")
                    .withDetail("payrollRunId", payrollRunId);
        }

        BigDecimal totalAdvances = runLines.stream()
                .map(PayrollRunLine::getAdvanceDeduction)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Account advanceAccount = null;
        if (totalAdvances.compareTo(BigDecimal.ZERO) > 0) {
            advanceAccount = findAccountByCode(company, "EMP-ADV");
        }

        BigDecimal salaryPayableAmount = totalGrossPay.subtract(totalAdvances);

        List<JournalLineRequest> lines = new ArrayList<>();
        Account expenseAccount = run.getRunType() == PayrollRun.RunType.MONTHLY
                ? salaryExpenseAccount
                : wageExpenseAccount;
        lines.add(new JournalLineRequest(expenseAccount.getId(), "Payroll expense", totalGrossPay, BigDecimal.ZERO));

        lines.add(new JournalLineRequest(
                salaryPayableAccount.getId(),
                "Payroll payable",
                BigDecimal.ZERO,
                salaryPayableAmount));
        if (advanceAccount != null) {
            lines.add(new JournalLineRequest(
                    advanceAccount.getId(),
                    "Advance recovery",
                    BigDecimal.ZERO,
                    totalAdvances));
        }

        LocalDate postingDate = run.getPeriodEnd();
        LocalDate today = companyClock.today(company);
        if (postingDate == null || postingDate.isAfter(today)) {
            postingDate = today;
        }

        String runNumber = run.getRunNumber();
        if (!StringUtils.hasText(runNumber) && run.getId() != null) {
            runNumber = "LEGACY-" + run.getId();
            run.setRunNumber(runNumber);
        }

        String memo = "Payroll - " + (runNumber != null ? runNumber : "RUN");
        JournalCreationRequest standardizedPayrollRequest = new JournalCreationRequest(
                totalGrossPay,
                expenseAccount.getId(),
                salaryPayableAccount.getId(),
                memo,
                "PAYROLL",
                "PAYROLL-" + (runNumber != null ? runNumber : "RUN"),
                null,
                lines.stream()
                        .map(line -> new JournalCreationRequest.LineRequest(
                                line.accountId(),
                                line.debit(),
                                line.credit(),
                                line.description()))
                        .toList(),
                postingDate,
                null,
                null,
                false);
        lines = standardizedPayrollRequest.resolvedLines();

        JournalEntryDto journal = accountingFacade.postPayrollRun(runNumber, run.getId(), postingDate, memo, lines);

        if (hasPostingJournalLink && run.getJournalEntryId() != null && !run.getJournalEntryId().equals(journal.id())) {
            throw new ApplicationException(ErrorCode.CONCURRENCY_CONFLICT,
                    "Payroll run already linked to a different posting journal")
                    .withDetail("payrollRunId", run.getId())
                    .withDetail("journalEntryId", run.getJournalEntryId())
                    .withDetail("postedJournalEntryId", journal.id());
        }

        boolean updated = false;
        if (run.getJournalEntryId() == null) {
            run.setJournalEntryId(journal.id());
            updated = true;
        }
        if (run.getJournalEntry() == null) {
            run.setJournalEntry(companyEntityLookup.requireJournalEntry(company, journal.id()));
            updated = true;
        }
        if (run.getStatus() != PayrollRun.PayrollStatus.POSTED) {
            run.setStatus(PayrollRun.PayrollStatus.POSTED);
            if (run.getPostedBy() == null) {
                run.setPostedBy(getCurrentUser());
            }
            if (run.getPostedAt() == null) {
                run.setPostedAt(CompanyTime.now(company));
            }
            if (run.getTotalAmount() == null) {
                run.setTotalAmount(run.getTotalNetPay());
            }
            updated = true;
        }

        if (!statusPosted) {
            Map<Long, List<Attendance>> attendanceByEmployeeId = attendanceRepository.findByCompanyAndEmployeeIdsAndDateRange(
                            company,
                            runLines.stream().map(line -> line.getEmployee().getId()).distinct().toList(),
                            run.getPeriodStart(),
                            run.getPeriodEnd())
                    .stream()
                    .collect(java.util.stream.Collectors.groupingBy(record -> record.getEmployee().getId()));

            List<Attendance> linkedAttendance = new ArrayList<>();
            for (PayrollRunLine line : runLines) {
                List<Attendance> employeeAttendance = attendanceByEmployeeId.getOrDefault(line.getEmployee().getId(), List.of());
                for (Attendance attendance : employeeAttendance) {
                    attendance.setPayrollRunId(run.getId());
                    linkedAttendance.add(attendance);
                }
            }
            if (!linkedAttendance.isEmpty()) {
                attendanceRepository.saveAll(linkedAttendance);
            }
        }

        if (updated) {
            payrollRunRepository.save(run);
        }

        if (!statusPosted) {
            Map<String, String> auditMetadata = requiredPayrollPostedAuditMetadata(
                    run,
                    journal,
                    postingDate,
                    totalGrossPay,
                    totalAdvances,
                    salaryPayableAmount);
            auditService.logSuccess(AuditEvent.PAYROLL_POSTED, auditMetadata);
        }

        return PayrollService.toDto(run);
    }

    @Transactional
    public PayrollRunDto markAsPaid(Long payrollRunId, String paymentReference) {
        Company company = companyContextService.requireCurrentCompany();
        PayrollRun run = companyEntityLookup.lockPayrollRun(company, payrollRunId);

        if (run.getPaymentJournalEntryId() == null) {
            throw new ApplicationException(ErrorCode.BUSINESS_INVALID_STATE,
                    "Payroll payment journal is required before marking payroll as PAID")
                    .withDetail("canonicalPath", PAYROLL_PAYMENTS_CANONICAL_PATH);
        }

        var paymentJournal = companyEntityLookup.requireJournalEntry(company, run.getPaymentJournalEntryId());
        String canonicalPaymentReference = paymentJournal.getReferenceNumber();
        if (!StringUtils.hasText(canonicalPaymentReference)) {
            throw new ApplicationException(ErrorCode.BUSINESS_INVALID_STATE,
                    "Payroll payment journal reference is missing; re-record payment through accounting payroll payments")
                    .withDetail("payrollRunId", payrollRunId)
                    .withDetail("paymentJournalEntryId", run.getPaymentJournalEntryId())
                    .withDetail("canonicalPath", PAYROLL_PAYMENTS_CANONICAL_PATH);
        }
        canonicalPaymentReference = canonicalPaymentReference.trim();
        String persistedPaymentReference = StringUtils.hasText(paymentReference)
                ? paymentReference.trim()
                : canonicalPaymentReference;

        if (run.getStatus() != PayrollRun.PayrollStatus.POSTED && run.getStatus() != PayrollRun.PayrollStatus.PAID) {
            throw new ApplicationException(ErrorCode.BUSINESS_INVALID_STATE,
                    "Can only mark posted payroll as paid");
        }

        List<PayrollRunLine> lines = payrollRunLineRepository.findByPayrollRun(run);
        List<PayrollRunLine> dirtyLines = new ArrayList<>();
        for (PayrollRunLine line : lines) {
            if (line.getPaymentStatus() == PayrollRunLine.PaymentStatus.PAID) {
                continue;
            }
            line.setPaymentStatus(PayrollRunLine.PaymentStatus.PAID);
            line.setPaymentReference(canonicalPaymentReference);
            dirtyLines.add(line);

            BigDecimal advances = line.getAdvances() != null ? line.getAdvances() : BigDecimal.ZERO;
            if (advances.compareTo(BigDecimal.ZERO) > 0) {
                Employee employee = line.getEmployee();
                BigDecimal currentBalance = employee.getAdvanceBalance() != null
                        ? employee.getAdvanceBalance()
                        : BigDecimal.ZERO;
                BigDecimal newBalance = currentBalance.subtract(advances);
                if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
                    newBalance = BigDecimal.ZERO;
                }
                if (newBalance.compareTo(currentBalance) != 0) {
                    employee.setAdvanceBalance(newBalance);
                    employeeRepository.save(employee);
                }
            }
        }

        if (!dirtyLines.isEmpty()) {
            payrollRunLineRepository.saveAll(dirtyLines);
        }

        boolean payrollRunDirty = false;
        if (run.getStatus() != PayrollRun.PayrollStatus.PAID) {
            run.setStatus(PayrollRun.PayrollStatus.PAID);
            payrollRunDirty = true;
        }
        if (!Objects.equals(run.getPaymentReference(), persistedPaymentReference)) {
            run.setPaymentReference(persistedPaymentReference);
            payrollRunDirty = true;
        }
        if (payrollRunDirty) {
            payrollRunRepository.save(run);
        }

        return PayrollService.toDto(run);
    }

    static Map<String, String> requiredPayrollPostedAuditMetadata(PayrollRun run,
                                                                   JournalEntryDto journal,
                                                                   LocalDate postingDate,
                                                                   BigDecimal totalGrossPay,
                                                                   BigDecimal totalAdvances,
                                                                   BigDecimal salaryPayableAmount) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("payrollRunId", requiredAuditMetadataValue("payrollRunId", run.getId()));
        metadata.put("runNumber", requiredAuditMetadataValue("runNumber", run.getRunNumber()));
        metadata.put("runType", requiredAuditMetadataValue("runType", run.getRunType()));
        metadata.put("periodStart", requiredAuditMetadataValue("periodStart", run.getPeriodStart()));
        metadata.put("periodEnd", requiredAuditMetadataValue("periodEnd", run.getPeriodEnd()));
        metadata.put("journalEntryId", requiredAuditMetadataValue("journalEntryId", journal.id()));
        metadata.put("postingDate", requiredAuditMetadataValue("postingDate", postingDate));
        metadata.put("totalGrossPay", requiredAuditMetadataValue("totalGrossPay", totalGrossPay));
        metadata.put("totalAdvances", requiredAuditMetadataValue("totalAdvances", totalAdvances));
        metadata.put("netPayable", requiredAuditMetadataValue("netPayable", salaryPayableAmount));
        return metadata;
    }

    private static String requiredAuditMetadataValue(String key, Object value) {
        if (value == null) {
            throw missingPayrollPostedMetadataException(key);
        }
        String normalized = value instanceof BigDecimal decimal
                ? decimal.toPlainString()
                : value.toString();
        if (!StringUtils.hasText(normalized)) {
            throw missingPayrollPostedMetadataException(key);
        }
        return normalized;
    }

    private static ApplicationException missingPayrollPostedMetadataException(String key) {
        return new ApplicationException(ErrorCode.BUSINESS_INVALID_STATE,
                "Payroll posting audit metadata is missing required key: " + key)
                .withDetail("auditEvent", AuditEvent.PAYROLL_POSTED.name())
                .withDetail("metadataKey", key);
    }

    private boolean hasPostingJournalLink(PayrollRun run) {
        if (run == null) {
            return false;
        }
        if (run.getJournalEntryId() != null) {
            return true;
        }
        return run.getJournalEntry() != null && run.getJournalEntry().getId() != null;
    }

    private boolean hasPositive(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) > 0;
    }

    private Account findAccountByCode(Company company, String code) {
        String normalizedCode = StringUtils.hasText(code) ? code.trim().toUpperCase(Locale.ROOT) : "";
        AccountType expectedType = REQUIRED_PAYROLL_ACCOUNT_TYPES.get(normalizedCode);
        String expectedTypeName = expectedType != null ? expectedType.name() : "UNKNOWN";

        return accountRepository.findByCompanyAndCodeIgnoreCase(company, normalizedCode)
                .orElseThrow(() -> new ApplicationException(ErrorCode.VALIDATION_INVALID_REFERENCE,
                        "Required payroll account not found: " + normalizedCode
                                + " (expected type: " + expectedTypeName + "). "
                                + "Provision this account in Chart of Accounts before posting payroll.")
                        .withDetail("accountCode", normalizedCode)
                        .withDetail("expectedAccountType", expectedTypeName)
                        .withDetail("requiredPayrollAccounts", REQUIRED_PAYROLL_ACCOUNTS)
                        .withDetail("migrationSet", PAYROLL_MIGRATION_SET)
                        .withDetail("manualProvisioningRequired", true)
                        .withDetail("canonicalPath", PAYROLL_ACCOUNTS_CANONICAL_PATH));
    }

    private String getCurrentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "SYSTEM";
    }
}
