package com.bigbrightpaints.erp.modules.hr.domain;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PayrollRunLineRepository extends JpaRepository<PayrollRunLine, Long> {

  List<PayrollRunLine> findByPayrollRunOrderByEmployeeFirstNameAsc(PayrollRun payrollRun);

  @Query(
      "SELECT prl FROM PayrollRunLine prl JOIN FETCH prl.employee "
          + "WHERE prl.payrollRun = :run "
          + "ORDER BY prl.employee.firstName ASC")
  List<PayrollRunLine> findByPayrollRunWithEmployeeOrderByEmployeeFirstNameAsc(
      @Param("run") PayrollRun run);

  List<PayrollRunLine> findByPayrollRun(PayrollRun payrollRun);

  Optional<PayrollRunLine> findByPayrollRunAndEmployee(PayrollRun payrollRun, Employee employee);

  List<PayrollRunLine> findByEmployeeOrderByPayrollRunCreatedAtDesc(Employee employee);

  @Query("SELECT SUM(prl.netPay) FROM PayrollRunLine prl WHERE prl.payrollRun = :run")
  BigDecimal sumNetPayByPayrollRun(@Param("run") PayrollRun run);

  @Query(
      "SELECT prl FROM PayrollRunLine prl WHERE prl.payrollRun = :run "
          + "AND prl.paymentStatus = :status")
  List<PayrollRunLine> findByPayrollRunAndPaymentStatus(
      @Param("run") PayrollRun run, @Param("status") PayrollRunLine.PaymentStatus status);

  void deleteByPayrollRun(PayrollRun payrollRun);
}
