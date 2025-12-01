-- Add parent-child hierarchy to accounts for consolidated reports
-- Enables tree structure like:
-- Assets
-- ├── Current Assets
-- │   ├── Cash
-- │   └── Accounts Receivable
-- └── Fixed Assets
--     ├── Equipment
--     └── Vehicles

ALTER TABLE accounts ADD COLUMN parent_id BIGINT REFERENCES accounts(id);
ALTER TABLE accounts ADD COLUMN hierarchy_level INTEGER DEFAULT 1;

CREATE INDEX idx_accounts_parent ON accounts(parent_id);
CREATE INDEX idx_accounts_hierarchy ON accounts(company_id, hierarchy_level, code);

COMMENT ON COLUMN accounts.parent_id IS 'Parent account for hierarchy (NULL = root account)';
COMMENT ON COLUMN accounts.hierarchy_level IS 'Level in hierarchy: 1=Category, 2=Subcategory, 3=Detail';
