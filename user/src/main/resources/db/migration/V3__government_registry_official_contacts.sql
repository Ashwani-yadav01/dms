-- create-drop may have removed the table after V1/V2 were recorded as applied.
-- Recreate the current registry shape before applying the additive contact fields.
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
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    office_latitude DOUBLE PRECISION,
    office_longitude DOUBLE PRECISION,
    duty_radius_km DOUBLE PRECISION NOT NULL DEFAULT 20.0,
    official_email VARCHAR(255),
    official_phone VARCHAR(20)
);

ALTER TABLE government_registry
    ADD COLUMN IF NOT EXISTS office_latitude DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS office_longitude DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS duty_radius_km DOUBLE PRECISION NOT NULL DEFAULT 20.0,
    ADD COLUMN IF NOT EXISTS official_email VARCHAR(255),
    ADD COLUMN IF NOT EXISTS official_phone VARCHAR(20);
