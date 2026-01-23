ALTER TABLE sales_orders
    ADD COLUMN IF NOT EXISTS gst_inclusive BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE sales_orders so
SET gst_inclusive = TRUE
WHERE so.gst_total > 0
  AND so.total_amount IS NOT NULL
  AND EXISTS (
      SELECT 1
      FROM sales_order_items soi
      WHERE soi.sales_order_id = so.id
  )
  AND ABS(so.total_amount - (
      SELECT COALESCE(SUM(soi.quantity * soi.unit_price), 0)
      FROM sales_order_items soi
      WHERE soi.sales_order_id = so.id
  )) <= 0.01;
