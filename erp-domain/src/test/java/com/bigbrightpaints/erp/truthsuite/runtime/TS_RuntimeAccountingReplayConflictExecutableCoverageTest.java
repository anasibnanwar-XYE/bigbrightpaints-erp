package com.bigbrightpaints.erp.truthsuite.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.bigbrightpaints.erp.modules.accounting.service.AccountingFacade;
import com.bigbrightpaints.erp.modules.accounting.service.AccountingService;
import com.bigbrightpaints.erp.modules.accounting.service.CreditDebitNoteService;
import com.bigbrightpaints.erp.modules.accounting.service.DealerReceiptService;
import com.bigbrightpaints.erp.modules.accounting.service.InventoryAccountingService;
import com.bigbrightpaints.erp.modules.accounting.service.JournalEntryService;
import com.bigbrightpaints.erp.modules.accounting.service.SettlementService;

@Tag("critical")
class TS_RuntimeAccountingReplayConflictExecutableCoverageTest {

  @Test
  void accountingService_hides_retired_replay_helper_surface() {
    Set<String> methodNames =
        Arrays.stream(AccountingService.class.getDeclaredMethods())
            .map(method -> method.getName())
            .collect(Collectors.toSet());

    assertThat(methodNames)
        .doesNotContain(
            "validatePartnerJournalReplay",
            "validateSettlementIdempotencyKey",
            "missingReservedPartnerAllocation",
            "buildDealerReceiptReference",
            "toSettlementAllocationSummaries",
            "logSettlementAuditSuccess");
  }

  @Test
  void settlement_paths_route_through_focused_collaborators() {
    Set<String> settlementFieldTypes =
        Arrays.stream(SettlementService.class.getDeclaredFields())
            .map(field -> field.getType().getSimpleName())
            .collect(Collectors.toSet());

    assertThat(SettlementService.class.getSuperclass()).isEqualTo(Object.class);
    assertThat(settlementFieldTypes)
        .contains("SupplierPaymentService", "DealerSettlementService", "SupplierSettlementService");
  }

  @Test
  void accountingService_routes_sensitive_flows_through_composed_services() {
    Set<String> fieldTypes =
        Arrays.stream(AccountingService.class.getDeclaredFields())
            .map(field -> field.getType().getSimpleName())
            .collect(Collectors.toSet());

    assertThat(fieldTypes)
        .contains(
            "AccountResolutionOwnerService",
            JournalEntryService.class.getSimpleName(),
            DealerReceiptService.class.getSimpleName(),
            SettlementService.class.getSimpleName(),
            CreditDebitNoteService.class.getSimpleName(),
            InventoryAccountingService.class.getSimpleName());
  }

  @Test
  void payroll_specific_accounting_writes_are_owned_by_payroll_accounting_service_via_facade() {
    Set<String> fieldTypes =
        Arrays.stream(AccountingFacade.class.getDeclaredFields())
            .map(field -> field.getType().getSimpleName())
            .collect(Collectors.toSet());

    assertThat(fieldTypes).contains("PayrollAccountingService");
  }

  @Test
  void journal_entry_service_routes_manual_paths_through_focused_collaborators() {
    Set<String> fieldTypes =
        Arrays.stream(JournalEntryService.class.getDeclaredFields())
            .map(field -> field.getType().getSimpleName())
            .collect(Collectors.toSet());

    assertThat(fieldTypes).contains("ManualJournalService", "ClosingEntryReversalService");
  }
}
