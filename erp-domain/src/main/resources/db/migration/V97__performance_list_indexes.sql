CREATE INDEX IF NOT EXISTS idx_sales_orders_company_created_at
    ON sales_orders (company_id, created_at DESC, id DESC);

CREATE INDEX IF NOT EXISTS idx_sales_orders_company_status_created_at
    ON sales_orders (company_id, status, created_at DESC, id DESC);

CREATE INDEX IF NOT EXISTS idx_invoices_company_issue_date
    ON invoices (company_id, issue_date DESC, id DESC);

CREATE INDEX IF NOT EXISTS idx_invoices_company_dealer_issue_date
    ON invoices (company_id, dealer_id, issue_date DESC, id DESC);

CREATE INDEX IF NOT EXISTS idx_inventory_movements_ref_created_at
    ON inventory_movements (reference_type, reference_id, created_at);

CREATE INDEX IF NOT EXISTS idx_orchestrator_outbox_pending_created
    ON orchestrator_outbox (status, dead_letter, next_attempt_at, created_at);
