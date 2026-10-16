-- Make incident_id nullable in tbl_patient_admissions to support walk-in and mass casualty admissions without prior centralized incident records
ALTER TABLE tbl_patient_admissions ALTER COLUMN incident_id DROP NOT NULL;
