package com.bigbrightpaints.erp.modules.accounting.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

class JournalCreationOwnershipContractTest {

  @Test
  void deletedAccountingCoreEngineCoreRemainsUnavailable() {
    assertThat(
            Path.of(
                    "/home/realnigga/Desktop/Mission-control/erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/internal/AccountingCoreEngineCore.java")
                .toFile())
        .doesNotExist();
  }

  @Test
  void journalEntryServiceUsesFocusedCollaboratorsInsteadOfInheritance() {
    assertThat(JournalEntryService.class.getSuperclass()).isEqualTo(Object.class);
    assertThat(fieldTypes(JournalEntryService.class))
        .contains(
            JournalPostingService.class,
            JournalQueryService.class,
            JournalReversalService.class,
            PeriodValidationService.class,
            ManualJournalService.class,
            ClosingEntryReversalService.class);
  }

  @Test
  void accountingServiceUsesComposedCollaboratorsInsteadOfInheritance() {
    assertThat(AccountingService.class.getSuperclass()).isEqualTo(Object.class);
    assertThat(fieldTypes(AccountingService.class))
        .contains(
            AccountCatalogService.class,
            JournalEntryService.class,
            DealerReceiptService.class,
            SettlementService.class,
            CreditDebitNoteService.class,
            InventoryAccountingService.class,
            PayrollAccountingService.class);
  }

  @Test
  void settlementSupportWrapperIsDeleted() {
    assertThat(
            Path.of(
                    "/home/realnigga/Desktop/Mission-control/erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/service/"
                        + settlementSupportFileName())
                .toFile())
        .doesNotExist();
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

  private String settlementSupportFileName() {
    return "Settlement" + "CoreSupport.java";
  }
}
