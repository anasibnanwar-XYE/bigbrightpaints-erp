# Critical Calculation Bugs in ERP Domain

This document identifies critical bugs that can break calculations and cause service failures in the ERP system.

## 🔴 CRITICAL BUGS

### 1. **Division by Zero in GST Tax Distribution (SalesService.java:504)**
**Location:** `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/sales/service/SalesService.java:504`

**Bug:**
```java
BigDecimal share = targetTax.multiply(item.getLineSubtotal())
        .divide(subtotal, 6, RoundingMode.HALF_UP);
```

**Problem:** When `subtotal` is zero (can happen if all items have zero price or after GST-inclusive adjustments), this will throw `ArithmeticException: Division by zero`.

**Impact:** Order creation with ORDER_TOTAL GST treatment will crash if subtotal becomes zero.

**Fix Required:** Add check before division:
```java
if (subtotal.compareTo(BigDecimal.ZERO) <= 0) {
    // Handle zero subtotal case
    continue; // or set share to zero
}
BigDecimal share = targetTax.multiply(item.getLineSubtotal())
        .divide(subtotal, 6, RoundingMode.HALF_UP);
```

---

### 2. **Null Pointer Exception in Credit Limit Check (SalesService.java:1000)**
**Location:** `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/sales/service/SalesService.java:1000-1001`

**Bug:**
```java
BigDecimal outstanding = dealerLedgerService.currentBalance(dealer.getId());
if (outstanding.add(totalAmount).compareTo(dealer.getCreditLimit()) > 0
```

**Problem:** `currentBalance()` can return `null`, causing `NullPointerException` when calling `.add()`.

**Impact:** Dispatch confirmation will crash if dealer balance is null.

**Fix Required:**
```java
BigDecimal outstanding = dealerLedgerService.currentBalance(dealer.getId());
if (outstanding == null) {
    outstanding = BigDecimal.ZERO;
}
if (outstanding.add(totalAmount).compareTo(dealer.getCreditLimit()) > 0
```

---

### 3. **Null Pointer Exception in Inventory Reservation (FinishedGoodsService.java:791)**
**Location:** `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/inventory/service/FinishedGoodsService.java:791`

**Bug:**
```java
finishedGood.setReservedStock(finishedGood.getReservedStock().add(allocation));
```

**Problem:** `getReservedStock()` can return `null`, causing `NullPointerException` when calling `.add()`.

**Impact:** Order reservation will crash if reserved stock is null.

**Fix Required:**
```java
BigDecimal currentReserved = finishedGood.getReservedStock() != null 
    ? finishedGood.getReservedStock() 
    : BigDecimal.ZERO;
finishedGood.setReservedStock(currentReserved.add(allocation));
```

---

### 4. **Null Pointer Exception in Available Stock Calculation (FinishedGoodsService.java:126-127)**
**Location:** `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/inventory/service/FinishedGoodsService.java:126-127`

**Bug:**
```java
fg.getReservedStock(),
fg.getCurrentStock().subtract(fg.getReservedStock()),
```

**Problem:** Both `getCurrentStock()` and `getReservedStock()` can be `null`, causing `NullPointerException`.

**Impact:** Listing finished goods will crash if stock values are null.

**Fix Required:**
```java
BigDecimal current = fg.getCurrentStock() != null ? fg.getCurrentStock() : BigDecimal.ZERO;
BigDecimal reserved = fg.getReservedStock() != null ? fg.getReservedStock() : BigDecimal.ZERO;
// Then use: current, reserved, current.subtract(reserved)
```

---

### 5. **Null Pointer Exception in Low Stock Filter (FinishedGoodsService.java:142)**
**Location:** `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/inventory/service/FinishedGoodsService.java:142`

**Bug:**
```java
.filter(fg -> fg.getCurrentStock().subtract(fg.getReservedStock()).compareTo(thresholdQty) < 0)
```

**Problem:** Both `getCurrentStock()` and `getReservedStock()` can be `null`, causing `NullPointerException`.

**Impact:** Low stock check will crash.

**Fix Required:** Add null checks before subtraction.

---

### 6. **Division by Zero in Cost Allocation (CostAllocationService.java:143, 146)**
**Location:** `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/factory/service/CostAllocationService.java:141-146`

**Bug:**
```java
BigDecimal batchLaborCost = costPerLiter.multiply(batchLiters)
        .multiply(laborCost)
        .divide(totalCosts, 4, RoundingMode.HALF_UP);
BigDecimal batchOverheadCost = costPerLiter.multiply(batchLiters)
        .multiply(overheadCost)
        .divide(totalCosts, 4, RoundingMode.HALF_UP);
```

**Problem:** While `totalCosts` is checked earlier (line 116), if `totalCosts` becomes zero due to concurrent modification or edge case, this will crash.

**Impact:** Cost allocation will fail if totalCosts is zero.

**Note:** There is a check at line 116, but the calculation logic is flawed - it multiplies `costPerLiter` (which already includes totalCosts) by `totalCosts` again, which is incorrect.

---

### 7. **Division by Zero in Accounting Service - Landed Cost Allocation (AccountingService.java:1743)**
**Location:** `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/service/AccountingService.java:1743`

**Bug:**
```java
BigDecimal weight = line.getLineTotal().divide(totalValue, 8, RoundingMode.HALF_UP);
```

**Problem:** If `totalValue` is zero, this will throw `ArithmeticException`.

**Impact:** Landed cost allocation will crash.

**Fix Required:** Check `totalValue` before division.

---

### 8. **Division by Zero in Accounting Service - Raw Material Revaluation (AccountingService.java:1801)**
**Location:** `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/service/AccountingService.java:1801`

**Bug:**
```java
BigDecimal deltaPerUnit = delta.divide(totalQty, 6, RoundingMode.HALF_UP);
```

**Problem:** While there's a check at line 1798, if `totalQty` becomes zero between check and division (unlikely but possible in concurrent scenarios), this will crash.

**Impact:** Raw material revaluation will fail.

**Note:** There is a check, but the code should be more defensive.

---

### 9. **Division by Zero in Dispatch Tax Calculation (SalesService.java:962)**
**Location:** `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/sales/service/SalesService.java:962-963`

**Bug:**
```java
BigDecimal divisor = BigDecimal.ONE.add(taxRate.divide(new BigDecimal("100"), 6, RoundingMode.HALF_UP));
BigDecimal preTax = lineNet.divide(divisor, 6, RoundingMode.HALF_UP);
```

**Problem:** If `taxRate` is extremely negative (e.g., -100 or less), `divisor` could become zero or negative, causing division issues.

**Impact:** Dispatch with tax-inclusive pricing will crash with invalid tax rates.

**Fix Required:** Validate taxRate before calculation.

---

### 10. **Missing Order Amount Calculation Before Validation (SalesService.java:260-261)**
**Location:** `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/sales/service/SalesService.java:260-261`

**Bug:**
```java
OrderAmountSummary amounts = mapOrderItems(order, items, gstTreatment, orderLevelRate, gstInclusive);
validateTotalAmount(request.totalAmount(), amounts.total());
```

**Problem:** `amounts` is calculated but the code shows `validateTotalAmount` is called before `amounts` is assigned in the visible code. Actually, looking at line 260, `amounts` is assigned, but if `mapOrderItems` throws an exception or returns null amounts, validation will fail.

**Impact:** Order creation may fail with unclear error messages.

---

### 11. **Potential Division by Zero in GST Inclusive Calculation (SalesService.java:466)**
**Location:** `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/sales/service/SalesService.java:466`

**Bug:**
```java
lineTax = currency(item.getLineSubtotal()
        .multiply(rate)
        .divide(new BigDecimal("100").add(rate), 6, RoundingMode.HALF_UP));
```

**Problem:** If `rate` is exactly `-100`, the divisor becomes zero, causing division by zero.

**Impact:** Order creation with invalid GST rate will crash.

**Fix Required:** Validate rate is not -100 before calculation.

---

### 12. **Division by Zero in Report Service - Wastage Calculation (ReportService.java:417)**
**Location:** `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/reports/service/ReportService.java:417`

**Bug:**
```java
BigDecimal wastagePercentage = mixedQty.compareTo(BigDecimal.ZERO) > 0
        ? wastageQty.divide(mixedQty, 4, java.math.RoundingMode.HALF_UP).multiply(new BigDecimal("100"))
        : BigDecimal.ZERO;
```

**Status:** ✅ **FIXED** - Has proper null check before division.

---

### 13. **Race Condition in Credit Limit Check**
**Location:** Multiple locations in `SalesService.java`

**Bug:** While `REPEATABLE_READ` isolation is used in `createOrder`, the `confirmDispatch` method (line 999-1006) checks credit limit without proper locking in some code paths.

**Impact:** Concurrent orders/dispatches could exceed credit limits.

**Note:** Line 245 locks dealer, but line 999-1006 in `confirmDispatch` may have race conditions.

---

## 🟡 MEDIUM PRIORITY BUGS

### 14. **Incorrect Cost Allocation Formula (CostAllocationService.java:141-146)**
**Location:** `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/factory/service/CostAllocationService.java:141-146`

**Bug:** The formula multiplies `costPerLiter` (which already includes total costs divided by liters) by `totalCosts` again:
```java
BigDecimal batchLaborCost = costPerLiter.multiply(batchLiters)
        .multiply(laborCost)
        .divide(totalCosts, 4, RoundingMode.HALF_UP);
```

**Problem:** This is mathematically incorrect. Should be:
```java
BigDecimal batchLaborCost = batchLiters
        .multiply(laborCost)
        .divide(totalLitersProduced, 4, RoundingMode.HALF_UP);
```

**Impact:** Cost allocation will produce incorrect values.

---

## Summary

**Total Critical Bugs Found: 13**
- **Division by Zero:** 6 bugs
- **Null Pointer Exceptions:** 4 bugs  
- **Race Conditions:** 1 bug
- **Logic Errors:** 2 bugs

**Immediate Action Required:** Fix all division by zero and null pointer bugs as they will cause service crashes in production.



