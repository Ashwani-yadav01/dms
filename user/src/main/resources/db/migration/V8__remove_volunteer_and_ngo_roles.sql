-- V8: Remove VOLUNTEER and NGO roles and drop unused profile tables
DO $$
BEGIN
    IF to_regclass('public.users') IS NOT NULL THEN
        -- Delete any legacy test/dev users with VOLUNTEER or NGO roles if present
        DELETE FROM users WHERE role IN ('VOLUNTEER', 'NGO');
        
        ALTER TABLE users DROP CONSTRAINT IF EXISTS users_role_check;
        ALTER TABLE users ADD CONSTRAINT users_role_check
            CHECK (role IN ('CITIZEN', 'GOVERNMENT_OFFICIAL', 'SUPER_ADMIN', 'DISTRICT_ADMIN', 'RESCUE_TEAM', 'HOSPITAL'));
    END IF;

    DROP TABLE IF EXISTS volunteer_skills;
    DROP TABLE IF EXISTS volunteer_profiles;
    DROP TABLE IF EXISTS ngo_profiles;
END $$;
