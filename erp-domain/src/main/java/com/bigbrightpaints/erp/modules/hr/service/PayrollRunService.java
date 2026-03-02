package com.bigbrightpaints.erp.modules.hr.service;

import com.bigbrightpaints.erp.core.exception.ApplicationException;
import com.bigbrightpaints.erp.core.exception.ErrorCode;
import com.bigbrightpaints.erp.core.idempotency.IdempotencySignatureBuilder;
import com.bigbrightpaints.erp.core.util.CompanyClock;
import com.bigbrightpaints.erp.modules.company.domain.Company;
import com.bigbrightpaints.erp.modules.company.service.CompanyContextService;
import com.bigbrightpaints.erp.modules.hr.domain.PayrollRun;
import com.bigbrightpaints.erp.modules.hr.domain.PayrollRunRepository;
import com.bigbrightpaints.erp.modules.hr.service.PayrollService.CreatePayrollRunRequest;
import com.bigbrightpaints.erp.modules.hr.service.PayrollService.PayrollRunDto;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class PayrollRunService {

    private final PayrollRunRepository payrollRunRepository;
    private final CompanyContextService companyContextService;
    private final CompanyClock companyClock;

    public PayrollRunService(PayrollRunRepository payrollRunRepository,
                             CompanyContextService companyContextService,
                             CompanyClock companyClock) {
        this.payrollRunRepository = payrollRunRepository;
        this.companyContextService = companyContextService;
        this.companyClock = companyClock;
    }

    @Transactional
    public PayrollRunDto createPayrollRun(CreatePayrollRunRequest request) {
        Company company = companyContextService.requireCurrentCompany();
        if (request == null) {
            throw new ApplicationException(ErrorCode.VALIDATION_MISSING_REQUIRED_FIELD,
                    "Payroll run request is required");
        }
        if (request.runType() == null) {
            throw new ApplicationException(ErrorCode.VALIDATION_MISSING_REQUIRED_FIELD,
                    "Payroll run type is required");
        }
        if (request.periodStart() == null || request.periodEnd() == null) {
            throw new ApplicationException(ErrorCode.VALIDATION_MISSING_REQUIRED_FIELD,
                    "Payroll period start/end dates are required");
        }
        if (request.periodEnd().isBefore(request.periodStart())) {
            throw new ApplicationException(ErrorCode.VALIDATION_INVALID_INPUT,
                    "Payroll period end date cannot be before start date");
        }

        String idempotencyKey = buildIdempotencyKey(request.runType(), request.periodStart(), request.periodEnd());
        String requestSignature = buildRunSignature(
                request.runType(), request.periodStart(), request.periodEnd(), request.remarks());

        Optional<PayrollRun> existing = payrollRunRepository.findByCompanyAndIdempotencyKey(company, idempotencyKey);
        if (existing.isPresent()) {
            PayrollRun run = existing.get();
            assertRunSignatureMatches(run, requestSignature, idempotencyKey);
            ensureIdempotencyMetadata(run, idempotencyKey, requestSignature);
            return PayrollService.toDto(run);
        }

        Optional<PayrollRun> legacy = payrollRunRepository.findByCompanyAndRunTypeAndPeriodStartAndPeriodEnd(
                company,
                request.runType(),
                request.periodStart(),
                request.periodEnd());
        if (legacy.isPresent()) {
            PayrollRun run = legacy.get();
            assertRunSignatureMatches(run, requestSignature, idempotencyKey);
            ensureIdempotencyMetadata(run, idempotencyKey, requestSignature);
            return PayrollService.toDto(run);
        }

        PayrollRun run = new PayrollRun();
        run.setCompany(company);
        run.setRunType(request.runType());
        run.setPeriodStart(request.periodStart());
        run.setPeriodEnd(request.periodEnd());
        LocalDate runDate = request.periodEnd() != null ? request.periodEnd() : request.periodStart();
        run.setRunDate(runDate != null ? runDate : companyClock.today(company));
        run.setRunNumber(generateRunNumber(request.runType(), request.periodStart()));
        run.setCreatedBy(getCurrentUser());
        run.setRemarks(request.remarks());
        run.setIdempotencyKey(idempotencyKey);
        run.setIdempotencyHash(requestSignature);

        try {
            return PayrollService.toDto(payrollRunRepository.save(run));
        } catch (DataIntegrityViolationException ex) {
            Optional<PayrollRun> concurrent = payrollRunRepository.findByCompanyAndIdempotencyKey(company, idempotencyKey);
            if (concurrent.isPresent()) {
                PayrollRun existingRun = concurrent.get();
                assertRunSignatureMatches(existingRun, requestSignature, idempotencyKey);
                ensureIdempotencyMetadata(existingRun, idempotencyKey, requestSignature);
                return PayrollService.toDto(existingRun);
            }
            Optional<PayrollRun> legacyConcurrent = payrollRunRepository.findByCompanyAndRunTypeAndPeriodStartAndPeriodEnd(
                    company,
                    request.runType(),
                    request.periodStart(),
                    request.periodEnd());
            if (legacyConcurrent.isPresent()) {
                PayrollRun existingRun = legacyConcurrent.get();
                assertRunSignatureMatches(existingRun, requestSignature, idempotencyKey);
                ensureIdempotencyMetadata(existingRun, idempotencyKey, requestSignature);
                return PayrollService.toDto(existingRun);
            }
            throw ex;
        }
    }

    @Transactional
    public PayrollRunDto createWeeklyPayrollRun(LocalDate weekEndingDate) {
        LocalDate weekStart = weekEndingDate.with(DayOfWeek.MONDAY);
        LocalDate weekEnd = weekEndingDate.with(DayOfWeek.SATURDAY);

        return createPayrollRun(new CreatePayrollRunRequest(
                PayrollRun.RunType.WEEKLY,
                weekStart,
                weekEnd,
                "Weekly payroll for labourers"));
    }

    @Transactional
    public PayrollRunDto createMonthlyPayrollRun(int year, int month) {
        LocalDate monthStart = LocalDate.of(year, month, 1);
        LocalDate monthEnd = monthStart.with(TemporalAdjusters.lastDayOfMonth());

        return createPayrollRun(new CreatePayrollRunRequest(
                PayrollRun.RunType.MONTHLY,
                monthStart,
                monthEnd,
                "Monthly payroll for staff - " + monthStart.getMonth() + " " + year));
    }

    public static String buildIdempotencyKey(PayrollRun.RunType runType,
                                              LocalDate periodStart,
                                              LocalDate periodEnd) {
        return "PAYROLL:%s:%s:%s".formatted(
                runType.name(),
                periodStart,
                periodEnd);
    }

    public static String buildRunSignature(PayrollRun.RunType runType,
                                           LocalDate periodStart,
                                           LocalDate periodEnd,
                                           String remarks) {
        return IdempotencySignatureBuilder.create()
                .add(runType.name())
                .add(periodStart)
                .add(periodEnd)
                .addToken(remarks)
                .buildHash();
    }

    public static String buildRunSignature(PayrollRun run) {
        if (run == null || run.getRunType() == null || run.getPeriodStart() == null || run.getPeriodEnd() == null) {
            return null;
        }
        return buildRunSignature(run.getRunType(), run.getPeriodStart(), run.getPeriodEnd(), run.getNotes());
    }

    private void assertRunSignatureMatches(PayrollRun run, String expectedSignature, String idempotencyKey) {
        if (run == null) {
            return;
        }
        if (run.getRunType() == null || run.getPeriodStart() == null || run.getPeriodEnd() == null) {
            throw new ApplicationException(ErrorCode.CONCURRENCY_CONFLICT,
                    "Existing payroll run is missing canonical period fields")
                    .withDetail("payrollRunId", run.getId())
                    .withDetail("idempotencyKey", idempotencyKey);
        }
        String storedSignature = run.getIdempotencyHash();
        if (!StringUtils.hasText(storedSignature)) {
            String derivedSignature = buildRunSignature(run);
            if (StringUtils.hasText(derivedSignature) && !derivedSignature.equals(expectedSignature)) {
                throw new ApplicationException(ErrorCode.CONCURRENCY_CONFLICT,
                        "Idempotency key already used with different payload")
                        .withDetail("idempotencyKey", idempotencyKey);
            }
            return;
        }
        if (!storedSignature.equals(expectedSignature)) {
            throw new ApplicationException(ErrorCode.CONCURRENCY_CONFLICT,
                    "Idempotency key already used with different payload")
                    .withDetail("idempotencyKey", idempotencyKey);
        }
    }

    private void ensureIdempotencyMetadata(PayrollRun run, String idempotencyKey, String signature) {
        boolean changed = false;
        if (!StringUtils.hasText(run.getIdempotencyKey())) {
            run.setIdempotencyKey(idempotencyKey);
            changed = true;
        }
        if (!StringUtils.hasText(run.getIdempotencyHash()) && StringUtils.hasText(signature)) {
            run.setIdempotencyHash(signature);
            changed = true;
        }
        if (changed) {
            payrollRunRepository.save(run);
        }
    }

    private String generateRunNumber(PayrollRun.RunType runType, LocalDate periodStart) {
        String prefix = runType == PayrollRun.RunType.WEEKLY ? "PR-W" : "PR-M";
        int year = periodStart.getYear();

        if (runType == PayrollRun.RunType.WEEKLY) {
            int weekNum = periodStart.get(WeekFields.ISO.weekOfYear());
            return String.format("%s-%d-W%02d", prefix, year, weekNum);
        }
        return String.format("%s-%d-%02d", prefix, year, periodStart.getMonthValue());
    }

    private String getCurrentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "SYSTEM";
    }
}
