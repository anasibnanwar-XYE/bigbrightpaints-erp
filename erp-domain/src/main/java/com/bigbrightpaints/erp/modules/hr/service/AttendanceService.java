package com.bigbrightpaints.erp.modules.hr.service;

import com.bigbrightpaints.erp.modules.company.domain.Company;
import com.bigbrightpaints.erp.modules.company.service.CompanyContextService;
import com.bigbrightpaints.erp.modules.hr.domain.*;
import com.bigbrightpaints.erp.core.util.CompanyClock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing attendance.
 * - Staff marks their own attendance
 * - Factory portal marks labourers' attendance
 */
@Service
public class AttendanceService {

    private static final Logger log = LoggerFactory.getLogger(AttendanceService.class);

    private final AttendanceRepository attendanceRepository;
    private final EmployeeRepository employeeRepository;
    private final CompanyContextService companyContextService;
    private final CompanyClock companyClock;

    public AttendanceService(AttendanceRepository attendanceRepository,
                            EmployeeRepository employeeRepository,
                            CompanyContextService companyContextService,
                            CompanyClock companyClock) {
        this.attendanceRepository = attendanceRepository;
        this.employeeRepository = employeeRepository;
        this.companyContextService = companyContextService;
        this.companyClock = companyClock;
    }

    /**
     * Staff marks their own attendance (self check-in)
     */
    @Transactional
    public AttendanceDto markSelfPresent() {
        Company company = companyContextService.requireCurrentCompany();
        String username = getCurrentUsername();
        
        // Find employee by email (linked to user account)
        Employee employee = employeeRepository.findByCompanyAndEmail(company, username)
                .orElseThrow(() -> com.bigbrightpaints.erp.core.validation.ValidationUtils.invalidInput("No employee record found for user: " + username));
        
        return markPresent(employee.getId(), Attendance.AttendanceStatus.PRESENT, null);
    }

    /**
     * Mark an employee/labourer as present (for Factory portal to mark labourers)
     */
    @Transactional
    public AttendanceDto markPresent(Long employeeId, Attendance.AttendanceStatus status, String remarks) {
        Company company = companyContextService.requireCurrentCompany();
        Employee employee = employeeRepository.findByCompanyAndId(company, employeeId)
                .orElseThrow(() -> com.bigbrightpaints.erp.core.validation.ValidationUtils.invalidInput("Employee not found"));

        LocalDate today = companyClock.today(company);
        ZoneId zoneId = companyClock.zoneId(company);
        
        // Check if already marked
        Optional<Attendance> existing = attendanceRepository.findByCompanyAndEmployeeAndAttendanceDate(
                company, employee, today);
        
        Attendance attendance;
        if (existing.isPresent()) {
            attendance = existing.get();
            attendance.setStatus(status);
            attendance.setRemarks(remarks);
            log.info("Updated attendance for {} to {}", employee.getFullName(), status);
        } else {
            attendance = new Attendance();
            attendance.setCompany(company);
            attendance.setEmployee(employee);
            attendance.setAttendanceDate(today);
            attendance.setStatus(status);
            attendance.setCheckInTime(LocalTime.ofInstant(companyClock.now(company), zoneId));
            attendance.setRemarks(remarks);
            attendance.setWeekend(isWeekend(today));
            log.info("Marked {} as {} for {}", employee.getFullName(), status, today);
        }
        
        attendance.setMarkedBy(getCurrentUsername());
        attendanceRepository.save(attendance);
        
        return toDto(attendance);
    }

    /**
     * Mark half day attendance
     */
    @Transactional
    public AttendanceDto markHalfDay(Long employeeId, String remarks) {
        return markPresent(employeeId, Attendance.AttendanceStatus.HALF_DAY, remarks);
    }

    /**
     * Get today's attendance for all employees
     */
    @Transactional(readOnly = true)
    public List<AttendanceDto> getTodayAttendance() {
        Company company = companyContextService.requireCurrentCompany();
        LocalDate today = companyClock.today(company);
        return attendanceRepository.findByCompanyAndAttendanceDateOrderByEmployeeFirstNameAsc(company, today)
                .stream()
                .map(this::toDto)
                .toList();
    }

    /**
     * Get attendance for a specific employee in date range
     */
    @Transactional(readOnly = true)
    public List<AttendanceDto> getEmployeeAttendance(Long employeeId, LocalDate startDate, LocalDate endDate) {
        Company company = companyContextService.requireCurrentCompany();
        Employee employee = employeeRepository.findByCompanyAndId(company, employeeId)
                .orElseThrow(() -> com.bigbrightpaints.erp.core.validation.ValidationUtils.invalidInput("Employee not found"));
        
        return attendanceRepository.findByCompanyAndEmployeeAndAttendanceDateBetweenOrderByAttendanceDateAsc(
                company, employee, startDate, endDate)
                .stream()
                .map(this::toDto)
                .toList();
    }

    /**
     * Get attendance summary for payroll calculation
     */
    @Transactional(readOnly = true)
    public AttendanceSummary getAttendanceSummary(Long employeeId, LocalDate startDate, LocalDate endDate) {
        Company company = companyContextService.requireCurrentCompany();
        Employee employee = employeeRepository.findByCompanyAndId(company, employeeId)
                .orElseThrow(() -> com.bigbrightpaints.erp.core.validation.ValidationUtils.invalidInput("Employee not found"));
        
        long presentDays = attendanceRepository.countByEmployeeAndDateRangeAndStatus(
                company, employee, startDate, endDate, Attendance.AttendanceStatus.PRESENT);
        long halfDays = attendanceRepository.countByEmployeeAndDateRangeAndStatus(
                company, employee, startDate, endDate, Attendance.AttendanceStatus.HALF_DAY);
        long absentDays = attendanceRepository.countByEmployeeAndDateRangeAndStatus(
                company, employee, startDate, endDate, Attendance.AttendanceStatus.ABSENT);
        long leaveDays = attendanceRepository.countByEmployeeAndDateRangeAndStatus(
                company, employee, startDate, endDate, Attendance.AttendanceStatus.LEAVE);
        
        return new AttendanceSummary(
                employeeId,
                employee.getFullName(),
                startDate,
                endDate,
                (int) presentDays,
                (int) halfDays,
                (int) absentDays,
                (int) leaveDays
        );
    }

    /**
     * Search labourers by name (for Factory portal)
     */
    @Transactional(readOnly = true)
    public List<EmployeeSearchResult> searchLabourers(String query) {
        Company company = companyContextService.requireCurrentCompany();
        return employeeRepository.findByCompanyAndEmployeeTypeAndStatus(
                company, Employee.EmployeeType.LABOUR, "ACTIVE")
                .stream()
                .filter(e -> e.getFullName().toLowerCase().contains(query.toLowerCase()))
                .map(e -> new EmployeeSearchResult(
                        e.getId(),
                        e.getFullName(),
                        e.getDailyWage(),
                        isMarkedToday(company, e)
                ))
                .toList();
    }

    /**
     * Bulk mark attendance for multiple labourers (Factory portal)
     */
    @Transactional
    public int bulkMarkPresent(List<Long> employeeIds, Attendance.AttendanceStatus status) {
        int count = 0;
        for (Long id : employeeIds) {
            try {
                markPresent(id, status, "Bulk marked");
                count++;
            } catch (Exception e) {
                log.warn("Failed to mark attendance for employee {}: {}", id, e.getMessage());
            }
        }
        return count;
    }

    private boolean isMarkedToday(Company company, Employee employee) {
        return attendanceRepository.existsByCompanyAndEmployeeAndAttendanceDate(
                company, employee, companyClock.today(company));
    }

    private boolean isWeekend(LocalDate date) {
        DayOfWeek day = date.getDayOfWeek();
        return day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
    }

    private String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "system";
    }

    private AttendanceDto toDto(Attendance attendance) {
        return new AttendanceDto(
                attendance.getId(),
                attendance.getEmployee().getId(),
                attendance.getEmployee().getFullName(),
                attendance.getEmployee().getEmployeeType().name(),
                attendance.getAttendanceDate(),
                attendance.getStatus().name(),
                attendance.getCheckInTime(),
                attendance.getCheckOutTime(),
                attendance.getMarkedBy(),
                attendance.getRemarks(),
                attendance.isWeekend()
        );
    }

    // DTOs
    public record AttendanceDto(
            Long id,
            Long employeeId,
            String employeeName,
            String employeeType,
            LocalDate date,
            String status,
            LocalTime checkInTime,
            LocalTime checkOutTime,
            String markedBy,
            String remarks,
            boolean weekend
    ) {}

    public record AttendanceSummary(
            Long employeeId,
            String employeeName,
            LocalDate startDate,
            LocalDate endDate,
            int presentDays,
            int halfDays,
            int absentDays,
            int leaveDays
    ) {
        public double getEffectiveDays() {
            return presentDays + (halfDays * 0.5);
        }
    }

    public record EmployeeSearchResult(
            Long id,
            String name,
            java.math.BigDecimal dailyWage,
            boolean markedToday
    ) {}
}
