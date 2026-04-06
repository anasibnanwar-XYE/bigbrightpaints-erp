package com.bigbrightpaints.erp.modules.hr.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bigbrightpaints.erp.core.util.CompanyScopedLookupService;
import com.bigbrightpaints.erp.modules.company.domain.Company;
import com.bigbrightpaints.erp.modules.hr.domain.Employee;
import com.bigbrightpaints.erp.modules.hr.domain.EmployeeRepository;
import com.bigbrightpaints.erp.modules.hr.domain.LeaveRequest;
import com.bigbrightpaints.erp.modules.hr.domain.LeaveRequestRepository;
import com.bigbrightpaints.erp.modules.hr.domain.PayrollRun;
import com.bigbrightpaints.erp.modules.hr.domain.PayrollRunRepository;

@Service
public class CompanyScopedHrLookupService {

  private final CompanyScopedLookupService companyScopedLookupService;
  private final EmployeeRepository employeeRepository;
  private final LeaveRequestRepository leaveRequestRepository;
  private final PayrollRunRepository payrollRunRepository;

  @Autowired
  public CompanyScopedHrLookupService(
      CompanyScopedLookupService companyScopedLookupService,
      EmployeeRepository employeeRepository,
      LeaveRequestRepository leaveRequestRepository,
      PayrollRunRepository payrollRunRepository) {
    this.companyScopedLookupService = companyScopedLookupService;
    this.employeeRepository = employeeRepository;
    this.leaveRequestRepository = leaveRequestRepository;
    this.payrollRunRepository = payrollRunRepository;
  }

  public Employee requireEmployee(Company company, Long employeeId) {
    return companyScopedLookupService.require(
        company, employeeId, employeeRepository::findByCompanyAndId, "Employee");
  }

  public LeaveRequest requireLeaveRequest(Company company, Long leaveRequestId) {
    return companyScopedLookupService.require(
        company, leaveRequestId, leaveRequestRepository::findByCompanyAndId, "Leave request");
  }

  public PayrollRun lockPayrollRun(Company company, Long payrollRunId) {
    return companyScopedLookupService.require(
        company, payrollRunId, payrollRunRepository::lockByCompanyAndId, "Payroll run");
  }
}
