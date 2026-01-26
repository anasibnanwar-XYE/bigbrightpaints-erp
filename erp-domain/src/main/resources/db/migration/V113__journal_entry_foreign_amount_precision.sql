-- Ensure foreign_amount_total uses fixed precision/scale for BigDecimal mapping
ALTER TABLE journal_entries
    ALTER COLUMN foreign_amount_total TYPE NUMERIC(18,2)
    USING foreign_amount_total::numeric;
