package com.bigbrightpaints.erp.modules.hr.service;

import com.bigbrightpaints.erp.core.exception.ApplicationException;
import com.bigbrightpaints.erp.core.exception.ErrorCode;
import com.bigbrightpaints.erp.core.util.CompanyEntityLookup;
import com.bigbrightpaints.erp.modules.company.domain.Company;
import com.bigbrightpaints.erp.modules.company.service.CompanyContextService;
import com.bigbrightpaints.erp.modules.hr.domain.Employee;
import com.bigbrightpaints.erp.modules.hr.domain.EmployeeRepository;
import com.bigbrightpaints.erp.modules.hr.domain.LeaveRequest;
import com.bigbrightpaints.erp.modules.hr.domain.LeaveRequestRepository;
import com.bigbrightpaints.erp.modules.hr.dto.LeaveRequestDto;
import com.bigbrightpaints.erp.modules.hr.dto.LeaveRequestRequest;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LeaveService {

    private final CompanyContextService companyContextService;
    private final EmployeeRepository employeeRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final CompanyEntityLookup companyEntityLookup;

    public LeaveService(CompanyContextService companyContextService,
                        EmployeeRepository employeeRepository,
                        LeaveRequestRepository leaveRequestRepository,
                        CompanyEntityLookup companyEntityLookup) {
        this.companyContextService = companyContextService;
        this.employeeRepository = employeeRepository;
        this.leaveRequestRepository = leaveRequestRepository;
        this.companyEntityLookup = companyEntityLookup;
    }

    public List<LeaveRequestDto> listLeaveRequests() {
        Company company = companyContextService.requireCurrentCompany();
        return leaveRequestRepository.findByCompanyOrderByCreatedAtDesc(company)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public LeaveRequestDto createLeaveRequest(LeaveRequestRequest request) {
        Company company = companyContextService.requireCurrentCompany();
        if (request.employeeId() == null) {
            throw new ApplicationException(ErrorCode.VALIDATION_INVALID_INPUT,
                    "Employee is required for leave request");
        }

        Employee employee = employeeRepository.lockByCompanyAndId(company, request.employeeId())
                .orElseThrow(() -> new ApplicationException(
                        ErrorCode.VALIDATION_INVALID_REFERENCE,
                        "Employee not found"));

        if (leaveRequestRepository.existsOverlappingByEmployeeIdAndDates(
                request.employeeId(), request.startDate(), request.endDate())) {
            throw new ApplicationException(ErrorCode.VALIDATION_INVALID_INPUT,
                    "Overlapping leave request exists for employee");
        }

        LeaveRequest leaveRequest = new LeaveRequest();
        leaveRequest.setCompany(company);
        leaveRequest.setEmployee(employee);
        leaveRequest.setLeaveType(request.leaveType());
        leaveRequest.setStartDate(request.startDate());
        leaveRequest.setEndDate(request.endDate());
        leaveRequest.setReason(request.reason());
        if (request.status() != null) {
            leaveRequest.setStatus(parseLeaveStatus(request.status()));
        }

        return toDto(leaveRequestRepository.save(leaveRequest));
    }

    @Transactional
    public LeaveRequestDto updateLeaveStatus(Long id, String status) {
        Company company = companyContextService.requireCurrentCompany();
        LeaveRequest leaveRequest = companyEntityLookup.requireLeaveRequest(company, id);
        leaveRequest.setStatus(parseLeaveStatus(status));
        return toDto(leaveRequest);
    }

    private String parseLeaveStatus(String rawLeaveStatus) {
        try {
            return LeaveStatus.valueOf(rawLeaveStatus.trim().toUpperCase(Locale.ROOT)).name();
        } catch (RuntimeException ex) {
            throw new ApplicationException(ErrorCode.VALIDATION_INVALID_INPUT,
                    "Invalid leave status. Allowed values: "
                            + Arrays.toString(LeaveStatus.values()))
                    .withDetail("leaveStatus", rawLeaveStatus);
        }
    }

    private LeaveRequestDto toDto(LeaveRequest leaveRequest) {
        String employeeName = leaveRequest.getEmployee() != null
                ? leaveRequest.getEmployee().getFirstName() + " " + leaveRequest.getEmployee().getLastName()
                : null;
        Long employeeId = leaveRequest.getEmployee() != null ? leaveRequest.getEmployee().getId() : null;
        return new LeaveRequestDto(
                leaveRequest.getId(),
                leaveRequest.getPublicId(),
                employeeId,
                employeeName,
                leaveRequest.getLeaveType(),
                leaveRequest.getStartDate(),
                leaveRequest.getEndDate(),
                leaveRequest.getStatus(),
                leaveRequest.getReason(),
                leaveRequest.getCreatedAt());
    }

    private enum LeaveStatus {
        PENDING,
        APPROVED,
        REJECTED,
        CANCELLED
    }
}
