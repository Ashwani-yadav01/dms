CREATE TABLE IF NOT EXISTS government_registry (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    government_profile_id VARCHAR(32),
    employee_id VARCHAR(255) NOT NULL UNIQUE,
    department_name VARCHAR(255) NOT NULL,
    designation VARCHAR(255) NOT NULL,
    hierarchy_level VARCHAR(30) NOT NULL,
    authorization_code VARCHAR(255) NOT NULL UNIQUE,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

DO $$
BEGIN
    IF to_regclass('public.government_official_profiles') IS NOT NULL THEN
        ALTER TABLE government_official_profiles ADD COLUMN IF NOT EXISTS government_profile_id VARCHAR(32);
        CREATE UNIQUE INDEX IF NOT EXISTS uk_government_profile_id
            ON government_official_profiles(government_profile_id) WHERE government_profile_id IS NOT NULL;
    END IF;
    IF to_regclass('public.user_profiles') IS NOT NULL THEN
        CREATE TABLE IF NOT EXISTS rescue_team_profiles (
            user_id UUID PRIMARY KEY REFERENCES user_profiles(user_id) ON DELETE CASCADE,
            team_name VARCHAR(255) NOT NULL,
            team_code VARCHAR(255) NOT NULL UNIQUE,
            department_id UUID,
            team_lead_name VARCHAR(255) NOT NULL,
            team_size INTEGER NOT NULL,
            is_verified BOOLEAN NOT NULL DEFAULT FALSE
        );
    END IF;
END $$;

INSERT INTO government_registry(employee_id, department_name, designation, hierarchy_level, authorization_code, status)
VALUES
 ('UP-GOV-18492', 'DISASTER MANAGEMENT', 'District Officer', 'DISTRICT_OFFICER', 'GOV-A8F4K2', 'ACTIVE'),
 ('UP-GOV-29481', 'State Emergency Operations', 'Emergency Coordinator', 'STATE_OFFICER', 'GOV-M7P2Q9', 'ACTIVE'),
 ('UP-GOV-38127', 'Fire and Emergency Services', 'Response Officer', 'DISTRICT_OFFICER', 'GOV-R4K8L1', 'ACTIVE'),
 ('UP-GOV-49106', 'Disaster Management', 'Inactive Test Officer', 'LOCAL_OFFICER', 'GOV-X2N6B9', 'INACTIVE')
ON CONFLICT (employee_id) DO NOTHING;
