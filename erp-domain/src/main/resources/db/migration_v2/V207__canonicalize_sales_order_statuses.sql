-- Hard-cut retired sales-order status tokens into the canonical current-state model.
-- Runtime code intentionally no longer accepts these aliases.

UPDATE public.sales_orders
SET status =
    CASE UPPER(TRIM(status))
        WHEN 'BOOKED' THEN 'CONFIRMED'
        WHEN 'PENDING' THEN 'DRAFT'
        WHEN 'APPROVED' THEN 'CONFIRMED'
        WHEN 'SHIPPED' THEN 'DISPATCHED'
        WHEN 'FULFILLED' THEN 'DISPATCHED'
        WHEN 'COMPLETED' THEN 'SETTLED'
        ELSE UPPER(TRIM(status))
    END
WHERE status IS NOT NULL
  AND (
      status <> UPPER(TRIM(status))
      OR UPPER(TRIM(status)) IN (
       'BOOKED',
       'PENDING',
       'APPROVED',
       'SHIPPED',
       'FULFILLED',
       'COMPLETED'
      )
  );

UPDATE public.sales_order_status_history
SET from_status =
    CASE UPPER(TRIM(from_status))
        WHEN 'BOOKED' THEN 'CONFIRMED'
        WHEN 'PENDING' THEN 'DRAFT'
        WHEN 'APPROVED' THEN 'CONFIRMED'
        WHEN 'SHIPPED' THEN 'DISPATCHED'
        WHEN 'FULFILLED' THEN 'DISPATCHED'
        WHEN 'COMPLETED' THEN 'SETTLED'
        ELSE UPPER(TRIM(from_status))
    END
WHERE from_status IS NOT NULL
  AND (
      from_status <> UPPER(TRIM(from_status))
      OR UPPER(TRIM(from_status)) IN (
       'BOOKED',
       'PENDING',
       'APPROVED',
       'SHIPPED',
       'FULFILLED',
       'COMPLETED'
      )
  );

UPDATE public.sales_order_status_history
SET to_status =
    CASE UPPER(TRIM(to_status))
        WHEN 'BOOKED' THEN 'CONFIRMED'
        WHEN 'PENDING' THEN 'DRAFT'
        WHEN 'APPROVED' THEN 'CONFIRMED'
        WHEN 'SHIPPED' THEN 'DISPATCHED'
        WHEN 'FULFILLED' THEN 'DISPATCHED'
        WHEN 'COMPLETED' THEN 'SETTLED'
        ELSE UPPER(TRIM(to_status))
    END
WHERE to_status IS NOT NULL
  AND (
      to_status <> UPPER(TRIM(to_status))
      OR UPPER(TRIM(to_status)) IN (
       'BOOKED',
       'PENDING',
       'APPROVED',
       'SHIPPED',
       'FULFILLED',
       'COMPLETED'
      )
  );
