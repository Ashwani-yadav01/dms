-- V9: Add department_category to district_admin_registry
ALTER TABLE district_admin_registry ADD COLUMN IF NOT EXISTS department_category VARCHAR(40);
