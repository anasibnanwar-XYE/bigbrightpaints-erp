-- Widen foreign amount total precision to align with other money columns.
ALTER TABLE journal_entries
    ALTER COLUMN foreign_amount_total TYPE NUMERIC(19,2);
