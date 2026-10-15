-- V3: Add is_reporting_on_behalf column to tbl_incidents
DO $$
BEGIN
    IF to_regclass('public.tbl_incidents') IS NOT NULL THEN
        ALTER TABLE tbl_incidents ADD COLUMN IF NOT EXISTS is_reporting_on_behalf BOOLEAN NOT NULL DEFAULT FALSE;
    END IF;
END $$;
