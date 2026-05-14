package com.bigbrightpaints.erp.truthsuite.o2c;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.bigbrightpaints.erp.truthsuite.support.TruthSuiteFileAssert;

@Tag("critical")
@Tag("reconciliation")
class TS_O2COrchestratorDispatchRetirementTest {

  private static final String INTEGRATION_COORDINATOR =
      "src/main/java/com/bigbrightpaints/erp/orchestrator/service/IntegrationCoordinator.java";
  private static final String COMMAND_DISPATCHER =
      "src/main/java/com/bigbrightpaints/erp/orchestrator/service/CommandDispatcher.java";

  @Test
  void integrationCoordinatorDoesNotContainRetiredDispatchJournalMethodsOrHelpers() {
    String source = TruthSuiteFileAssert.read(INTEGRATION_COORDINATOR);

    assertFalse(
        source.contains("postDispatchJournal("),
        "IntegrationCoordinator must not expose postDispatchJournal");
    assertFalse(
        source.contains("createAccountingEntry("),
        "IntegrationCoordinator must not expose createAccountingEntry");
    assertFalse(
        source.contains("postJournal("), "Retired orchestrator journal helper must be removed");
    assertFalse(
        source.contains("DISPATCH-"),
        "IntegrationCoordinator must not build DISPATCH-prefixed journal references");
    assertFalse(
        source.contains("erp.dispatch.debit-account-id"),
        "Retired dispatch debit mapping must be removed");
    assertFalse(
        source.contains("erp.dispatch.credit-account-id"),
        "Retired dispatch credit mapping must be removed");
  }

  @Test
  void commandDispatcherDoesNotExposeRetiredDispatchShortcut() {
    String source = TruthSuiteFileAssert.read(COMMAND_DISPATCHER);

    assertFalse(
        source.contains("dispatchBatch("),
        "CommandDispatcher must not keep a retired orchestrator dispatch shortcut");
    assertFalse(
        source.contains("DispatchRequest"),
        "CommandDispatcher must not reference the retired dispatch request payload");
    assertFalse(
        source.contains("integrationCoordinator.updateProductionStatus("),
        "Retired dispatch shortcut must not advance production status independently");
    assertFalse(
        source.contains("integrationCoordinator.releaseInventory("),
        "Retired dispatch shortcut must not release inventory independently");
    assertFalse(
        source.contains("integrationCoordinator.postDispatchJournal("),
        "Retired dispatch shortcut must not post orchestrator dispatch journals");
    assertFalse(
        source.contains("startWorkflow(\"dispatch\")"),
        "Retired dispatch shortcut must not start a dispatch workflow");
  }
}
