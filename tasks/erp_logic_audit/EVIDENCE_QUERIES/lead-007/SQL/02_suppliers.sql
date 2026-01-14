-- Params:
--   :company_id (numeric)

SELECT id, code, name, payable_account_id
FROM suppliers
WHERE company_id = :company_id
ORDER BY id;
