-- V4: Update tbl_incidents_incident_type_check constraint to include ACCIDENT incident type
DO $$
BEGIN
    IF to_regclass('public.tbl_incidents') IS NOT NULL THEN
        ALTER TABLE tbl_incidents DROP CONSTRAINT IF EXISTS tbl_incidents_incident_type_check;
        ALTER TABLE tbl_incidents ADD CONSTRAINT tbl_incidents_incident_type_check
            CHECK (incident_type IN ('FLOOD', 'FIRE', 'COLLAPSE', 'MEDICAL', 'ACCIDENT', 'HAZMAT', 'OTHER'));
    END IF;
END $$;
