package com.bigbrightpaints.erp.modules.hr.domain;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bigbrightpaints.erp.modules.company.domain.Company;

public interface LeaveBalanceRepository extends JpaRepository<LeaveBalance, Long> {

  Optional<LeaveBalance> findByCompanyAndEmployeeAndLeaveTypeAndBalanceYear(
      Company company, Employee employee, String leaveType, Integer balanceYear);

  List<LeaveBalance> findByCompanyAndEmployeeAndBalanceYearOrderByLeaveTypeAsc(
      Company company, Employee employee, Integer balanceYear);
}
