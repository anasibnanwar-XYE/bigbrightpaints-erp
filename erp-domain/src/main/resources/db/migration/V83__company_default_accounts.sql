ALTER TABLE companies
    ADD COLUMN default_inventory_account_id BIGINT NULL,
    ADD COLUMN default_cogs_account_id BIGINT NULL,
    ADD COLUMN default_revenue_account_id BIGINT NULL,
    ADD COLUMN default_discount_account_id BIGINT NULL,
    ADD COLUMN default_tax_account_id BIGINT NULL;
