-- Allow finished good reservations to coexist with raw material reservations
ALTER TABLE inventory_reservations
    ALTER COLUMN raw_material_id DROP NOT NULL;
