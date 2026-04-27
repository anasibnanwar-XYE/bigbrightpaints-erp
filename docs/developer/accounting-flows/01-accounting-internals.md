# Accounting Internals

## Folder Map

- `modules/accounting/controller`
  Purpose: HTTP transport for journals, receipts, settlements, period close, reconciliation, statements, audit, setup, payroll, and imports.
- `modules/accounting/service`
  Purpose: public service layer and focused posting services for journals, receipts, settlements, notes, reconciliation, imports, and reporting.
- `modules/accounting/domain`
  Purpose: journal, period, subledger, discrepancy, and import persistence truth.
- `modules/accounting/dto`
  Purpose: request/response models for posting, settlement, period, and setup.
- `modules/accounting/event`
  Purpose: accounting event persistence plus the inventory-to-accounting listener boundary.

## Canonical Service Owners

- `AccountingFacade`
  Purpose: canonical cross-module accounting seam for journal posting.
- `JournalEntryService`
  Purpose: canonical journal creation and reversal service.
- `JournalReplayService`
  Purpose: idempotency-reference mapping, replay lookup, and allocation waits.
- `AccountingPeriodService`
  Purpose: canonical period-close and reopen state machine.
- `ReconciliationService`
  Purpose: canonical reconciliation and discrepancy service.
- `AccountingAuditService`
  Purpose: accounting audit-event publishing and integration-failure metadata.

## Service Shells

- `AccountingService`
  Purpose: public facade used by controllers for core accounting operations.
- Focused posting services
  Purpose: dealer receipts, settlements, correction notes, inventory valuation, payroll, and factory journal posting.

## Major Workflows

### Journal Creation

```text
AccountingController
  -> AccountingService / JournalEntryService / AccountingFacade
  -> JournalEntryService.createStandardJournal(...) / AccountingFacade.createManualJournal(...)
  -> JournalEntry + JournalLine + account balances + event trail
```

Key methods:
- `AccountingFacade.createManualJournal`
- `JournalEntryService.createStandardJournal`
- `JournalEntryService.createManualJournalEntry`

### Reversal

```text
AccountingController.reverse / cascadeReverse
  -> JournalEntryService.reverseJournalEntry(...)
  -> AccountingComplianceAuditService.recordJournalReversal(...)
```

### Settlement and Receipt Execution

```text
AccountingController
  -> DealerReceiptService / SettlementService
  -> JournalReplayService + focused posting services
  -> PartnerSettlementAllocation + subledger sync + invoice/purchase state update
```

### Audit and Event Trail

```text
AccountingAuditController / AdminAuditController / SuperAdminAuditController
  -> AuditAccessService
  -> AuditLogReadAdapter + BusinessAuditReadAdapter

AccountingFacade / JournalEntryService
  -> AccountingEventStore
  -> strict event persistence when enabled
```

## What Works

- one canonical accounting facade plus focused posting services own journal-backed writes
- period close and reconciliation each have one canonical service
- audit trail has a canonical transaction-detail read model
- reports and portals already depend mostly on accounting read models, not on their own duplicated persistence

## Duplicates and Bad Paths

- `AccountingService` is still a broad controller-facing shell over specialized services
- `BankReconciliationSessionService.reconcileLegacy(...)` is a remaining legacy-named method that should be reviewed before future reconciliation work
- retired digest and legacy audit-trail endpoints are hard-cut; canonical reads now live on `AccountingAuditController`, `AdminAuditController`, and `SuperAdminAuditController`
- `JournalReferenceResolver` still walks direct -> legacy -> canonical mappings

## Review Hotspots

- `AccountingFacade`
- `JournalEntryService`
- `JournalReplayService`
- `AccountingPeriodService`
- `ReconciliationService`
- `AccountingAuditService`
- `AccountingService` override block
- `InventoryAccountingEventListener`
