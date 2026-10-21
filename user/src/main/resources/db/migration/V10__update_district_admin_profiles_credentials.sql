DO $$
BEGIN
    IF to_regclass('public.district_admin_profiles') IS NOT NULL THEN
        IF NOT EXISTS (
            SELECT 1 FROM information_schema.columns 
            WHERE table_name = 'district_admin_profiles' AND column_name = 'employee_id'
        ) THEN
            ALTER TABLE district_admin_profiles ADD COLUMN employee_id VARCHAR(255);
        END IF;

        IF NOT EXISTS (
            SELECT 1 FROM information_schema.columns 
            WHERE table_name = 'district_admin_profiles' AND column_name = 'verification_code'
        ) THEN
            ALTER TABLE district_admin_profiles ADD COLUMN verification_code VARCHAR(255);
        END IF;

        IF NOT EXISTS (
            SELECT 1 FROM information_schema.columns 
            WHERE table_name = 'district_admin_profiles' AND column_name = 'helpline_number'
        ) THEN
            ALTER TABLE district_admin_profiles ADD COLUMN helpline_number VARCHAR(50);
            UPDATE district_admin_profiles SET helpline_number = control_room_helpline WHERE helpline_number IS NULL;
        END IF;
    END IF;
END $$;
