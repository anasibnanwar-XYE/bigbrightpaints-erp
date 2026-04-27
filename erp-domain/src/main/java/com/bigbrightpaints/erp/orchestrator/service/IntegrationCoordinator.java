package com.bigbrightpaints.erp.orchestrator.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.bigbrightpaints.erp.core.exception.ApplicationException;
import com.bigbrightpaints.erp.core.exception.ErrorCode;
import com.bigbrightpaints.erp.modules.accounting.dto.JournalEntryDto;
import com.bigbrightpaints.erp.modules.hr.dto.PayrollRunDto;
import com.bigbrightpaints.erp.modules.inventory.service.FinishedGoodsService.InventoryReservationResult;

@Service
public class IntegrationCoordinator {

  private final OrderIntegrationCoordinator orderIntegrationCoordinator;
  private final PayrollIntegrationCoordinator payrollIntegrationCoordinator;
  private final DashboardIntegrationCoordinator dashboardIntegrationCoordinator;

  @Autowired
  public IntegrationCoordinator(
      OrderIntegrationCoordinator orderIntegrationCoordinator,
      PayrollIntegrationCoordinator payrollIntegrationCoordinator,
      DashboardIntegrationCoordinator dashboardIntegrationCoordinator) {
    this.orderIntegrationCoordinator = orderIntegrationCoordinator;
    this.payrollIntegrationCoordinator = payrollIntegrationCoordinator;
    this.dashboardIntegrationCoordinator = dashboardIntegrationCoordinator;
  }

  @Transactional
  public InventoryReservationResult reserveInventory(String orderId, String companyId) {
    return reserveInventory(orderId, companyId, null, null);
  }

  @Transactional
  public InventoryReservationResult reserveInventory(
      String orderId, String companyId, String traceId, String idempotencyKey) {
    return orderIntegrationCoordinator.reserveInventory(
        orderId, companyId, traceId, idempotencyKey);
  }

  @Transactional
  public void queueProduction(String orderId, String companyId) {
    orderIntegrationCoordinator.queueProduction(orderId, companyId);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public AutoApprovalResult autoApproveOrder(String orderId, String companyId) {
    return autoApproveOrder(orderId, companyId, null, null);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public AutoApprovalResult autoApproveOrder(
      String orderId, String companyId, String traceId, String idempotencyKey) {
    return orderIntegrationCoordinator.autoApproveOrder(
        orderId, companyId, traceId, idempotencyKey);
  }

  @Transactional
  public void updateProductionStatus(String planId, String companyId) {
    updateProductionStatus(planId, companyId, null, null);
  }

  @Transactional
  public void updateProductionStatus(
      String planId, String companyId, String traceId, String idempotencyKey) {
    orderIntegrationCoordinator.updateProductionStatus(planId, companyId, traceId, idempotencyKey);
  }

  @Transactional
  public AutoApprovalResult updateFulfillment(
      String orderId, String requestedStatus, String companyId) {
    return updateFulfillment(orderId, requestedStatus, companyId, null, null);
  }

  @Transactional
  public AutoApprovalResult updateFulfillment(
      String orderId,
      String requestedStatus,
      String companyId,
      String traceId,
      String idempotencyKey) {
    String normalizedStatus = requestedStatus == null ? "" : requestedStatus.trim().toUpperCase();
    switch (normalizedStatus) {
      case "SHIPPED":
      case "DISPATCHED":
      case "FULFILLED":
      case "COMPLETED":
        throw new ApplicationException(
                ErrorCode.BUSINESS_INVALID_STATE,
                "Orchestrator cannot update dispatch-like statuses. Use /api/v1/dispatch/confirm.")
            .withDetail("canonicalPath", "/api/v1/dispatch/confirm")
            .withDetail("requestedStatus", requestedStatus);
      default:
        break;
    }
    return orderIntegrationCoordinator.updateFulfillment(
        orderId, requestedStatus, companyId, traceId, idempotencyKey);
  }

  @Transactional(readOnly = true)
  public void syncEmployees(String companyId) {
    syncEmployees(companyId, null, null);
  }

  @Transactional(readOnly = true)
  public void syncEmployees(String companyId, String traceId, String idempotencyKey) {
    payrollIntegrationCoordinator.syncEmployees(companyId, traceId, idempotencyKey);
  }

  @Transactional
  public PayrollRunDto generatePayroll(
      LocalDate payrollDate, BigDecimal totalAmount, String companyId) {
    return generatePayroll(payrollDate, totalAmount, companyId, null, null);
  }

  @Transactional
  public PayrollRunDto generatePayroll(
      LocalDate payrollDate,
      BigDecimal totalAmount,
      String companyId,
      String traceId,
      String idempotencyKey) {
    return payrollIntegrationCoordinator.generatePayroll(
        payrollDate, totalAmount, companyId, traceId, idempotencyKey);
  }

  @Transactional
  public JournalEntryDto recordPayrollPayment(
      Long payrollRunId,
      BigDecimal amount,
      Long expenseAccountId,
      Long cashAccountId,
      String companyId) {
    return recordPayrollPayment(
        payrollRunId, amount, expenseAccountId, cashAccountId, companyId, null, null);
  }

  @Transactional
  public JournalEntryDto recordPayrollPayment(
      Long payrollRunId,
      BigDecimal amount,
      Long expenseAccountId,
      Long cashAccountId,
      String companyId,
      String traceId,
      String idempotencyKey) {
    return payrollIntegrationCoordinator.recordPayrollPayment(
        payrollRunId, amount, expenseAccountId, cashAccountId, companyId, traceId, idempotencyKey);
  }

  @Transactional(readOnly = true)
  public Map<String, Object> health() {
    return dashboardIntegrationCoordinator.health();
  }

  @Transactional(readOnly = true)
  public Map<String, Object> fetchAdminDashboard(String companyId) {
    return dashboardIntegrationCoordinator.fetchAdminDashboard(companyId);
  }

  @Transactional(readOnly = true)
  public Map<String, Object> fetchFactoryDashboard(String companyId) {
    return dashboardIntegrationCoordinator.fetchFactoryDashboard(companyId);
  }

  @Transactional(readOnly = true)
  public Map<String, Object> fetchFinanceDashboard(String companyId) {
    return dashboardIntegrationCoordinator.fetchFinanceDashboard(companyId);
  }

  public record AutoApprovalResult(String orderStatus, boolean awaitingProduction) {}
}
