package com.bigbrightpaints.erp.truthsuite.periodclose;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.bigbrightpaints.erp.truthsuite.support.TruthSuiteFileAssert;

@Tag("critical")
@Tag("concurrency")
@Tag("reconciliation")
class TS_PeriodCloseAtomicSnapshotTest {

  private static final String PERIOD_SERVICE =
      "src/main/java/com/bigbrightpaints/erp/modules/accounting/service/AccountingPeriodService.java";

  @Test
  void closePeriodLocksThenCapturesSnapshotBeforeClosedState() {
    TruthSuiteFileAssert.assertContainsInOrder(
        PERIOD_SERVICE,
        "AccountingPeriod period = accountingPeriodRepository.lockByCompanyAndId(company,"
            + " periodId)",
        "periodCloseHook.onPeriodCloseLocked(company, period);",
        "snapshotService.captureSnapshot(company, period, user);",
        "period.setStatus(AccountingPeriodStatus.CLOSED);");
  }

  @Test
  void repeatCloseOnClosedPeriodReturnsWithoutSnapshotRecapture() {
    String content = TruthSuiteFileAssert.read(PERIOD_SERVICE);
    String guard = "if (period.getStatus() == AccountingPeriodStatus.CLOSED) {";
    int start = content.indexOf(guard);
    assertTrue(start >= 0, "Closed-period idempotency guard must exist in closePeriod");
    int end = content.indexOf('}', start);
    assertTrue(end > start, "Closed-period guard block must be syntactically complete");
    String closedBranch = content.substring(start, end + 1);
    assertTrue(
        closedBranch.contains("return toDto(period);"),
        "Closed-period guard must short-circuit to DTO return");
    assertFalse(
        closedBranch.contains("snapshotService.captureSnapshot"),
        "Closed-period guard must not recapture snapshot on repeated close");
  }

  @Test
  void closedPeriodRejectsOperationalPostingViaOpenPeriodGuard() {
    TruthSuiteFileAssert.assertContains(
        PERIOD_SERVICE,
        "public AccountingPeriod requireOpenPeriod(Company company, LocalDate referenceDate)",
        "if (period.getStatus() != AccountingPeriodStatus.OPEN) {",
        "\"Accounting period \" + period.getLabel() + \" is locked/closed\"");
  }

  @Test
  void reopenUsesCanonicalReverseBoundaryAndDropsSnapshot() {
    TruthSuiteFileAssert.assertContains(
        PERIOD_SERVICE,
        "String reason = request.reason().trim();",
        ".ifPresent(closing -> reverseClosingJournalIfNeeded(closing, period, reason));",
        "snapshotService.deleteSnapshotForPeriod(company, period);",
        "accountingFacade.reverseClosingEntryForPeriodReopen(closing, period, reason);");
  }
}
