package com.bigbrightpaints.erp.modules.hr.service;

import com.bigbrightpaints.erp.core.exception.ApplicationException;
import com.bigbrightpaints.erp.core.exception.ErrorCode;
import com.bigbrightpaints.erp.core.util.CompanyEntityLookup;
import com.bigbrightpaints.erp.modules.company.domain.Company;
import com.bigbrightpaints.erp.modules.company.service.CompanyContextService;
import com.bigbrightpaints.erp.modules.hr.domain.Employee;
import com.bigbrightpaints.erp.modules.hr.domain.EmployeeRepository;
import com.bigbrightpaints.erp.modules.hr.dto.EmployeeDto;
import com.bigbrightpaints.erp.modules.hr.dto.EmployeeRequest;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmployeeService {

    private final CompanyContextService companyContextService;
    private final EmployeeRepository employeeRepository;
    private final CompanyEntityLookup companyEntityLookup;

    public EmployeeService(CompanyContextService companyContextService,
                           EmployeeRepository employeeRepository,
                           CompanyEntityLookup companyEntityLookup) {
        this.companyContextService = companyContextService;
        this.employeeRepository = employeeRepository;
        this.companyEntityLookup = companyEntityLookup;
    }

    public List<EmployeeDto> listEmployees() {
        Company company = companyContextService.requireCurrentCompany();
        return employeeRepository.findByCompanyOrderByFirstNameAsc(company)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public EmployeeDto createEmployee(EmployeeRequest request) {
        if (request == null) {
            throw new ApplicationException(ErrorCode.VALIDATION_MISSING_REQUIRED_FIELD,
                    "Employee request is required");
        }

        Company company = companyContextService.requireCurrentCompany();
        Employee employee = new Employee();
        employee.setCompany(company);
        applyMutableFields(employee, request);
        return toDto(employeeRepository.save(employee));
    }

    @Transactional
    public EmployeeDto updateEmployee(Long id, EmployeeRequest request) {
        if (request == null) {
            throw new ApplicationException(ErrorCode.VALIDATION_MISSING_REQUIRED_FIELD,
                    "Employee request is required");
        }

        Company company = companyContextService.requireCurrentCompany();
        Employee employee = employeeRepository.lockByCompanyAndId(company, id)
                .orElseThrow(() -> new ApplicationException(
                        ErrorCode.VALIDATION_INVALID_REFERENCE,
                        "Employee not found"));

        applyMutableFields(employee, request);
        return toDto(employeeRepository.save(employee));
    }

    @Transactional
    public void deleteEmployee(Long id) {
        Company company = companyContextService.requireCurrentCompany();
        Employee employee = companyEntityLookup.requireEmployee(company, id);
        employeeRepository.delete(employee);
    }

    private void applyMutableFields(Employee employee, EmployeeRequest request) {
        employee.setFirstName(request.firstName());
        employee.setLastName(request.lastName());
        employee.setEmail(request.email());
        employee.setRole(request.role());
        employee.setHiredDate(request.hiredDate());

        if (request.phone() != null) {
            employee.setPhone(request.phone());
        }
        if (request.employeeType() != null) {
            employee.setEmployeeType(parseEmployeeType(request.employeeType()));
        }
        if (request.paymentSchedule() != null) {
            employee.setPaymentSchedule(parsePaymentSchedule(request.paymentSchedule()));
        }
        if (request.monthlySalary() != null) {
            employee.setMonthlySalary(request.monthlySalary());
        }
        if (request.dailyWage() != null) {
            employee.setDailyWage(request.dailyWage());
        }
        if (request.workingDaysPerMonth() != null) {
            employee.setWorkingDaysPerMonth(request.workingDaysPerMonth());
        }
        if (request.weeklyOffDays() != null) {
            employee.setWeeklyOffDays(request.weeklyOffDays());
        }
        if (request.standardHoursPerDay() != null) {
            employee.setStandardHoursPerDay(request.standardHoursPerDay());
        }
        if (request.overtimeRateMultiplier() != null) {
            employee.setOvertimeRateMultiplier(request.overtimeRateMultiplier());
        }
        if (request.doubleOtRateMultiplier() != null) {
            employee.setDoubleOtRateMultiplier(request.doubleOtRateMultiplier());
        }
        if (request.bankAccountNumber() != null) {
            employee.setBankAccountNumber(request.bankAccountNumber());
        }
        if (request.bankName() != null) {
            employee.setBankName(request.bankName());
        }
        if (request.ifscCode() != null) {
            employee.setIfscCode(request.ifscCode());
        }
    }

    private Employee.EmployeeType parseEmployeeType(String rawEmployeeType) {
        try {
            return Employee.EmployeeType.valueOf(rawEmployeeType.trim().toUpperCase(Locale.ROOT));
        } catch (RuntimeException ex) {
            throw new ApplicationException(ErrorCode.VALIDATION_INVALID_INPUT,
                    "Invalid employeeType. Allowed values: "
                            + Arrays.toString(Employee.EmployeeType.values()))
                    .withDetail("employeeType", rawEmployeeType);
        }
    }

    private Employee.PaymentSchedule parsePaymentSchedule(String rawPaymentSchedule) {
        try {
            return Employee.PaymentSchedule.valueOf(rawPaymentSchedule.trim().toUpperCase(Locale.ROOT));
        } catch (RuntimeException ex) {
            throw new ApplicationException(ErrorCode.VALIDATION_INVALID_INPUT,
                    "Invalid paymentSchedule. Allowed values: "
                            + Arrays.toString(Employee.PaymentSchedule.values()))
                    .withDetail("paymentSchedule", rawPaymentSchedule);
        }
    }

    private EmployeeDto toDto(Employee employee) {
        return new EmployeeDto(
                employee.getId(),
                employee.getPublicId(),
                employee.getFirstName(),
                employee.getLastName(),
                employee.getEmail(),
                employee.getRole(),
                employee.getStatus(),
                employee.getHiredDate());
    }
}
