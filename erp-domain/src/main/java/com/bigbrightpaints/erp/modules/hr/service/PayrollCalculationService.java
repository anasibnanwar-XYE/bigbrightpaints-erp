package com.bigbrightpaints.erp.modules.hr.service;

import com.bigbrightpaints.erp.core.exception.ApplicationException;
import com.bigbrightpaints.erp.core.exception.ErrorCode;
import com.bigbrightpaints.erp.core.util.CompanyClock;
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
import com.bigbrightpaints.erp.modules.hr.service.PayrollService.EmployeeMonthlyPayDto;
import com.bigbrightpaints.erp.modules.hr.service.PayrollService.EmployeeWeeklyPayDto;
import com.bigbrightpaints.erp.modules.hr.service.PayrollService.MonthlyPaySummaryDto;
import com.bigbrightpaints.erp.modules.hr.service.PayrollService.PayrollRunDto;
import com.bigbrightpaints.erp.modules.hr.service.PayrollService.WeeklyPaySummaryDto;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class PayrollCalculationService {

    private static final BigDecimal ADVANCE_DEDUCTION_CAP = new BigDecimal("0.20");

    private final PayrollRunRepository payrollRunRepository;
    private final PayrollRunLineRepository payrollRunLineRepository;
    private final EmployeeRepository employeeRepository;
    private final AttendanceRepository attendanceRepository;
    private final CompanyContextService companyContextService;
    private final CompanyClock companyClock;

    public PayrollCalculationService(PayrollRunRepository payrollRunRepository,
                                     PayrollRunLineRepository payrollRunLineRepository,
                                     EmployeeRepository employeeRepository,
                                     AttendanceRepository attendanceRepository,
                                     CompanyContextService companyContextService,
                                     CompanyClock companyClock) {
        this.payrollRunRepository = payrollRunRepository;
        this.payrollRunLineRepository = payrollRunLineRepository;
        this.employeeRepository = employeeRepository;
        this.attendanceRepository = attendanceRepository;
        this.companyContextService = companyContextService;
        this.companyClock = companyClock;
    }

    @Transactional
    public PayrollRunDto calculatePayroll(Long payrollRunId) {
        Company company = companyContextService.requireCurrentCompany();
        PayrollRun run = payrollRunRepository.findByCompanyAndId(company, payrollRunId)
                .orElseThrow(() -> com.bigbrightpaints.erp.core.validation.ValidationUtils
                        .invalidInput("Payroll run not found"));

        if (run.getStatus() != PayrollRun.PayrollStatus.DRAFT) {
            throw new ApplicationException(ErrorCode.BUSINESS_INVALID_STATE,
                    "Can only calculate payroll in DRAFT status")
                    .withDetail("payrollRunId", payrollRunId)
                    .withDetail("currentStatus", run.getStatus().name());
        }

        payrollRunLineRepository.deleteByPayrollRun(run);

        List<Employee> employees = run.getRunType() == PayrollRun.RunType.WEEKLY
                ? employeeRepository.findByCompanyAndEmployeeTypeAndStatus(
                        company,
                        Employee.EmployeeType.LABOUR,
                        "ACTIVE")
                : employeeRepository.findByCompanyAndEmployeeTypeAndStatus(
                        company,
                        Employee.EmployeeType.STAFF,
                        "ACTIVE");

        BigDecimal totalBasePay = BigDecimal.ZERO;
        BigDecimal totalOvertimePay = BigDecimal.ZERO;
        BigDecimal totalDeductions = BigDecimal.ZERO;
        BigDecimal totalNetPay = BigDecimal.ZERO;
        BigDecimal totalPresentDays = BigDecimal.ZERO;
        BigDecimal totalOtHours = BigDecimal.ZERO;

        for (Employee employee : employees) {
            PayrollRunLine line = calculateEmployeePay(run, employee);
            payrollRunLineRepository.save(line);

            totalBasePay = totalBasePay.add(line.getBasePay());
            totalOvertimePay = totalOvertimePay.add(line.getOvertimePay());
            totalDeductions = totalDeductions.add(line.getTotalDeductions());
            totalNetPay = totalNetPay.add(line.getNetPay());
            totalPresentDays = totalPresentDays.add(line.getPresentDays());
            totalOtHours = totalOtHours.add(line.getOvertimeHours()).add(line.getDoubleOtHours());
        }

        run.setTotalEmployees(employees.size());
        run.setTotalBasePay(totalBasePay);
        run.setTotalOvertimePay(totalOvertimePay);
        run.setTotalDeductions(totalDeductions);
        run.setTotalNetPay(totalNetPay);
        run.setTotalPresentDays(totalPresentDays);
        run.setTotalOvertimeHours(totalOtHours);
        run.setTotalAmount(totalNetPay);
        if (run.getRunDate() == null) {
            LocalDate runDate = run.getPeriodEnd() != null ? run.getPeriodEnd() : run.getPeriodStart();
            run.setRunDate(runDate != null ? runDate : companyClock.today(company));
        }
        run.setProcessedBy(getCurrentUser());
        run.setStatus(PayrollRun.PayrollStatus.CALCULATED);

        payrollRunRepository.save(run);
        return PayrollService.toDto(run);
    }

    public WeeklyPaySummaryDto getWeeklyPaySummary(LocalDate weekEndingDate) {
        Company company = companyContextService.requireCurrentCompany();
        LocalDate weekStart = weekEndingDate.with(DayOfWeek.MONDAY);
        LocalDate weekEnd = weekEndingDate.with(DayOfWeek.SATURDAY);

        List<Employee> labourers = employeeRepository.findByCompanyAndEmployeeTypeAndStatus(
                company,
                Employee.EmployeeType.LABOUR,
                "ACTIVE");
        List<Attendance> attendance = attendanceRepository.findByEmployeeTypeAndStatusAndDateRange(
                company,
                Employee.EmployeeType.LABOUR,
                "ACTIVE",
                weekStart,
                weekEnd);

        List<EmployeeWeeklyPayDto> employeePay = new ArrayList<>();
        BigDecimal totalBasePay = BigDecimal.ZERO;
        BigDecimal totalOtPay = BigDecimal.ZERO;
        BigDecimal totalNetPay = BigDecimal.ZERO;

        Map<Long, BigDecimal> presentDaysByEmployee = new HashMap<>();
        Map<Long, BigDecimal> holidayDaysByEmployee = new HashMap<>();
        Map<Long, BigDecimal> overtimeHoursByEmployee = new HashMap<>();
        Map<Long, BigDecimal> doubleOtHoursByEmployee = new HashMap<>();

        for (Attendance att : attendance) {
            Long employeeId = att.getEmployee().getId();
            if (att.getStatus() == Attendance.AttendanceStatus.PRESENT) {
                presentDaysByEmployee.merge(employeeId, BigDecimal.ONE, BigDecimal::add);
            } else if (att.getStatus() == Attendance.AttendanceStatus.HALF_DAY) {
                presentDaysByEmployee.merge(employeeId, new BigDecimal("0.5"), BigDecimal::add);
            } else if (att.getStatus() == Attendance.AttendanceStatus.HOLIDAY) {
                holidayDaysByEmployee.merge(employeeId, BigDecimal.ONE, BigDecimal::add);
            }
            if (att.getOvertimeHours() != null) {
                overtimeHoursByEmployee.merge(employeeId, att.getOvertimeHours(), BigDecimal::add);
            }
            if (att.getDoubleOvertimeHours() != null) {
                doubleOtHoursByEmployee.merge(employeeId, att.getDoubleOvertimeHours(), BigDecimal::add);
            }
        }

        for (Employee employee : labourers) {
            BigDecimal presentDays = presentDaysByEmployee.getOrDefault(employee.getId(), BigDecimal.ZERO);
            BigDecimal otHours = overtimeHoursByEmployee.getOrDefault(employee.getId(), BigDecimal.ZERO);
            BigDecimal doubleOtHours = doubleOtHoursByEmployee.getOrDefault(employee.getId(), BigDecimal.ZERO);
            BigDecimal holidayDays = holidayDaysByEmployee.getOrDefault(employee.getId(), BigDecimal.ZERO);

            BigDecimal baseDays = presentDays.add(holidayDays);
            BigDecimal basePay = employee.getDailyRate().multiply(baseDays);
            BigDecimal standardHoursPerDay = requireValidStandardHoursPerDay(employee);
            BigDecimal hourlyRate = employee.getDailyRate().divide(standardHoursPerDay, 2, RoundingMode.HALF_UP);
            BigDecimal otPay = hourlyRate.multiply(employee.getOvertimeRateMultiplier()).multiply(otHours);
            BigDecimal doubleOtPay = hourlyRate.multiply(employee.getDoubleOtRateMultiplier()).multiply(doubleOtHours);
            BigDecimal netPay = basePay.add(otPay).add(doubleOtPay);

            BigDecimal daysWorkedExact = presentDays;
            int daysWorked = daysWorkedExact.setScale(0, RoundingMode.HALF_UP).intValue();
            employeePay.add(new EmployeeWeeklyPayDto(
                    employee.getId(),
                    employee.getFullName(),
                    employee.getDailyRate(),
                    daysWorked,
                    daysWorkedExact,
                    otHours,
                    basePay,
                    otPay,
                    netPay));

            totalBasePay = totalBasePay.add(basePay);
            totalOtPay = totalOtPay.add(otPay).add(doubleOtPay);
            totalNetPay = totalNetPay.add(netPay);
        }

        return new WeeklyPaySummaryDto(
                weekStart,
                weekEnd,
                labourers.size(),
                totalBasePay,
                totalOtPay,
                totalNetPay,
                employeePay);
    }

    public MonthlyPaySummaryDto getMonthlyPaySummary(int year, int month) {
        Company company = companyContextService.requireCurrentCompany();
        LocalDate monthStart = LocalDate.of(year, month, 1);
        LocalDate monthEnd = monthStart.with(TemporalAdjusters.lastDayOfMonth());

        List<Employee> staff = employeeRepository.findByCompanyAndEmployeeTypeAndStatus(
                company,
                Employee.EmployeeType.STAFF,
                "ACTIVE");
        List<Attendance> attendance = attendanceRepository.findByEmployeeTypeAndStatusAndDateRange(
                company,
                Employee.EmployeeType.STAFF,
                "ACTIVE",
                monthStart,
                monthEnd);

        List<EmployeeMonthlyPayDto> employeePay = new ArrayList<>();
        BigDecimal totalGross = BigDecimal.ZERO;
        BigDecimal totalDeductions = BigDecimal.ZERO;
        BigDecimal totalNet = BigDecimal.ZERO;

        Map<Long, BigDecimal> presentDaysByEmployee = new HashMap<>();
        Map<Long, BigDecimal> halfDaysByEmployee = new HashMap<>();
        Map<Long, BigDecimal> absentDaysByEmployee = new HashMap<>();
        Map<Long, BigDecimal> holidayDaysByEmployee = new HashMap<>();

        for (Attendance att : attendance) {
            Long employeeId = att.getEmployee().getId();
            switch (att.getStatus()) {
                case PRESENT -> presentDaysByEmployee.merge(employeeId, BigDecimal.ONE, BigDecimal::add);
                case HALF_DAY -> presentDaysByEmployee.merge(employeeId, new BigDecimal("0.5"), BigDecimal::add);
                case ABSENT -> absentDaysByEmployee.merge(employeeId, BigDecimal.ONE, BigDecimal::add);
                case HOLIDAY -> holidayDaysByEmployee.merge(employeeId, BigDecimal.ONE, BigDecimal::add);
                case LEAVE, WEEKEND -> {
                }
            }
            if (att.getStatus() == Attendance.AttendanceStatus.HALF_DAY) {
                halfDaysByEmployee.merge(employeeId, BigDecimal.ONE, BigDecimal::add);
            }
        }

        for (Employee employee : staff) {
            BigDecimal presentDays = presentDaysByEmployee.getOrDefault(employee.getId(), BigDecimal.ZERO);
            BigDecimal halfDays = halfDaysByEmployee.getOrDefault(employee.getId(), BigDecimal.ZERO);
            BigDecimal absentDays = absentDaysByEmployee.getOrDefault(employee.getId(), BigDecimal.ZERO);
            BigDecimal holidayDays = holidayDaysByEmployee.getOrDefault(employee.getId(), BigDecimal.ZERO);

            BigDecimal dailyRate = employee.getDailyRate();
            BigDecimal monthlySalary = employee.getMonthlySalary();
            BigDecimal grossPay;
            if (monthlySalary != null && monthlySalary.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal deductionDays = absentDays;
                if (halfDays.compareTo(BigDecimal.ZERO) > 0) {
                    deductionDays = deductionDays.add(halfDays.multiply(new BigDecimal("0.5")));
                }
                BigDecimal absenceDeduction = dailyRate.multiply(deductionDays);
                grossPay = monthlySalary.subtract(absenceDeduction);
                if (grossPay.compareTo(BigDecimal.ZERO) < 0) {
                    grossPay = BigDecimal.ZERO;
                }
            } else {
                BigDecimal baseDays = presentDays;
                BigDecimal basePay = dailyRate.multiply(baseDays);
                BigDecimal holidayPay = dailyRate.multiply(holidayDays);
                grossPay = basePay.add(holidayPay);
            }

            BigDecimal pfDeduction = BigDecimal.ZERO;
            BigDecimal totalDed = BigDecimal.ZERO;
            BigDecimal netPay = grossPay;

            employeePay.add(new EmployeeMonthlyPayDto(
                    employee.getId(),
                    employee.getFullName(),
                    employee.getMonthlySalary(),
                    presentDays.setScale(0, RoundingMode.HALF_UP).intValue(),
                    absentDays.setScale(0, RoundingMode.HALF_UP).intValue(),
                    presentDays,
                    halfDays,
                    absentDays,
                    grossPay,
                    pfDeduction,
                    netPay));

            totalGross = totalGross.add(grossPay);
            totalDeductions = totalDeductions.add(totalDed);
            totalNet = totalNet.add(netPay);
        }

        return new MonthlyPaySummaryDto(
                year,
                month,
                staff.size(),
                totalGross,
                totalDeductions,
                totalNet,
                employeePay);
    }

    private PayrollRunLine calculateEmployeePay(PayrollRun run, Employee employee) {
        PayrollRunLine line = new PayrollRunLine();
        line.setPayrollRun(run);
        line.setEmployee(employee);
        line.setName(employee.getFullName());

        List<Attendance> attendance = attendanceRepository.findByEmployeeAndAttendanceDateBetween(
                employee,
                run.getPeriodStart(),
                run.getPeriodEnd());

        BigDecimal presentDays = BigDecimal.ZERO;
        BigDecimal halfDays = BigDecimal.ZERO;
        BigDecimal absentDays = BigDecimal.ZERO;
        BigDecimal leaveDays = BigDecimal.ZERO;
        BigDecimal holidayDays = BigDecimal.ZERO;
        BigDecimal regularHours = BigDecimal.ZERO;
        BigDecimal overtimeHours = BigDecimal.ZERO;
        BigDecimal doubleOtHours = BigDecimal.ZERO;

        for (Attendance att : attendance) {
            switch (att.getStatus()) {
                case PRESENT -> presentDays = presentDays.add(BigDecimal.ONE);
                case HALF_DAY -> halfDays = halfDays.add(BigDecimal.ONE);
                case ABSENT -> absentDays = absentDays.add(BigDecimal.ONE);
                case LEAVE -> leaveDays = leaveDays.add(BigDecimal.ONE);
                case HOLIDAY -> holidayDays = holidayDays.add(BigDecimal.ONE);
                case WEEKEND -> {
                }
            }
            if (att.getRegularHours() != null) {
                regularHours = regularHours.add(att.getRegularHours());
            }
            if (att.getOvertimeHours() != null) {
                overtimeHours = overtimeHours.add(att.getOvertimeHours());
            }
            if (att.getDoubleOvertimeHours() != null) {
                doubleOtHours = doubleOtHours.add(att.getDoubleOvertimeHours());
            }
        }

        line.setPresentDays(presentDays);
        line.setHalfDays(halfDays);
        line.setAbsentDays(absentDays);
        line.setLeaveDays(leaveDays);
        line.setHolidayDays(holidayDays);
        line.setRegularHours(regularHours);
        line.setOvertimeHours(overtimeHours);
        line.setDoubleOtHours(doubleOtHours);

        BigDecimal dailyRate = employee.getDailyRate();
        if (dailyRate == null) {
            dailyRate = BigDecimal.ZERO;
        }
        BigDecimal dailyWage = employee.getDailyWage() != null ? employee.getDailyWage() : dailyRate;
        BigDecimal standardHoursPerDay = requireValidStandardHoursPerDay(employee);
        BigDecimal hourlyRate = dailyRate.divide(standardHoursPerDay, 2, RoundingMode.HALF_UP);
        line.setDailyWage(dailyWage);
        line.setDailyRate(dailyRate);
        line.setHourlyRate(hourlyRate);
        line.setOtRateMultiplier(employee.getOvertimeRateMultiplier());
        line.setDoubleOtMultiplier(employee.getDoubleOtRateMultiplier());

        BigDecimal effectiveDays = presentDays.add(halfDays.multiply(new BigDecimal("0.5")));
        line.setDaysWorked(effectiveDays.setScale(0, RoundingMode.HALF_UP).intValue());
        BigDecimal basePay = dailyRate.multiply(effectiveDays);
        line.setBasePay(basePay);

        BigDecimal otRate = hourlyRate.multiply(employee.getOvertimeRateMultiplier());
        BigDecimal doubleOtRate = hourlyRate.multiply(employee.getDoubleOtRateMultiplier());
        BigDecimal overtimePay = otRate.multiply(overtimeHours).add(doubleOtRate.multiply(doubleOtHours));
        line.setOvertimePay(overtimePay);

        BigDecimal holidayPay = dailyRate.multiply(holidayDays);
        line.setHolidayPay(holidayPay);

        BigDecimal grossPay = basePay.add(overtimePay).add(holidayPay);
        line.setGrossPay(grossPay);

        BigDecimal advanceDeduction = calculateAdvanceDeduction(grossPay, employee);
        line.setAdvanceDeduction(advanceDeduction);
        line.setAdvances(advanceDeduction);
        line.setPfDeduction(BigDecimal.ZERO);

        BigDecimal totalDeductions = advanceDeduction;
        line.setTotalDeductions(totalDeductions);

        BigDecimal netPay = grossPay.subtract(totalDeductions);
        line.setNetPay(netPay);
        line.setLineTotal(netPay);

        return line;
    }

    private BigDecimal calculateAdvanceDeduction(BigDecimal grossPay, Employee employee) {
        if (employee == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal balance = employee.getAdvanceBalance();
        if (balance == null || balance.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal cap = grossPay.multiply(ADVANCE_DEDUCTION_CAP).setScale(2, RoundingMode.HALF_UP);
        return balance.min(cap);
    }

    private BigDecimal requireValidStandardHoursPerDay(Employee employee) {
        BigDecimal configuredStandardHours = employee != null ? employee.getConfiguredStandardHoursPerDay() : null;
        if (configuredStandardHours == null || configuredStandardHours.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ApplicationException(ErrorCode.VALIDATION_INVALID_INPUT,
                    "Employee standardHoursPerDay must be greater than zero for payroll calculation")
                    .withDetail("employeeId", employee != null ? employee.getId() : null)
                    .withDetail("configuredStandardHoursPerDay", configuredStandardHours);
        }
        return configuredStandardHours;
    }

    private String getCurrentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "SYSTEM";
    }
}
