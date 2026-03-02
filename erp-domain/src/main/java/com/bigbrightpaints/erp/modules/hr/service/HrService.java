package com.bigbrightpaints.erp.modules.hr.service;

import com.bigbrightpaints.erp.core.exception.ApplicationException;
import com.bigbrightpaints.erp.core.exception.ErrorCode;
import com.bigbrightpaints.erp.core.util.CompanyClock;
import com.bigbrightpaints.erp.core.util.CompanyEntityLookup;
import com.bigbrightpaints.erp.modules.company.service.CompanyContextService;
import com.bigbrightpaints.erp.modules.hr.domain.AttendanceRepository;
import com.bigbrightpaints.erp.modules.hr.domain.EmployeeRepository;
import com.bigbrightpaints.erp.modules.hr.domain.LeaveRequestRepository;
import com.bigbrightpaints.erp.modules.hr.domain.PayrollRunRepository;
import com.bigbrightpaints.erp.modules.hr.dto.AttendanceDto;
import com.bigbrightpaints.erp.modules.hr.dto.AttendanceSummaryDto;
import com.bigbrightpaints.erp.modules.hr.dto.BulkMarkAttendanceRequest;
import com.bigbrightpaints.erp.modules.hr.dto.EmployeeDto;
import com.bigbrightpaints.erp.modules.hr.dto.EmployeeRequest;
import com.bigbrightpaints.erp.modules.hr.dto.LeaveRequestDto;
import com.bigbrightpaints.erp.modules.hr.dto.LeaveRequestRequest;
import com.bigbrightpaints.erp.modules.hr.dto.MarkAttendanceRequest;
import com.bigbrightpaints.erp.modules.hr.dto.PayrollRunDto;
import com.bigbrightpaints.erp.modules.hr.dto.PayrollRunRequest;
import java.time.LocalDate;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HrService {

    private final EmployeeService employeeService;
    private final LeaveService leaveService;
    private final AttendanceService attendanceService;

    @Autowired
    public HrService(EmployeeService employeeService,
                     LeaveService leaveService,
                     AttendanceService attendanceService) {
        this.employeeService = employeeService;
        this.leaveService = leaveService;
        this.attendanceService = attendanceService;
    }

    @SuppressWarnings("unused")
    public HrService(CompanyContextService companyContextService,
                     EmployeeRepository employeeRepository,
                     LeaveRequestRepository leaveRequestRepository,
                     PayrollRunRepository payrollRunRepository,
                     AttendanceRepository attendanceRepository,
                     CompanyEntityLookup companyEntityLookup,
                     CompanyClock companyClock) {
        this(
                new EmployeeService(companyContextService, employeeRepository, companyEntityLookup),
                new LeaveService(companyContextService, employeeRepository, leaveRequestRepository, companyEntityLookup),
                new AttendanceService(companyContextService, attendanceRepository, employeeRepository, companyEntityLookup, companyClock)
        );
    }

    public List<EmployeeDto> listEmployees() {
        return employeeService.listEmployees();
    }

    @Transactional
    public EmployeeDto createEmployee(EmployeeRequest request) {
        return employeeService.createEmployee(request);
    }

    @Transactional
    public EmployeeDto updateEmployee(Long id, EmployeeRequest request) {
        return employeeService.updateEmployee(id, request);
    }

    @Transactional
    public void deleteEmployee(Long id) {
        employeeService.deleteEmployee(id);
    }

    public List<LeaveRequestDto> listLeaveRequests() {
        return leaveService.listLeaveRequests();
    }

    @Transactional
    public LeaveRequestDto createLeaveRequest(LeaveRequestRequest request) {
        return leaveService.createLeaveRequest(request);
    }

    @Transactional
    public LeaveRequestDto updateLeaveStatus(Long id, String status) {
        return leaveService.updateLeaveStatus(id, status);
    }

    @Deprecated
    @Transactional
    public PayrollRunDto createPayrollRun(PayrollRunRequest request) {
        throw new ApplicationException(ErrorCode.BUSINESS_CONSTRAINT_VIOLATION,
                "Legacy payroll run creation is deprecated; use /api/v1/payroll/runs")
                .withDetail("canonicalPath", "/api/v1/payroll/runs");
    }

    public List<AttendanceDto> listAttendanceByDate(LocalDate date) {
        return attendanceService.listAttendanceByDate(date);
    }

    public List<AttendanceDto> listEmployeeAttendance(Long employeeId,
                                                      LocalDate startDate,
                                                      LocalDate endDate) {
        return attendanceService.listEmployeeAttendance(employeeId, startDate, endDate);
    }

    @Transactional
    public AttendanceDto markAttendance(Long employeeId, MarkAttendanceRequest request) {
        return attendanceService.markAttendance(employeeId, request);
    }

    @Transactional
    public List<AttendanceDto> bulkMarkAttendance(BulkMarkAttendanceRequest request) {
        return attendanceService.bulkMarkAttendance(request);
    }

    public AttendanceSummaryDto getTodayAttendanceSummary() {
        return attendanceService.getTodayAttendanceSummary();
    }
}
