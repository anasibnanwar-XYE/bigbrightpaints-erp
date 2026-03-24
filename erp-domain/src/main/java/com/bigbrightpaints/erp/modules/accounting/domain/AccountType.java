package com.bigbrightpaints.erp.modules.accounting.domain;

/**
 * Standard account types supported by the system.
 * Keeps downstream logic from relying on free-form strings.
 */
public enum AccountType {
  ASSET,
  LIABILITY,
  EQUITY,
  REVENUE,
  EXPENSE,
  COGS,
  OTHER_INCOME, // Non-operating income (interest, gains)
  OTHER_EXPENSE; // Non-operating expenses (interest expense, losses)

  public boolean isDebitNormalBalance() {
    return switch (this) {
      case ASSET, EXPENSE, COGS, OTHER_EXPENSE -> true;
      default -> false;
    };
  }

  public boolean affectsNetIncome() {
    return switch (this) {
      case REVENUE, EXPENSE, COGS, OTHER_INCOME, OTHER_EXPENSE -> true;
      default -> false;
    };
  }

  public boolean isCreditNormalBalance() {
    return !isDebitNormalBalance();
  }
}
