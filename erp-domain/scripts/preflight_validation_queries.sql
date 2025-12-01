-- Pre-deployment validation queries (run these MANUALLY before deployment)
-- These are SELECT-only queries to identify data issues
-- DO NOT run as a Flyway migration!

-- =============================================================================
-- 1. LEGACY SLIPS/RESERVATIONS VS STOCK
-- =============================================================================

-- Find packaging slips not fully dispatched with unreconciled reservations
SELECT 
    ps.id as slip_id,
    ps.slip_number,
    ps.status as slip_status,
    so.id as order_id,
    so.order_number,
    ir.id as reservation_id,
    ir.status as reservation_status,
    ir.reserved_quantity,
    ir.fulfilled_quantity,
    fgb.id as batch_id,
    fgb.quantity_available as batch_available
FROM packaging_slips ps
JOIN sales_orders so ON ps.sales_order_id = so.id
LEFT JOIN inventory_reservations ir ON ir.reference_type = 'SALES_ORDER' 
    AND ir.reference_id = so.id::text
LEFT JOIN finished_good_batches fgb ON ir.finished_good_batch_id = fgb.id
WHERE ps.status != 'DISPATCHED'
  AND ir.status = 'RESERVED'
  AND (ir.reserved_quantity > COALESCE(fgb.quantity_available, 0));

-- Orphan reservations (RESERVED status but no packaging slip)
SELECT 
    ir.id as reservation_id,
    ir.reference_type,
    ir.reference_id,
    ir.reserved_quantity,
    ir.status,
    ir.created_at
FROM inventory_reservations ir
WHERE ir.status = 'RESERVED'
  AND ir.reference_type = 'SALES_ORDER'
  AND NOT EXISTS (
      SELECT 1 FROM packaging_slips ps 
      WHERE ps.sales_order_id = ir.reference_id::bigint
  );

-- =============================================================================
-- 2. ACCOUNT MAPPINGS COMPLETENESS
-- =============================================================================

-- Finished goods missing required account mappings
SELECT 
    fg.id,
    fg.sku_code,
    fg.name,
    CASE WHEN fg.valuation_account_id IS NULL THEN 'MISSING' ELSE 'OK' END as valuation_account,
    CASE WHEN fg.cogs_account_id IS NULL THEN 'MISSING' ELSE 'OK' END as cogs_account,
    CASE WHEN fg.revenue_account_id IS NULL THEN 'MISSING' ELSE 'OK' END as revenue_account,
    CASE WHEN fg.tax_account_id IS NULL THEN 'MISSING' ELSE 'OK' END as tax_account
FROM finished_goods fg
WHERE fg.valuation_account_id IS NULL
   OR fg.cogs_account_id IS NULL
   OR fg.revenue_account_id IS NULL
   OR fg.tax_account_id IS NULL;

-- Suppliers missing payable account
SELECT 
    s.id,
    s.code,
    s.name,
    'MISSING payable_account_id' as issue
FROM suppliers s
WHERE s.payable_account_id IS NULL;

-- Dealers missing receivable account
SELECT 
    d.id,
    d.code,
    d.name,
    'MISSING receivable_account_id' as issue
FROM dealers d
WHERE d.receivable_account_id IS NULL;

-- =============================================================================
-- 3. REFERENCE UNIQUENESS / COLLISION CHECK
-- =============================================================================

-- Check for existing SALE-{number} pattern journals (potential collision)
SELECT 
    je.id,
    je.reference_number,
    je.entry_date,
    je.memo,
    'SALE-* pattern - may collide with new flow' as warning
FROM journal_entries je
WHERE je.reference_number ~ '^SALE-[0-9]+$';

-- Check for existing *-COGS-* pattern journals
SELECT 
    je.id,
    je.reference_number,
    je.entry_date,
    je.memo,
    '*-COGS-* pattern - may collide with new flow' as warning
FROM journal_entries je
WHERE je.reference_number LIKE '%-COGS-%';

-- =============================================================================
-- 4. IDEMPOTENCY MARKERS BACKFILL VERIFICATION
-- =============================================================================

-- Orders with invoices but missing fulfillment_invoice_id marker
SELECT 
    so.id as order_id,
    so.order_number,
    so.status,
    inv.id as invoice_id,
    inv.invoice_number,
    so.fulfillment_invoice_id as marker_value,
    CASE WHEN so.fulfillment_invoice_id IS NULL THEN 'MISSING' ELSE 'OK' END as status
FROM sales_orders so
JOIN invoices inv ON inv.sales_order_id = so.id
WHERE so.fulfillment_invoice_id IS NULL;

-- Orders with shipped status but missing journal markers
SELECT 
    so.id as order_id,
    so.order_number,
    so.status,
    so.sales_journal_entry_id,
    so.cogs_journal_entry_id,
    'SHIPPED but missing journal markers' as warning
FROM sales_orders so
WHERE so.status = 'SHIPPED'
  AND (so.sales_journal_entry_id IS NULL OR so.cogs_journal_entry_id IS NULL);

-- =============================================================================
-- 5. AR VS DEALER LEDGER RECONCILIATION
-- =============================================================================

-- Compare GL AR account balance vs dealer ledger total
WITH ar_accounts AS (
    SELECT SUM(balance) as gl_ar_total
    FROM accounts
    WHERE type = 'ASSET'
      AND (UPPER(code) LIKE '%AR%' OR UPPER(code) LIKE '%RECEIVABLE%')
),
dealer_ledger_total AS (
    SELECT SUM(debit - credit) as ledger_total
    FROM dealer_ledger_entries
)
SELECT 
    ar.gl_ar_total,
    dl.ledger_total,
    ar.gl_ar_total - COALESCE(dl.ledger_total, 0) as variance,
    CASE 
        WHEN ABS(ar.gl_ar_total - COALESCE(dl.ledger_total, 0)) < 0.01 THEN 'RECONCILED'
        ELSE 'DISCREPANCY'
    END as status
FROM ar_accounts ar
CROSS JOIN dealer_ledger_total dl;

-- Per-dealer balance check
SELECT 
    d.id as dealer_id,
    d.code,
    d.name,
    d.outstanding_balance as dealer_record_balance,
    COALESCE(SUM(dle.debit - dle.credit), 0) as ledger_balance,
    d.outstanding_balance - COALESCE(SUM(dle.debit - dle.credit), 0) as variance
FROM dealers d
LEFT JOIN dealer_ledger_entries dle ON dle.dealer_id = d.id
GROUP BY d.id, d.code, d.name, d.outstanding_balance
HAVING ABS(d.outstanding_balance - COALESCE(SUM(dle.debit - dle.credit), 0)) > 0.01;

-- =============================================================================
-- 6. DATA INTEGRITY CHECKS
-- =============================================================================

-- Journal entries with unbalanced lines (should be zero)
SELECT 
    je.id,
    je.reference_number,
    je.entry_date,
    SUM(jl.debit) as total_debits,
    SUM(jl.credit) as total_credits,
    SUM(jl.debit) - SUM(jl.credit) as imbalance
FROM journal_entries je
JOIN journal_lines jl ON jl.journal_entry_id = je.id
GROUP BY je.id, je.reference_number, je.entry_date
HAVING ABS(SUM(jl.debit) - SUM(jl.credit)) > 0.01;

-- Invoices without corresponding journal entries
SELECT 
    inv.id,
    inv.invoice_number,
    inv.total_amount,
    inv.status,
    'No journal entry linked' as issue
FROM invoices inv
WHERE inv.journal_entry_id IS NULL
  AND inv.status = 'ISSUED';

-- Summary counts
SELECT 'SUMMARY' as section, NULL as count
UNION ALL
SELECT 'Total orders', COUNT(*)::text FROM sales_orders
UNION ALL
SELECT 'Orders with invoice marker', COUNT(*)::text FROM sales_orders WHERE fulfillment_invoice_id IS NOT NULL
UNION ALL
SELECT 'Orders with sales journal marker', COUNT(*)::text FROM sales_orders WHERE sales_journal_entry_id IS NOT NULL
UNION ALL
SELECT 'Orders with COGS journal marker', COUNT(*)::text FROM sales_orders WHERE cogs_journal_entry_id IS NOT NULL
UNION ALL
SELECT 'Active reservations (RESERVED)', COUNT(*)::text FROM inventory_reservations WHERE status = 'RESERVED'
UNION ALL
SELECT 'Pending packaging slips', COUNT(*)::text FROM packaging_slips WHERE status != 'DISPATCHED';
