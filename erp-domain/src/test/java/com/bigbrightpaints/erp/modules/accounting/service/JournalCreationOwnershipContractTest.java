package com.bigbrightpaints.erp.modules.accounting.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

class JournalCreationOwnershipContractTest {

  @Test
  void journalEntryServiceUsesFocusedCollaboratorsInsteadOfInheritance() {
    assertThat(JournalEntryService.class.getSuperclass()).isEqualTo(Object.class);
    assertThat(fieldTypes(JournalEntryService.class))
        .contains(
            JournalPostingService.class,
            JournalQueryService.class,
            JournalReversalService.class,
            ManualJournalService.class,
            ClosingEntryReversalService.class);
  }

  @Test
  void accountingServiceUsesComposedCollaboratorsInsteadOfInheritance() {
    assertThat(AccountingService.class.getSuperclass()).isEqualTo(Object.class);
    assertThat(fieldTypes(AccountingService.class))
        .contains(
            AccountResolutionOwnerService.class,
            JournalEntryService.class,
            DealerReceiptService.class,
            SettlementService.class,
            CreditDebitNoteService.class,
            InventoryAccountingService.class)
        .doesNotContain(PayrollAccountingService.class);
  }

  @Test
  void accountingFacadeOwnsPayrollBoundaryDirectlyThroughFocusedService() {
    assertThat(fieldTypes(AccountingFacade.class)).contains(PayrollAccountingService.class);
  }

  @Test
  void settlementServiceUsesFocusedSettlementCollaborators() {
    assertThat(SettlementService.class.getSuperclass()).isEqualTo(Object.class);
    assertThat(fieldTypes(SettlementService.class))
        .contains(
            SupplierPaymentService.class,
            DealerSettlementService.class,
            SupplierSettlementService.class);
  }

  private Set<Class<?>> fieldTypes(Class<?> type) {
    return Arrays.stream(type.getDeclaredFields())
        .map(field -> field.getType())
        .collect(Collectors.toSet());
  }
}
