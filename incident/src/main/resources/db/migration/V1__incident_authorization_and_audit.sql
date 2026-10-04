DO $$
BEGIN
    IF to_regclass('public.tbl_incidents') IS NOT NULL THEN
        ALTER TABLE tbl_incidents ADD COLUMN IF NOT EXISTS assigned_department_id UUID;
        ALTER TABLE tbl_incidents ADD COLUMN IF NOT EXISTS assigned_official_id UUID;
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS incident_audit_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    incident_id UUID NOT NULL,
    performed_by UUID,
    actor_type VARCHAR(16) NOT NULL DEFAULT 'SYSTEM',
    action VARCHAR(32) NOT NULL,
    previous_status VARCHAR(20),
    new_status VARCHAR(20),
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_incident_audit_incident ON incident_audit_log(incident_id);
