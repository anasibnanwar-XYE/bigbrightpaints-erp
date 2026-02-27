-- Add optional media field for promotion creatives.
ALTER TABLE public.promotions
    ADD COLUMN IF NOT EXISTS image_url character varying(1024);

COMMENT ON COLUMN public.promotions.image_url IS 'Optional promotion image URL/path.';
