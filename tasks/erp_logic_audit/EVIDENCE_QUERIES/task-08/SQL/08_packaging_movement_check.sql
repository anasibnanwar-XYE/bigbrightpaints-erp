--   :company_id (numeric)
--   :since_ts (string, timestamptz)
--
-- Purpose:
--   Check packaging movements created after a probe start time.

SELECT
  im.reference_id,
  im.movement_type,
  COUNT(*) AS movement_count,
  SUM(im.quantity) AS total_quantity,
  MIN(im.created_at) AS first_seen,
  MAX(im.created_at) AS last_seen
FROM inventory_movements im
JOIN finished_goods fg ON fg.id = im.finished_good_id
WHERE fg.company_id = :company_id
  AND im.reference_type = 'PACKAGING'
  AND im.created_at >= :'since_ts'::timestamptz
GROUP BY im.reference_id, im.movement_type
ORDER BY movement_count DESC, last_seen DESC;
