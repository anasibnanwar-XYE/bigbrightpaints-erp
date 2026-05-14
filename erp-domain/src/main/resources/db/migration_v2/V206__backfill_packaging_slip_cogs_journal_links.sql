-- Flyway v2: preserve replay safety for already-dispatched packaging slips
-- before relying only on the canonical packaging-slip COGS journal marker.

UPDATE public.packaging_slips ps
SET cogs_journal_entry_id = je.id
FROM public.journal_entries je
WHERE ps.cogs_journal_entry_id IS NULL
  AND ps.company_id = je.company_id
  AND ps.slip_number IS NOT NULL
  AND je.reference_number = 'COGS-' || ps.slip_number
  AND NOT EXISTS (
      SELECT 1
      FROM public.packaging_slips linked
      WHERE linked.cogs_journal_entry_id = je.id
  );
