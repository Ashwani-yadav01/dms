-- V5: Update users_role_check constraint to include HOSPITAL role
DO $$
BEGIN
    IF to_regclass('public.users') IS NOT NULL THEN
        ALTER TABLE users DROP CONSTRAINT IF EXISTS users_role_check;
        ALTER TABLE users ADD CONSTRAINT users_role_check
            CHECK (role IN ('CITIZEN', 'VOLUNTEER', 'NGO', 'GOVERNMENT_OFFICIAL', 'SUPER_ADMIN', 'DISTRICT_ADMIN', 'RESCUE_TEAM', 'HOSPITAL'));
    END IF;
END $$;
