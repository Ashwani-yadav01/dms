ALTER TABLE government_registry
    ALTER COLUMN department_name DROP NOT NULL,
    ALTER COLUMN designation DROP NOT NULL,
    ALTER COLUMN hierarchy_level DROP NOT NULL;
