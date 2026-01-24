# Rounding & Tax Helper Inventory (Task 00 — EPIC C1)

## Helper Catalog (by file)

### `SalesService`
- `currency(BigDecimal)` → `setScale(2, HALF_UP)`  
  `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/sales/service/SalesService.java`
- `normalizePercent(BigDecimal)` → clamps ≥0, scales to 2 then 4 decimals  
  `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/sales/service/SalesService.java`
- `computeDispatchLineAmounts(...)` → inclusive/exclusive tax calc, rounds net/tax to 2 decimals  
  `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/sales/service/SalesService.java`

### `SalesJournalService`
- `currency(BigDecimal)` → `setScale(2, HALF_UP)`  
  `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/sales/service/SalesJournalService.java`
- `normalizeDiscountNet(...)` → tax‑inclusive discount normalization using `divide(..., 6, HALF_UP)`  
  `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/sales/service/SalesJournalService.java`

### `InvoiceService`
- `currency(BigDecimal)` → `setScale(2, HALF_UP)`  
  `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/invoice/service/InvoiceService.java`

### `TaxService`
- `roundCurrency(BigDecimal)` → `setScale(2, HALF_UP)`  
  `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/service/TaxService.java`

## Divergences / Observations
- Multiple `currency` helpers across modules duplicate the same 2‑decimal rounding behavior.
- Tax rate normalization and inclusive/exclusive computations live in `SalesService` and `SalesJournalService` separately.
- Discount normalization for GST‑inclusive pricing is handled in `SalesJournalService.normalizeDiscountNet(...)` while dispatch uses `computeDispatchLineAmounts(...)`.

## Minimal‑Diff Consolidation Plan (proposal)
1) Introduce a shared rounding helper (e.g., `MoneyUtils.roundCurrency` or a small `TaxMath` utility) with:
   - `roundCurrency(BigDecimal)` (2‑decimal HALF_UP)
   - `normalizePercent(BigDecimal)` (2‑ or 4‑decimal, consistent across modules)
   - `computeLineAmounts(...)` for inclusive/exclusive tax
2) Replace module‑local `currency(...)` methods first (no behavior change).
3) Move GST‑inclusive discount normalization into the shared helper and reuse in:
   - `SalesService.computeDispatchLineAmounts(...)`
   - `SalesJournalService.normalizeDiscountNet(...)`
4) Guard changes with existing GST rounding tests (`GstInclusiveRoundingIT`) and add property‑style assertions in EPIC C2 before refactors.
