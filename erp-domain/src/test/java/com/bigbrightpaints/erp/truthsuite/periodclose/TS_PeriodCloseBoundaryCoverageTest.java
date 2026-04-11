package com.bigbrightpaints.erp.truthsuite.periodclose;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.bigbrightpaints.erp.truthsuite.support.TruthSuiteFileAssert;

@Tag("critical")
@Tag("reconciliation")
class TS_PeriodCloseBoundaryCoverageTest {

  private static final String CHECKLIST_SERVICE =
      "src/main/java/com/bigbrightpaints/erp/modules/accounting/service/AccountingPeriodChecklistService.java";
  private static final String LIFECYCLE_SERVICE =
      "src/main/java/com/bigbrightpaints/erp/modules/accounting/service/AccountingPeriodLifecycleService.java";
  private static final String STATUS_WORKFLOW =
      "src/main/java/com/bigbrightpaints/erp/modules/accounting/service/AccountingPeriodStatusWorkflow.java";

  @Test
  void closePeriodChecksUninvoicedReceiptsBeforeChecklistForceBypass() {
    TruthSuiteFileAssert.assertContainsInOrder(
        STATUS_WORKFLOW,
        "boolean force = request != null && Boolean.TRUE.equals(request.force());",
        "assertNoUninvoicedReceipts(company, period);",
        "if (!force) {",
        "checklistService.assertChecklistComplete(company, period);");
  }

  @Test
  void checklistValidationRemainsFailClosedForUnresolvedAndStructuralDriftSignals() {
    TruthSuiteFileAssert.assertContains(
        CHECKLIST_SERVICE,
        "UNRESOLVED_CONTROLS_PREFIX + formatUnresolvedControls(unresolvedControls)",
        "if (diagnostics.unbalancedJournals() > 0) {",
        "if (diagnostics.unlinkedDocuments() > 0) {",
        "if (diagnostics.unpostedDocuments() > 0) {");
    TruthSuiteFileAssert.assertContains(
        STATUS_WORKFLOW,
        "assertNoUninvoicedReceipts(company, period);",
        "\"Un-invoiced goods receipts exist in this period (\" + uninvoicedReceipts + \")");
  }

  @Test
  void checklistMutationGuardRejectsLockedAndClosedPeriodWrites() {
    TruthSuiteFileAssert.assertContains(
        CHECKLIST_SERVICE,
        "private void assertChecklistMutable(AccountingPeriod period) {",
        "if (period.getStatus() == AccountingPeriodStatus.CLOSED",
        "|| period.getStatus() == AccountingPeriodStatus.LOCKED)",
        "Checklist cannot be updated for a locked or closed period");
  }

  @Test
  void requireOpenPeriodRejectsLockedOrClosedStates() {
    TruthSuiteFileAssert.assertContains(
        LIFECYCLE_SERVICE,
        "AccountingPeriod requireOpenPeriod(Company company, LocalDate referenceDate) {",
        "if (period.getStatus() != AccountingPeriodStatus.OPEN) {",
        "\"Accounting period \" + period.getLabel() + \" is locked/closed\"");
  }

  @Test
  void reopenBoundaryReversesClosingEntryAndDropsSnapshot() {
    TruthSuiteFileAssert.assertContains(
        STATUS_WORKFLOW,
        ".ifPresent(closing -> reverseClosingJournalIfNeeded(closing, period, reason));",
        "snapshotService.deleteSnapshotForPeriod(company, period);",
        ".reverseClosingEntryForPeriodReopen(closing, period, reason);");
  }
}
