CREATE TABLE IF NOT EXISTS district_admin_registry (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_id VARCHAR(255) NOT NULL UNIQUE,
    authorization_code VARCHAR(255) NOT NULL UNIQUE,
    admin_email VARCHAR(255),
    admin_phone VARCHAR(20),
    district_name VARCHAR(255),
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

DO $$
BEGIN
    IF to_regclass('public.user_profiles') IS NOT NULL THEN
        CREATE TABLE IF NOT EXISTS district_admin_profiles (
            user_id UUID PRIMARY KEY REFERENCES user_profiles(user_id) ON DELETE CASCADE,
            district_name VARCHAR(255),
            jurisdiction VARCHAR(255),
            designation VARCHAR(255),
            department VARCHAR(255),
            control_room_helpline VARCHAR(50)
        );
    END IF;
END $$;
