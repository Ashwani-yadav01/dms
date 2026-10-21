-- V11: Add department_category column to government_registry table
DO $$
BEGIN
    IF to_regclass('public.government_registry') IS NOT NULL THEN
        ALTER TABLE government_registry ADD COLUMN IF NOT EXISTS department_category VARCHAR(40);
    END IF;
END $$;
