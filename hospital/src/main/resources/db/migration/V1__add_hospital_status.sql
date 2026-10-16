-- Add hospital status column with default for existing rows
ALTER TABLE tbl_hospitals ADD COLUMN status varchar(16) NOT NULL DEFAULT 'ACTIVE';

-- Add check constraint
ALTER TABLE tbl_hospitals ADD CONSTRAINT chk_hospital_status CHECK (status IN ('ACTIVE','SUSPENDED','DEACTIVATED'));

-- Remove the strict NOT NULL constraints from the UUID columns
