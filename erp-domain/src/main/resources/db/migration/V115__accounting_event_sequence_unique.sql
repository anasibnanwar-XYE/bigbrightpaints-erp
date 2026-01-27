-- Ensure aggregate event sequence numbers are unique for replay ordering
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM accounting_events
        GROUP BY aggregate_id, sequence_number
        HAVING COUNT(*) > 1
    ) THEN
        RAISE EXCEPTION 'Duplicate accounting_events (aggregate_id, sequence_number) rows detected; clean up before applying unique constraint.';
    END IF;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS uk_accounting_events_aggregate_sequence
    ON accounting_events(aggregate_id, sequence_number);
