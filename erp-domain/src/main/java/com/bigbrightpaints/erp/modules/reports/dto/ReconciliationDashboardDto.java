package com.bigbrightpaints.erp.modules.reports.dto;

import java.util.List;

public record ReconciliationDashboardDto(
    BankReconciliationDashboardDto bank,
    SubledgerReconciliationDashboardDto subledger,
    InventoryReconciliationDashboardDto inventory,
    List<BalanceWarningDto> balanceWarnings) {}
