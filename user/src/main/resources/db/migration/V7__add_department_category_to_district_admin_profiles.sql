DO $$
BEGIN
    IF to_regclass('public.district_admin_profiles') IS NOT NULL THEN
        IF NOT EXISTS (
            SELECT 1 FROM information_schema.columns 
            WHERE table_name = 'district_admin_profiles' AND column_name = 'department_category'
        ) THEN
            ALTER TABLE district_admin_profiles ADD COLUMN department_category VARCHAR(30);
        END IF;
    END IF;
END $$;
