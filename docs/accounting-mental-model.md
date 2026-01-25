# Accounting Mental Model (O2C / P2P / GST / Returns)

This document explains how accounting flows are intended to work across modules,
and where to validate them in tests. It is **descriptive** (no new features).

## Glossary
- **O2C**: Order‑to‑Cash (sales order → dispatch → invoice → settlement).
- **P2P**: Procure‑to‑Pay (purchase order → goods receipt → purchase invoice → settlement).
- **COGS**: Cost of goods sold (inventory relief on dispatch).
- **GST Output/Input**: Tax liability/asset accounts configured per company.
- **Canonical reference**: stable reference string for idempotency and journal mapping.

## O2C flow (Sales → Dispatch → Accounting)
**Primary entry point**
- `SalesService.confirmDispatch(...)` is the authoritative flow for shipped‑quantity accounting.

**Expected sequence**
1) **Sales Order** created and confirmed.
2) **Packaging Slip** reserved → **Dispatch** confirmed.
3) **InventoryMovement** entries created for DISPATCH.
4) **COGS Journal** posted (`AccountingFacade.postCogsJournal(...)`).
5) **Sales Journal** posted for AR/Revenue/Tax (`AccountingFacade.postSalesJournal(...)`).
6) **Invoice** created and linked to slip + journals.
7) **Dealer Ledger** synced (`DealerLedgerService.syncInvoiceLedger(...)`).

**Idempotency & linkage**
- Dispatch is retry‑safe; repeated confirms must not add inventory movements or journals.
- Slip/invoice/journal references are stable and used for reconciliation.

**Validation tests**
- `OrderFulfillmentE2ETest`
- `DispatchConfirmationIT`
- `ErpInvariantsSuiteIT` (dispatch → invoice → journal linkage)

## P2P flow (Purchasing → Receiving → Accounting)
**Primary entry points**
- `PurchasingService.createPurchaseOrder(...)`
- `PurchasingService.createGoodsReceipt(...)`
- `PurchasingService.createPurchase(...)`

**Expected sequence**
1) **Purchase Order** recorded.
2) **Goods Receipt** recorded (full or partial).
3) **Raw Material Purchase** (supplier invoice) recorded.
4) **Purchase Journal** posted (`AccountingFacade.postPurchaseJournal(...)`).
5) **Supplier Ledger** updated and **Settlement** applied.

**Validation tests**
- `ProcureToPayE2ETest`
- `CriticalAccountingAxesIT` (purchase balance + GST)

## GST + rounding rules
**Core expectations**
- Currency rounding is 2dp, HALF_UP (`MoneyUtils.roundCurrency(...)`).
- Journals must balance exactly at posting time.
- `net + tax == gross` for invoice lines (after rounding).

**GST modes**
- **Inclusive**: unit prices include GST; tax extracted from gross.
- **Exclusive**: tax computed on base.
- **None**: tax totals must be zero.

**Simple examples**
- Inclusive 10%: unit price 110.00 → net 100.00, tax 10.00.
- Exclusive 10%: unit price 100.00 → tax 10.00, gross 110.00.

**Validation tests**
- `GstInclusiveRoundingIT`
- `CriticalAccountingAxesIT`
- `ErpInvariantsSuiteIT`

## Returns
**Sales returns**
- `SalesReturnService.processReturn(...)` posts a credit note (revenue/tax reversal)
  and **restocks** inventory with a COGS reversal.
- Returns require dispatch cost layers; cannot exceed dispatched quantity.

**Validation tests**
- `SalesReturnCreditNoteE2EIT`
- `CriticalAccountingAxesIT`

## Settlements & payments
**Dealer/Supplier settlements**
- `AccountingService.settleDealerInvoices(...)`
- `AccountingService.settleSupplierInvoices(...)`
- Idempotency keys prevent duplicate allocations.

**Validation tests**
- `SettlementE2ETest`
- `ErpInvariantsSuiteIT` (sub‑ledger vs GL reconciliation)

## Period controls
**Policy**
- Posting requires an **OPEN** period (`AccountingPeriodService.requireOpenPeriod(...)`).
- Close/reopen flows produce reversing journals and must be idempotent.

**Validation tests**
- `PeriodCloseLockIT`

## What can go wrong (and how we guard)
- **Duplicate postings** → canonical references + idempotency tests.
- **Rounding drift** → centralized rounding + invariant assertions.
- **Missing tax accounts** → `CompanyAccountingSettingsService.requireTaxAccounts()`.
- **Cross‑company leakage** → company‑scoped queries + security tests.

## Where to verify quickly
- `ErpInvariantsSuiteIT` (core correctness)
- `OrderFulfillmentE2ETest` (O2C)
- `ProcureToPayE2ETest` (P2P)
- `SettlementE2ETest` (payments)
- `GstInclusiveRoundingIT` (GST rounding)
