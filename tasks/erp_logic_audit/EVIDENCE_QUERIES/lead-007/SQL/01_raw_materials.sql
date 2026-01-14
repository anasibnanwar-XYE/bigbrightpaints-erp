-- Params:
--   :company_id (numeric)

SELECT id, sku, name, inventory_account_id
FROM raw_materials
WHERE company_id = :company_id
ORDER BY id;
