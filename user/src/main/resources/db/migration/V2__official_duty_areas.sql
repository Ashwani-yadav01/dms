ALTER TABLE government_registry
    ADD COLUMN IF NOT EXISTS office_latitude DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS office_longitude DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS duty_radius_km DOUBLE PRECISION NOT NULL DEFAULT 20.0;

-- Mock registry duty locations. Replace these with authoritative office coordinates
-- when loading real government registry records.
UPDATE government_registry
SET office_latitude = 26.8467, office_longitude = 80.9462
WHERE employee_id = 'UP-GOV-18492' AND office_latitude IS NULL AND office_longitude IS NULL;

UPDATE government_registry
SET office_latitude = 26.4499, office_longitude = 80.3319
WHERE employee_id = 'UP-GOV-29481' AND office_latitude IS NULL AND office_longitude IS NULL;

UPDATE government_registry
SET office_latitude = 25.3176, office_longitude = 82.9739
WHERE employee_id = 'UP-GOV-38127' AND office_latitude IS NULL AND office_longitude IS NULL;

DO $$
BEGIN
    IF to_regclass('public.government_official_profiles') IS NOT NULL THEN
        ALTER TABLE government_official_profiles
            ADD COLUMN IF NOT EXISTS duty_office_latitude DOUBLE PRECISION,
            ADD COLUMN IF NOT EXISTS duty_office_longitude DOUBLE PRECISION,
            ADD COLUMN IF NOT EXISTS duty_radius_km DOUBLE PRECISION NOT NULL DEFAULT 20.0;

        UPDATE government_official_profiles p
        SET duty_office_latitude = r.office_latitude,
            duty_office_longitude = r.office_longitude,
            duty_radius_km = r.duty_radius_km
        FROM government_registry r
        WHERE p.employee_id = r.employee_id
          AND p.duty_office_latitude IS NULL
          AND p.duty_office_longitude IS NULL;
    END IF;
END $$;
