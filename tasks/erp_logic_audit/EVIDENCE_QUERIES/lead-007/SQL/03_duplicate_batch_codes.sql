-- Params:
--   :batch_code (text)

SELECT batch_code, COUNT(*) AS duplicate_count,
       MIN(id) AS first_batch_id,
       MAX(id) AS last_batch_id
FROM raw_material_batches
WHERE batch_code = :batch_code
GROUP BY batch_code;
