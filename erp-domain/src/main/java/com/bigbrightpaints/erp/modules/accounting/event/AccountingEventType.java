package com.bigbrightpaints.erp.modules.accounting.event;

/**
 * Types of accounting domain events for event sourcing.
 */
public enum AccountingEventType {
  // Journal Entry Events
  JOURNAL_ENTRY_CREATED,
  JOURNAL_ENTRY_POSTED,
  JOURNAL_ENTRY_REVERSED,
  JOURNAL_ENTRY_VOIDED,

  // Account Events
  ACCOUNT_CREATED,
  ACCOUNT_BALANCE_ADJUSTED,
  ACCOUNT_DEBIT_POSTED,
  ACCOUNT_CREDIT_POSTED,

  // Period Events
  PERIOD_OPENED,
  PERIOD_LOCKED,
  PERIOD_CLOSED,
  PERIOD_REOPENED,

  // Settlement Events
  DEALER_RECEIPT_POSTED,
  SUPPLIER_PAYMENT_POSTED,
  SETTLEMENT_ALLOCATED,

  // Correction Events
  BALANCE_CORRECTION,
  AUDIT_ADJUSTMENT
}
