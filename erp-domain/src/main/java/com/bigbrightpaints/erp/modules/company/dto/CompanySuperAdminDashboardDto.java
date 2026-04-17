package com.bigbrightpaints.erp.modules.company.dto;

import java.math.BigDecimal;
import java.util.List;

public record CompanySuperAdminDashboardDto(
    long totalTenants,
    long activeTenants,
    long suspendedTenants,
    long deactivatedTenants,
    long totalActiveUsers,
    long totalActiveUserQuota,
    long totalAuditStorageBytes,
    long totalStorageQuotaBytes,
    long totalCurrentConcurrentRequests,
    long totalConcurrentRequestQuota,
    BillingSummary billingSummary,
    List<TenantOverview> tenants) {

  public record BillingSummary(
      BigDecimal totalMonthlyRecurringRevenue,
      BigDecimal totalAnnualRecurringRevenue,
      long billedTenantCount) {}

  public record TenantOverview(
      Long companyId,
      String companyCode,
      String companyName,
      String region,
      String lifecycleState,
      String lifecycleReason,
      long activeUsers,
      long activeUserQuota,
      long auditStorageBytes,
      long storageQuotaBytes,
      long currentConcurrentRequests,
      long concurrentRequestQuota,
      long apiActivityCount,
      long apiRequestQuota,
      long apiErrorCount,
      long apiErrorRateInBasisPoints,
      boolean quotaSoftLimitEnabled,
      boolean quotaHardLimitEnabled,
      long activeUserUtilizationInBasisPoints,
      long auditStorageUtilizationInBasisPoints,
      long concurrentRequestUtilizationInBasisPoints) {}
}
