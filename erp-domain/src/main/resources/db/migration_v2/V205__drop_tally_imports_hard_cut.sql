-- Flyway v2: retire the old Tally import storage through a forward migration
-- while preserving historical migration checksums for already-applied schemas.

DROP TABLE IF EXISTS public.tally_imports;
