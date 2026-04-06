package com.bigbrightpaints.erp.truthsuite.accounting;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.bigbrightpaints.erp.truthsuite.support.TruthSuiteFileAssert;

@Tag("critical")
@Tag("reconciliation")
class TS_AccountingPeriodCloseChecklistSafetyContractTest {

  private static final String CHECKLIST_DIAGNOSTICS =
      "src/main/java/com/bigbrightpaints/erp/modules/accounting/service/AccountingPeriodChecklistDiagnostics.java";
  private static final String CHECKLIST_SERVICE =
      "src/main/java/com/bigbrightpaints/erp/modules/accounting/service/AccountingPeriodChecklistService.java";
  private static final String STATUS_WORKFLOW =
      "src/main/java/com/bigbrightpaints/erp/modules/accounting/service/AccountingPeriodStatusWorkflow.java";

  @Test
  void periodCloseAlwaysChecksReceiptsBeforeForceBypass() {
    TruthSuiteFileAssert.assertContainsInOrder(
        STATUS_WORKFLOW,
        "boolean force = request != null && Boolean.TRUE.equals(request.force());",
        "assertNoUninvoicedReceipts(company, period);",
        "if (!force) {",
        "checklistService.assertChecklistComplete(company, period);");
  }

  @Test
  void unresolvedChecklistControlsRemainDeterministicAndGuided() {
    TruthSuiteFileAssert.assertContains(
        CHECKLIST_DIAGNOSTICS,
        "private static final List<String> RECONCILIATION_CONTROL_ORDER = List.of(",
        "\"inventoryReconciled\",",
        "\"arReconciled\",",
        "\"apReconciled\",",
        "\"gstReconciled\",",
        "\"reconciliationDiscrepanciesResolved\");",
        "return List.copyOf(unresolved);");
    TruthSuiteFileAssert.assertContains(
        CHECKLIST_SERVICE,
        "private static final Map<String, String> UNRESOLVED_CONTROL_GUIDANCE = Map.of(",
        "UNRESOLVED_CONTROLS_PREFIX + formatUnresolvedControls(unresolvedControls)");
  }

  @Test
  void checklistCloseRejectsStructuralDriftSignals() {
    TruthSuiteFileAssert.assertContains(
        CHECKLIST_SERVICE,
        "if (diagnostics.unbalancedJournals() > 0) {",
        "if (diagnostics.unlinkedDocuments() > 0) {",
        "if (diagnostics.unpostedDocuments() > 0) {");
    TruthSuiteFileAssert.assertContains(
        STATUS_WORKFLOW, "Un-invoiced goods receipts exist in this period (");
  }
}
