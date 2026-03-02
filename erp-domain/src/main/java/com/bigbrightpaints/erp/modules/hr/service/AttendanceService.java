package com.bigbrightpaints.erp.modules.hr.service;

import com.bigbrightpaints.erp.core.exception.ApplicationException;
import com.bigbrightpaints.erp.core.exception.ErrorCode;
import com.bigbrightpaints.erp.core.util.CompanyClock;
import com.bigbrightpaints.erp.core.util.CompanyEntityLookup;
import com.bigbrightpaints.erp.core.util.CompanyTime;
import com.bigbrightpaints.erp.modules.company.domain.Company;
import com.bigbrightpaints.erp.modules.company.service.CompanyContextService;
import com.bigbrightpaints.erp.modules.hr.domain.Attendance;
import com.bigbrightpaints.erp.modules.hr.domain.AttendanceRepository;
import com.bigbrightpaints.erp.modules.hr.domain.Employee;
import com.bigbrightpaints.erp.modules.hr.domain.EmployeeRepository;
import com.bigbrightpaints.erp.modules.hr.dto.AttendanceDto;
import com.bigbrightpaints.erp.modules.hr.dto.AttendanceSummaryDto;
import com.bigbrightpaints.erp.modules.hr.dto.BulkMarkAttendanceRequest;
import com.bigbrightpaints.erp.modules.hr.dto.MarkAttendanceRequest;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AttendanceService {

    private final CompanyContextService companyContextService;
    private final AttendanceRepository attendanceRepository;
    private final EmployeeRepository employeeRepository;
    private final CompanyEntityLookup companyEntityLookup;
    private final CompanyClock companyClock;

    public AttendanceService(CompanyContextService companyContextService,
                             AttendanceRepository attendanceRepository,
                             EmployeeRepository employeeRepository,
                             CompanyEntityLookup companyEntityLookup,
                             CompanyClock companyClock) {
        this.companyContextService = companyContextService;
        this.attendanceRepository = attendanceRepository;
        this.employeeRepository = employeeRepository;
        this.companyEntityLookup = companyEntityLookup;
        this.companyClock = companyClock;
    }

    public List<AttendanceDto> listAttendanceByDate(LocalDate date) {
        Company company = companyContextService.requireCurrentCompany();
        return attendanceRepository.findByCompanyAndAttendanceDateOrderByEmployeeFirstNameAsc(company, date)
                .stream()
                .map(this::toAttendanceDto)
                .toList();
    }

    public List<AttendanceDto> listEmployeeAttendance(Long employeeId,
                                                      LocalDate startDate,
                                                      LocalDate endDate) {
        Company company = companyContextService.requireCurrentCompany();
        Employee employee = companyEntityLookup.requireEmployee(company, employeeId);
        return attendanceRepository.findByCompanyAndEmployeeAndAttendanceDateBetweenOrderByAttendanceDateAsc(
                        company,
                        employee,
                        startDate,
                        endDate)
                .stream()
                .map(this::toAttendanceDto)
                .toList();
    }

    @Transactional
    public AttendanceDto markAttendance(Long employeeId, MarkAttendanceRequest request) {
        Company company = companyContextService.requireCurrentCompany();
        Employee employee = companyEntityLookup.requireEmployee(company, employeeId);
        LocalDate date = request.date() != null ? request.date() : companyClock.today(company);

        Attendance attendance = attendanceRepository.findByCompanyAndEmployeeAndAttendanceDate(
                        company,
                        employee,
                        date)
                .orElseGet(() -> newAttendance(company, employee, date));

        applyAttendanceRequest(attendance, request.status(), request.checkInTime(), request.checkOutTime(),
                request.regularHours(), request.overtimeHours(), request.doubleOvertimeHours(),
                request.holiday(), request.weekend(), request.remarks(), company);

        return toAttendanceDto(attendanceRepository.save(attendance));
    }

    @Transactional
    public List<AttendanceDto> bulkMarkAttendance(BulkMarkAttendanceRequest request) {
        Company company = companyContextService.requireCurrentCompany();
        List<Long> employeeIds = request.employeeIds();
        if (employeeIds == null || employeeIds.isEmpty()) {
            return List.of();
        }

        // Validate status upfront so invalid status consistently surfaces as invalid input.
        parseAttendanceStatus(request.status());

        List<Employee> employees = employeeRepository.findAllById(employeeIds).stream()
                .filter(employee -> employee.getCompany() != null
                        && company.getId() != null
                        && company.getId().equals(employee.getCompany().getId()))
                .toList();

        if (employees.size() != employeeIds.size()) {
            throw new ApplicationException(ErrorCode.VALIDATION_INVALID_REFERENCE,
                    "One or more employees were not found for the current company");
        }

        List<Attendance> existingRows = attendanceRepository.findByCompanyAndEmployeeIdsAndDateRange(
                company,
                employeeIds,
                request.date(),
                request.date());

        Map<Long, Attendance> existingByEmployeeId = existingRows.stream()
                .collect(Collectors.toMap(row -> row.getEmployee().getId(), Function.identity()));

        String status = request.status();
        List<Attendance> toPersist = new ArrayList<>();
        for (Employee employee : employees) {
            Attendance attendance = existingByEmployeeId.get(employee.getId());
            if (attendance == null) {
                attendance = newAttendance(company, employee, request.date());
            }
            applyAttendanceRequest(attendance, status, request.checkInTime(), request.checkOutTime(),
                    request.regularHours(), request.overtimeHours(), null,
                    false, false, request.remarks(), company);
            toPersist.add(attendance);
        }

        return attendanceRepository.saveAll(toPersist)
                .stream()
                .map(this::toAttendanceDto)
                .toList();
    }

    public AttendanceSummaryDto getTodayAttendanceSummary() {
        Company company = companyContextService.requireCurrentCompany();
        LocalDate today = companyClock.today(company);

        List<Attendance> attendances = attendanceRepository.findByCompanyAndAttendanceDateOrderByEmployeeFirstNameAsc(
                company,
                today);
        long totalEmployees = employeeRepository.countByCompanyAndStatus(company, "ACTIVE");
        long present = attendances.stream()
                .filter(attendance -> attendance.getStatus() == Attendance.AttendanceStatus.PRESENT)
                .count();
        long absent = attendances.stream()
                .filter(attendance -> attendance.getStatus() == Attendance.AttendanceStatus.ABSENT)
                .count();
        long halfDay = attendances.stream()
                .filter(attendance -> attendance.getStatus() == Attendance.AttendanceStatus.HALF_DAY)
                .count();
        long leave = attendances.stream()
                .filter(attendance -> attendance.getStatus() == Attendance.AttendanceStatus.LEAVE)
                .count();
        long notMarked = totalEmployees - attendances.size();

        return new AttendanceSummaryDto(today, totalEmployees, present, absent, halfDay, leave, notMarked);
    }

    private Attendance newAttendance(Company company, Employee employee, LocalDate date) {
        Attendance attendance = new Attendance();
        attendance.setCompany(company);
        attendance.setEmployee(employee);
        attendance.setAttendanceDate(date);
        return attendance;
    }

    private void applyAttendanceRequest(Attendance attendance,
                                        String rawStatus,
                                        java.time.LocalTime checkInTime,
                                        java.time.LocalTime checkOutTime,
                                        java.math.BigDecimal regularHours,
                                        java.math.BigDecimal overtimeHours,
                                        java.math.BigDecimal doubleOvertimeHours,
                                        boolean holiday,
                                        boolean weekend,
                                        String remarks,
                                        Company company) {
        attendance.setStatus(parseAttendanceStatus(rawStatus));
        if (checkInTime != null) {
            attendance.setCheckInTime(checkInTime);
        }
        if (checkOutTime != null) {
            attendance.setCheckOutTime(checkOutTime);
        }
        if (regularHours != null) {
            attendance.setRegularHours(regularHours);
        }
        if (overtimeHours != null) {
            attendance.setOvertimeHours(overtimeHours);
        }
        if (doubleOvertimeHours != null) {
            attendance.setDoubleOvertimeHours(doubleOvertimeHours);
        }
        attendance.setHoliday(holiday);
        attendance.setWeekend(weekend);
        if (remarks != null) {
            attendance.setRemarks(remarks);
        }
        attendance.setMarkedBy(getCurrentUser());
        attendance.setMarkedAt(CompanyTime.now(company));
    }

    private Attendance.AttendanceStatus parseAttendanceStatus(String rawAttendanceStatus) {
        try {
            return Attendance.AttendanceStatus.valueOf(rawAttendanceStatus.trim().toUpperCase(Locale.ROOT));
        } catch (RuntimeException ex) {
            throw new ApplicationException(ErrorCode.VALIDATION_INVALID_INPUT,
                    "Invalid attendance status. Allowed values: "
                            + Arrays.toString(Attendance.AttendanceStatus.values()))
                    .withDetail("attendanceStatus", rawAttendanceStatus);
        }
    }

    private AttendanceDto toAttendanceDto(Attendance attendance) {
        Employee employee = attendance.getEmployee();
        return new AttendanceDto(
                attendance.getId(),
                employee.getId(),
                employee.getFirstName() + " " + employee.getLastName(),
                employee.getEmployeeType() != null ? employee.getEmployeeType().name() : null,
                attendance.getAttendanceDate(),
                attendance.getStatus().name(),
                attendance.getCheckInTime(),
                attendance.getCheckOutTime(),
                attendance.getRegularHours(),
                attendance.getOvertimeHours(),
                attendance.getDoubleOvertimeHours(),
                attendance.isHoliday(),
                attendance.isWeekend(),
                attendance.getRemarks(),
                attendance.getMarkedBy(),
                attendance.getMarkedAt());
    }

    private String getCurrentUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null ? authentication.getName() : "SYSTEM";
    }
}
