-- V12: Update government_official_profiles_department_category_check constraint to include all DepartmentCategory values
DO $$
BEGIN
    IF to_regclass('public.government_official_profiles') IS NOT NULL THEN
        ALTER TABLE government_official_profiles
            DROP CONSTRAINT IF EXISTS government_official_profiles_department_category_check;

        ALTER TABLE government_official_profiles
            ADD CONSTRAINT government_official_profiles_department_category_check
            CHECK (department_category IN (
                'DEPARTMENT_MANAGEMENT',
                'DISASTER_MANAGEMENT',
                'DISASTER_RESCUE',
                'FIRE_SAFETY',
                'ACCIDENT',
                'MEDICAL_RESPONSE',
                'MEDICAL_MANAGEMENT',
                'HOSPITAL_MANAGEMENT',
                'LAW_ENFORCEMENT',
                'REVENUE_RELIEF',
                'WATER_AND_SANITATION',
                'INFRASTRUCTURE_REPAIR',
                'OTHER'
            ));
    END IF;
END $$;
