-- Add overdue_notified flag to track 3-hour SLA breach escalation alerts
DO $$
BEGIN
    IF to_regclass('public.tbl_incidents') IS NOT NULL THEN
        ALTER TABLE tbl_incidents
        ADD COLUMN IF NOT EXISTS overdue_notified BOOLEAN NOT NULL DEFAULT FALSE;

        CREATE INDEX IF NOT EXISTS idx_incident_sla_overdue
        ON tbl_incidents (status, overdue_notified, created_at);
    END IF;
END $$;
