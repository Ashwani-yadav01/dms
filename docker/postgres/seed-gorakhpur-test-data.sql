-- ==============================================================================
-- DMS Clean Truncate & Deterministic Seed Script (Sahjanwa & Gorakhpur Region)
-- Exactly:
--   - 1 Citizen (Sahajanwa)
--   - 1 Super Admin (Gorakhpur Apex)
--   - All 6 District Admins (Department Management, Medical Management, Medical Response, Fire Safety, Accident, Disaster Management)
--   - 4 Government Officials (Fire Safety, Disaster Management, Accident, Medical Response)
--   - 5 Hospitals (Sahjanwa Hospital, BRD Medical College, City General Hospital, Rustampur Trauma Centre, Gorakhnath Community Hospital)
-- ==============================================================================

-- ------------------------------------------------------------------------------
-- 1. TRUNCATE INCIDENT DB
-- ------------------------------------------------------------------------------
\c incident_db;

DO $$
BEGIN
    IF to_regclass('public.tbl_incidents') IS NOT NULL THEN
        TRUNCATE TABLE tbl_incidents CASCADE;
    END IF;
END $$;

-- ------------------------------------------------------------------------------
-- 2. TRUNCATE RESCUE DB
-- ------------------------------------------------------------------------------
\c rescue_db;

DO $$
BEGIN
    IF to_regclass('public.tbl_rescue_teams') IS NOT NULL THEN
        TRUNCATE TABLE tbl_rescue_teams CASCADE;
    END IF;
    IF to_regclass('public.tbl_rescue_departments') IS NOT NULL THEN
        TRUNCATE TABLE tbl_rescue_departments CASCADE;
    END IF;
END $$;

-- Seed Sahjanwa Fire Station linked to District Admin & Fire Official
INSERT INTO tbl_rescue_departments (
    id, name, type, jurisdiction_code, latitude, longitude, contact_phone,
    total_capacity, active_missions_count, is_available, district_admin_id, governing_official_id,
    created_at, updated_at
) VALUES (
    '50000000-0000-0000-0000-000000000011',
    'DMS Test - Sahjanwa Fire Station',
    'FIRE_STATION',
    'GORAKHPUR',
    26.7500,
    83.2100,
    '0551-220044',
    5,
    0,
    true,
    '20000000-0000-0000-0000-000000000001',
    '30000000-0000-0000-0000-000000000001',
    NOW(),
    NOW()
) ON CONFLICT (id) DO UPDATE SET
    district_admin_id = '20000000-0000-0000-0000-000000000001',
    governing_official_id = '30000000-0000-0000-0000-000000000001';

-- ------------------------------------------------------------------------------
-- 3. TRUNCATE LOGISTICS & NOTIFICATION DBS
-- ------------------------------------------------------------------------------
\c logistics_db;

DO $$
BEGIN
    IF to_regclass('public.tbl_orders') IS NOT NULL THEN
        TRUNCATE TABLE tbl_orders CASCADE;
    END IF;
    IF to_regclass('public.tbl_vehicles') IS NOT NULL THEN
        TRUNCATE TABLE tbl_vehicles CASCADE;
    END IF;
END $$;

\c notification_db;

DO $$
BEGIN
    IF to_regclass('public.tbl_notification_logs') IS NOT NULL THEN
        TRUNCATE TABLE tbl_notification_logs CASCADE;
    END IF;
END $$;

-- ------------------------------------------------------------------------------
-- 4. TRUNCATE & SEED HOSPITAL DB (5 Hospitals around Sahjanwa and Gorakhpur)
-- ------------------------------------------------------------------------------
\c hospital_db;

DO $$
BEGIN
    IF to_regclass('public.hospital_specialities') IS NOT NULL THEN
        TRUNCATE TABLE hospital_specialities CASCADE;
    END IF;
    IF to_regclass('public.tbl_hospital_inventories') IS NOT NULL THEN
        TRUNCATE TABLE tbl_hospital_inventories CASCADE;
    END IF;
    IF to_regclass('public.tbl_patient_admissions') IS NOT NULL THEN
        TRUNCATE TABLE tbl_patient_admissions CASCADE;
    END IF;
    IF to_regclass('public.tbl_hospital_delivery_receipts') IS NOT NULL THEN
        TRUNCATE TABLE tbl_hospital_delivery_receipts CASCADE;
    END IF;
    IF to_regclass('public.tbl_hospitals') IS NOT NULL THEN
        TRUNCATE TABLE tbl_hospitals CASCADE;
    END IF;
END $$;

-- Hospital 1: Sahjanwa Hospital (Sahajanwa)
INSERT INTO tbl_hospitals (
    id, name, email, phone_number, address, type, latitude, longitude, elevation_meters,
    total_general_beds, available_general_beds, total_icu_beds, available_icu_beds,
    is_accepting_patients, status, admin_user_id
) VALUES (
    '40000000-0000-0000-0000-000000000011',
    'DMS Test - Sahjanwa Hospital',
    'hospital.sahajanwa@dms.gov.in',
    '+919876543231',
    'NH-28 Main Road, Sahajanwa, Gorakhpur',
    'GENERAL_HOSPITAL',
    26.7500,
    83.2100,
    95.0,
    60,
    15,
    10,
    4,
    true,
    'ACTIVE',
    '40000000-0000-0000-0000-000000000001'
);

-- Hospital 2: BRD Medical College (Gorakhpur)
INSERT INTO tbl_hospitals (
    id, name, email, phone_number, address, type, latitude, longitude, elevation_meters,
    total_general_beds, available_general_beds, total_icu_beds, available_icu_beds,
    is_accepting_patients, status, admin_user_id
) VALUES (
    '40000000-0000-0000-0000-000000000012',
    'DMS Test - BRD Medical College',
    'hospital.brd@dms.gov.in',
    '+919876543232',
    'BRD Medical College & Hospital, Medical College Road, Gorakhpur',
    'TRAUMA_CENTER',
    26.8133,
    83.4025,
    98.0,
    150,
    40,
    50,
    12,
    true,
    'ACTIVE',
    '40000000-0000-0000-0000-000000000002'
);

-- Hospital 3: City General Hospital (Golghar, Gorakhpur)
INSERT INTO tbl_hospitals (
    id, name, email, phone_number, address, type, latitude, longitude, elevation_meters,
    total_general_beds, available_general_beds, total_icu_beds, available_icu_beds,
    is_accepting_patients, status, admin_user_id
) VALUES (
    '40000000-0000-0000-0000-000000000013',
    'DMS Test - City General Hospital',
    'hospital.city@dms.gov.in',
    '+919876543233',
    'Civil Lines, Golghar, Gorakhpur',
    'GENERAL_HOSPITAL',
    26.7580,
    83.3730,
    96.0,
    80,
    20,
    15,
    5,
    true,
    'ACTIVE',
    '40000000-0000-0000-0000-000000000003'
);

-- Hospital 4: Rustampur Trauma Centre (Rustampur, Gorakhpur)
INSERT INTO tbl_hospitals (
    id, name, email, phone_number, address, type, latitude, longitude, elevation_meters,
    total_general_beds, available_general_beds, total_icu_beds, available_icu_beds,
    is_accepting_patients, status, admin_user_id
) VALUES (
    '40000000-0000-0000-0000-000000000014',
    'DMS Test - Rustampur Trauma Centre',
    'hospital.rustampur@dms.gov.in',
    '+919876543234',
    'Rustampur Bypass, Gorakhpur',
    'TRAUMA_CENTER',
    26.7300,
    83.3600,
    94.0,
    70,
    18,
    12,
    3,
    true,
    'ACTIVE',
    '40000000-0000-0000-0000-000000000004'
);

-- Hospital 5: Gorakhnath Community Hospital (Gorakhnath, Gorakhpur)
INSERT INTO tbl_hospitals (
    id, name, email, phone_number, address, type, latitude, longitude, elevation_meters,
    total_general_beds, available_general_beds, total_icu_beds, available_icu_beds,
    is_accepting_patients, status, admin_user_id
) VALUES (
    '40000000-0000-0000-0000-000000000015',
    'DMS Test - Gorakhnath Community Hospital',
    'hospital.gorakhnath@dms.gov.in',
    '+919876543235',
    'Gorakhnath Road, Gorakhpur',
    'GENERAL_HOSPITAL',
    26.7900,
    83.3900,
    97.0,
    60,
    12,
    8,
    2,
    true,
    'ACTIVE',
    '40000000-0000-0000-0000-000000000005'
);

-- Hospital Specialities
INSERT INTO hospital_specialities (hospital_id, speciality) VALUES
('40000000-0000-0000-0000-000000000011', 'LEVEL_1_TRAUMA'),
('40000000-0000-0000-0000-000000000011', 'CARDIAC_ICU'),
('40000000-0000-0000-0000-000000000012', 'LEVEL_1_TRAUMA'),
('40000000-0000-0000-0000-000000000012', 'CARDIAC_ICU'),
('40000000-0000-0000-0000-000000000012', 'BURN_UNIT'),
('40000000-0000-0000-0000-000000000013', 'CARDIAC_ICU'),
('40000000-0000-0000-0000-000000000013', 'NEONATAL_ICU'),
('40000000-0000-0000-0000-000000000014', 'LEVEL_1_TRAUMA'),
('40000000-0000-0000-0000-000000000014', 'BURN_UNIT'),
('40000000-0000-0000-0000-000000000015', 'NEONATAL_ICU'),
('40000000-0000-0000-0000-000000000015', 'ISOLATION_WARD');


-- ------------------------------------------------------------------------------
-- 5. TRUNCATE & SEED USER DB
-- ------------------------------------------------------------------------------
\c "userDb";

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Truncate all tables in userDb
DO $$
BEGIN
    IF to_regclass('public.citizen_profiles') IS NOT NULL THEN
        TRUNCATE TABLE citizen_profiles CASCADE;
    END IF;
    IF to_regclass('public.district_admin_profiles') IS NOT NULL THEN
        TRUNCATE TABLE district_admin_profiles CASCADE;
    END IF;
    IF to_regclass('public.district_admin_registry') IS NOT NULL THEN
        TRUNCATE TABLE district_admin_registry CASCADE;
    END IF;
    IF to_regclass('public.government_official_profiles') IS NOT NULL THEN
        TRUNCATE TABLE government_official_profiles CASCADE;
    END IF;
    IF to_regclass('public.government_registry') IS NOT NULL THEN
        TRUNCATE TABLE government_registry CASCADE;
    END IF;
    IF to_regclass('public.hospital_registry') IS NOT NULL THEN
        TRUNCATE TABLE hospital_registry CASCADE;
    END IF;
    IF to_regclass('public.rescue_team_profiles') IS NOT NULL THEN
        TRUNCATE TABLE rescue_team_profiles CASCADE;
    END IF;
    IF to_regclass('public.user_profiles') IS NOT NULL THEN
        TRUNCATE TABLE user_profiles CASCADE;
    END IF;
    IF to_regclass('public.users') IS NOT NULL THEN
        TRUNCATE TABLE users CASCADE;
    END IF;
END $$;


-- ------------------------------------------------------------------------------
-- A. SUPER ADMIN (Gorakhpur Apex Command)
-- superadmin@dms.gov.in / Admin@123
-- ------------------------------------------------------------------------------
INSERT INTO users (id, email, mobile_number, password, role, profile_completed, created_at, updated_at)
VALUES (
    '00000000-0000-0000-0000-000000000001',
    'superadmin@dms.gov.in',
    '+919876543200',
    crypt('Admin@123', gen_salt('bf', 10)),
    'SUPER_ADMIN',
    true,
    NOW(),
    NOW()
);

INSERT INTO user_profiles (user_id, name, address_line, city, district, state, pincode, latitude, longitude, created_at, updated_at)
VALUES (
    '00000000-0000-0000-0000-000000000001',
    'National Apex Administrator',
    'Civil Lines Apex Complex',
    'Gorakhpur',
    'Gorakhpur',
    'Uttar Pradesh',
    '273001',
    26.7606,
    83.3732,
    NOW(),
    NOW()
);


-- ------------------------------------------------------------------------------
-- B. CITIZEN (Exactly 1 Citizen in Sahajanwa)
-- citizen.sahajanwa@dms.gov.in / Citizen@123
-- ------------------------------------------------------------------------------
INSERT INTO users (id, email, mobile_number, password, role, profile_completed, created_at, updated_at)
VALUES (
    '10000000-0000-0000-0000-000000000001',
    'citizen.sahajanwa@dms.gov.in',
    '+919876543210',
    crypt('Citizen@123', gen_salt('bf', 10)),
    'CITIZEN',
    true,
    NOW(),
    NOW()
);

INSERT INTO user_profiles (user_id, name, address_line, city, district, state, pincode, latitude, longitude, created_at, updated_at)
VALUES (
    '10000000-0000-0000-0000-000000000001',
    'Ramesh Kumar',
    'Ward 5, Station Road',
    'Sahajanwa',
    'Gorakhpur',
    'Uttar Pradesh',
    '273209',
    26.7483,
    83.1931,
    NOW(),
    NOW()
);

INSERT INTO citizen_profiles (user_id, gender, date_of_birth, occupation, blood_group, emergency_contact_number)
VALUES (
    '10000000-0000-0000-0000-000000000001',
    'MALE',
    '1992-05-14',
    'Private Sector Employee',
    'O_POSITIVE',
    '+919876543200'
);


-- ------------------------------------------------------------------------------
-- C. DISTRICT ADMINS (All 6 Governance Categories around Sahjanwa & Gorakhpur)
-- Password for all: District@123
-- ------------------------------------------------------------------------------

-- 1. DEPARTMENT_MANAGEMENT (Overall Command & Station Admin)
INSERT INTO users (id, email, mobile_number, password, role, profile_completed, created_at, updated_at)
VALUES (
    '20000000-0000-0000-0000-000000000001',
    'districtadmin.gkp@dms.gov.in',
    '+919876543220',
    crypt('District@123', gen_salt('bf', 10)),
    'DISTRICT_ADMIN',
    true,
    NOW(),
    NOW()
);

INSERT INTO user_profiles (user_id, name, address_line, city, district, state, pincode, latitude, longitude, created_at, updated_at)
VALUES (
    '20000000-0000-0000-0000-000000000001',
    'Gorakhpur District Disaster Management Authority',
    'DM Office, Civil Lines',
    'Gorakhpur',
    'Gorakhpur',
    'Uttar Pradesh',
    '273001',
    26.7588,
    83.3697,
    NOW(),
    NOW()
);

INSERT INTO district_admin_registry (id, admin_email, admin_phone, district_name, department_category, employee_id, authorization_code, status, created_at, updated_at)
VALUES (
    gen_random_uuid(),
    'districtadmin.gkp@dms.gov.in',
    '+919876543220',
    'Gorakhpur',
    'DEPARTMENT_MANAGEMENT',
    'DMS-DIST-GKP01',
    'DIST-GKP-ALL01',
    'CONSUMED',
    NOW(),
    NOW()
);

INSERT INTO district_admin_profiles (user_id, district_name, jurisdiction, designation, department, department_category, control_room_helpline, employee_id, verification_code, helpline_number)
VALUES (
    '20000000-0000-0000-0000-000000000001',
    'Gorakhpur',
    'GORAKHPUR',
    'District Disaster Management Officer (DDMO)',
    'Disaster Management & District Administration',
    'DEPARTMENT_MANAGEMENT',
    '0551-220011',
    'DMS-DIST-GKP01',
    'DIST-GKP-ALL01',
    '0551-220011'
);

-- 2. MEDICAL_MANAGEMENT (Healthcare Authority - Hospital Accreditation, Demands & Reserves)
INSERT INTO users (id, email, mobile_number, password, role, profile_completed, created_at, updated_at)
VALUES (
    '20000000-0000-0000-0000-000000000002',
    'districtadmin.medical.gkp@dms.gov.in',
    '+919876543221',
    crypt('District@123', gen_salt('bf', 10)),
    'DISTRICT_ADMIN',
    true,
    NOW(),
    NOW()
);

INSERT INTO user_profiles (user_id, name, address_line, city, district, state, pincode, latitude, longitude, created_at, updated_at)
VALUES (
    '20000000-0000-0000-0000-000000000002',
    'Chief Medical Officer (CMO) Gorakhpur',
    'CMO Office, District Hospital Road',
    'Gorakhpur',
    'Gorakhpur',
    'Uttar Pradesh',
    '273001',
    26.7610,
    83.3750,
    NOW(),
    NOW()
);

INSERT INTO district_admin_registry (id, admin_email, admin_phone, district_name, department_category, employee_id, authorization_code, status, created_at, updated_at)
VALUES (
    gen_random_uuid(),
    'districtadmin.medical.gkp@dms.gov.in',
    '+919876543221',
    'Gorakhpur',
    'MEDICAL_MANAGEMENT',
    'DMS-DIST-GKPMED',
    'DIST-GKP-MED01',
    'CONSUMED',
    NOW(),
    NOW()
);

INSERT INTO district_admin_profiles (user_id, district_name, jurisdiction, designation, department, department_category, control_room_helpline, employee_id, verification_code, helpline_number)
VALUES (
    '20000000-0000-0000-0000-000000000002',
    'Gorakhpur',
    'GORAKHPUR',
    'Chief Medical Officer',
    'District Health & Medical Administration',
    'MEDICAL_MANAGEMENT',
    '0551-220022',
    'DMS-DIST-GKPMED',
    'DIST-GKP-MED01',
    '0551-220022'
);

-- 3. MEDICAL_RESPONSE (Verify & Triage Medical Emergency Incidents)
INSERT INTO users (id, email, mobile_number, password, role, profile_completed, created_at, updated_at)
VALUES (
    '20000000-0000-0000-0000-000000000003',
    'districtadmin.medresp.gkp@dms.gov.in',
    '+919876543222',
    crypt('District@123', gen_salt('bf', 10)),
    'DISTRICT_ADMIN',
    true,
    NOW(),
    NOW()
);

INSERT INTO user_profiles (user_id, name, address_line, city, district, state, pincode, latitude, longitude, created_at, updated_at)
VALUES (
    '20000000-0000-0000-0000-000000000003',
    'District Medical Emergency Response Controller',
    'Trauma & Emergency Operations Center, Sahajanwa-Gorakhpur Highway',
    'Sahajanwa',
    'Gorakhpur',
    'Uttar Pradesh',
    '273209',
    26.7520,
    83.2800,
    NOW(),
    NOW()
);

INSERT INTO district_admin_registry (id, admin_email, admin_phone, district_name, department_category, employee_id, authorization_code, status, created_at, updated_at)
VALUES (
    gen_random_uuid(),
    'districtadmin.medresp.gkp@dms.gov.in',
    '+919876543222',
    'Gorakhpur',
    'MEDICAL_RESPONSE',
    'DMS-DIST-GKPMR',
    'DIST-GKP-MR01',
    'CONSUMED',
    NOW(),
    NOW()
);

INSERT INTO district_admin_profiles (user_id, district_name, jurisdiction, designation, department, department_category, control_room_helpline, employee_id, verification_code, helpline_number)
VALUES (
    '20000000-0000-0000-0000-000000000003',
    'Gorakhpur',
    'GORAKHPUR',
    'Emergency Medical Triage Director',
    'Emergency Casualty & Medical Response Command',
    'MEDICAL_RESPONSE',
    '0551-220033',
    'DMS-DIST-GKPMR',
    'DIST-GKP-MR01',
    '0551-220033'
);

-- 4. FIRE_SAFETY (Verify & Manage Fire Incidents)
INSERT INTO users (id, email, mobile_number, password, role, profile_completed, created_at, updated_at)
VALUES (
    '20000000-0000-0000-0000-000000000004',
    'districtadmin.fire.gkp@dms.gov.in',
    '+919876543223',
    crypt('District@123', gen_salt('bf', 10)),
    'DISTRICT_ADMIN',
    true,
    NOW(),
    NOW()
);

INSERT INTO user_profiles (user_id, name, address_line, city, district, state, pincode, latitude, longitude, created_at, updated_at)
VALUES (
    '20000000-0000-0000-0000-000000000004',
    'District Fire & Safety Command Authority',
    'District Fire Headquarters, Golghar',
    'Gorakhpur',
    'Gorakhpur',
    'Uttar Pradesh',
    '273001',
    26.7580,
    83.3730,
    NOW(),
    NOW()
);

INSERT INTO district_admin_registry (id, admin_email, admin_phone, district_name, department_category, employee_id, authorization_code, status, created_at, updated_at)
VALUES (
    gen_random_uuid(),
    'districtadmin.fire.gkp@dms.gov.in',
    '+919876543223',
    'Gorakhpur',
    'FIRE_SAFETY',
    'DMS-DIST-GKPFS',
    'DIST-GKP-FS01',
    'CONSUMED',
    NOW(),
    NOW()
);

INSERT INTO district_admin_profiles (user_id, district_name, jurisdiction, designation, department, department_category, control_room_helpline, employee_id, verification_code, helpline_number)
VALUES (
    '20000000-0000-0000-0000-000000000004',
    'Gorakhpur',
    'GORAKHPUR',
    'Divisional Fire Marshal & Incident Commander',
    'Fire & Emergency Safety Services',
    'FIRE_SAFETY',
    '0551-220044',
    'DMS-DIST-GKPFS',
    'DIST-GKP-FS01',
    '0551-220044'
);

-- 5. ACCIDENT (Verify & Manage Highway Collisions & Traffic Incidents)
INSERT INTO users (id, email, mobile_number, password, role, profile_completed, created_at, updated_at)
VALUES (
    '20000000-0000-0000-0000-000000000005',
    'districtadmin.accident.gkp@dms.gov.in',
    '+919876543224',
    crypt('District@123', gen_salt('bf', 10)),
    'DISTRICT_ADMIN',
    true,
    NOW(),
    NOW()
);

INSERT INTO user_profiles (user_id, name, address_line, city, district, state, pincode, latitude, longitude, created_at, updated_at)
VALUES (
    '20000000-0000-0000-0000-000000000005',
    'Highway Transit & Collision Response Command',
    'NH-28 Highway Traffic Command Post',
    'Sahajanwa',
    'Gorakhpur',
    'Uttar Pradesh',
    '273209',
    26.7460,
    83.2150,
    NOW(),
    NOW()
);

INSERT INTO district_admin_registry (id, admin_email, admin_phone, district_name, department_category, employee_id, authorization_code, status, created_at, updated_at)
VALUES (
    gen_random_uuid(),
    'districtadmin.accident.gkp@dms.gov.in',
    '+919876543224',
    'Gorakhpur',
    'ACCIDENT',
    'DMS-DIST-GKPAC',
    'DIST-GKP-AC01',
    'CONSUMED',
    NOW(),
    NOW()
);

INSERT INTO district_admin_profiles (user_id, district_name, jurisdiction, designation, department, department_category, control_room_helpline, employee_id, verification_code, helpline_number)
VALUES (
    '20000000-0000-0000-0000-000000000005',
    'Gorakhpur',
    'GORAKHPUR',
    'Chief Traffic & Extrication Commander',
    'Road Accident & Highway Collision Department',
    'ACCIDENT',
    '0551-220055',
    'DMS-DIST-GKPAC',
    'DIST-GKP-AC01',
    '0551-220055'
);

-- 6. DISASTER_MANAGEMENT (Verify & Manage Flood / Landslide Incidents)
INSERT INTO users (id, email, mobile_number, password, role, profile_completed, created_at, updated_at)
VALUES (
    '20000000-0000-0000-0000-000000000006',
    'districtadmin.disaster.gkp@dms.gov.in',
    '+919876543225',
    crypt('District@123', gen_salt('bf', 10)),
    'DISTRICT_ADMIN',
    true,
    NOW(),
    NOW()
);

INSERT INTO user_profiles (user_id, name, address_line, city, district, state, pincode, latitude, longitude, created_at, updated_at)
VALUES (
    '20000000-0000-0000-0000-000000000006',
    'Disaster Rescue & Relief Authority Gorakhpur',
    'DEOC Building, Collectorate',
    'Gorakhpur',
    'Gorakhpur',
    'Uttar Pradesh',
    '273001',
    26.7550,
    83.3700,
    NOW(),
    NOW()
);

INSERT INTO district_admin_registry (id, admin_email, admin_phone, district_name, department_category, employee_id, authorization_code, status, created_at, updated_at)
VALUES (
    gen_random_uuid(),
    'districtadmin.disaster.gkp@dms.gov.in',
    '+919876543225',
    'Gorakhpur',
    'DISASTER_MANAGEMENT',
    'DMS-DIST-GKPDM',
    'DIST-GKP-DM01',
    'CONSUMED',
    NOW(),
    NOW()
);

INSERT INTO district_admin_profiles (user_id, district_name, jurisdiction, designation, department, department_category, control_room_helpline, employee_id, verification_code, helpline_number)
VALUES (
    '20000000-0000-0000-0000-000000000006',
    'Gorakhpur',
    'GORAKHPUR',
    'Zonal Relief & Flood Operations Commander',
    'Disaster Rescue & Relief Department',
    'DISASTER_MANAGEMENT',
    '0551-220066',
    'DMS-DIST-GKPDM',
    'DIST-GKP-DM01',
    '0551-220066'
);


-- ------------------------------------------------------------------------------
-- D. GOVERNMENT OFFICIALS (4 Operational On-Scene Responders around Sahjanwa)
-- Password for all: Official@123
-- ------------------------------------------------------------------------------

-- 1. FIRE_SAFETY Official
INSERT INTO users (id, email, mobile_number, password, role, profile_completed, created_at, updated_at)
VALUES (
    '30000000-0000-0000-0000-000000000001',
    'official.fire.gkp@dms.gov.in',
    '+919876543251',
    crypt('Official@123', gen_salt('bf', 10)),
    'GOVERNMENT_OFFICIAL',
    true,
    NOW(),
    NOW()
);

INSERT INTO user_profiles (user_id, name, address_line, city, district, state, pincode, latitude, longitude, created_at, updated_at)
VALUES (
    '30000000-0000-0000-0000-000000000001',
    'Chief Fire Officer Suresh Chandra',
    'Fire Post Sahajanwa-Gorakhpur, NH-28',
    'Sahajanwa',
    'Gorakhpur',
    'Uttar Pradesh',
    '273209',
    26.7500,
    83.2120,
    NOW(),
    NOW()
);

INSERT INTO government_registry (id, employee_id, department_name, department_category, designation, official_email, official_phone, authorization_code, hierarchy_level, office_latitude, office_longitude, duty_radius_km, status, created_at, updated_at)
VALUES (
    gen_random_uuid(),
    'UP-GOV-GKP-FS',
    'Fire and Emergency Services',
    'FIRE_SAFETY',
    'Divisional Fire Officer',
    'official.fire.gkp@dms.gov.in',
    '+919876543251',
    'GOV-GKP-FS01',
    'DISTRICT_OFFICER',
    26.7500,
    83.2120,
    35.0,
    'ACTIVE',
    NOW(),
    NOW()
);

INSERT INTO government_official_profiles (user_id, department_name, department_category, designation, employee_id, official_phone, hierarchy_level, status, duty_radius_km, duty_office_latitude, duty_office_longitude, is_verified, government_profile_id, jurisdiction_code)
VALUES (
    '30000000-0000-0000-0000-000000000001',
    'Fire and Emergency Services',
    'FIRE_SAFETY',
    'Divisional Fire Officer',
    'UP-GOV-GKP-FS',
    '+919876543251',
    'DISTRICT_OFFICER',
    'AVAILABLE',
    35.0,
    26.7500,
    83.2120,
    true,
    'GOV-UP-GOV-GKP-FS',
    'GORAKHPUR'
);

-- 2. DISASTER_MANAGEMENT Official
INSERT INTO users (id, email, mobile_number, password, role, profile_completed, created_at, updated_at)
VALUES (
    '30000000-0000-0000-0000-000000000002',
    'official.disaster.gkp@dms.gov.in',
    '+919876543252',
    crypt('Official@123', gen_salt('bf', 10)),
    'GOVERNMENT_OFFICIAL',
    true,
    NOW(),
    NOW()
);

INSERT INTO user_profiles (user_id, name, address_line, city, district, state, pincode, latitude, longitude, created_at, updated_at)
VALUES (
    '30000000-0000-0000-0000-000000000002',
    'Dr. Arvind Verma',
    'Sahajanwa Sector Emergency Post, Ami River Basin',
    'Sahajanwa',
    'Gorakhpur',
    'Uttar Pradesh',
    '273209',
    26.7490,
    83.2080,
    NOW(),
    NOW()
);

INSERT INTO government_registry (id, employee_id, department_name, department_category, designation, official_email, official_phone, authorization_code, hierarchy_level, office_latitude, office_longitude, duty_radius_km, status, created_at, updated_at)
VALUES (
    gen_random_uuid(),
    'UP-GOV-GKP-DM',
    'Disaster Rescue & Relief Department',
    'DISASTER_MANAGEMENT',
    'Zonal Emergency Response Officer',
    'official.disaster.gkp@dms.gov.in',
    '+919876543252',
    'GOV-GKP-DM01',
    'DISTRICT_OFFICER',
    26.7490,
    83.2080,
    35.0,
    'ACTIVE',
    NOW(),
    NOW()
);

INSERT INTO government_official_profiles (user_id, department_name, department_category, designation, employee_id, official_phone, hierarchy_level, status, duty_radius_km, duty_office_latitude, duty_office_longitude, is_verified, government_profile_id, jurisdiction_code)
VALUES (
    '30000000-0000-0000-0000-000000000002',
    'Disaster Rescue & Relief Department',
    'DISASTER_MANAGEMENT',
    'Zonal Emergency Response Officer',
    'UP-GOV-GKP-DM',
    '+919876543252',
    'DISTRICT_OFFICER',
    'AVAILABLE',
    35.0,
    26.7490,
    83.2080,
    true,
    'GOV-UP-GOV-GKP-DM',
    'GORAKHPUR'
);

-- 3. ACCIDENT Official
INSERT INTO users (id, email, mobile_number, password, role, profile_completed, created_at, updated_at)
VALUES (
    '30000000-0000-0000-0000-000000000003',
    'official.accident.gkp@dms.gov.in',
    '+919876543253',
    crypt('Official@123', gen_salt('bf', 10)),
    'GOVERNMENT_OFFICIAL',
    true,
    NOW(),
    NOW()
);

INSERT INTO user_profiles (user_id, name, address_line, city, district, state, pincode, latitude, longitude, created_at, updated_at)
VALUES (
    '30000000-0000-0000-0000-000000000003',
    'Inspector Manoj Tiwari',
    'Highway Emergency Unit, NH-28 Sahajanwa',
    'Sahajanwa',
    'Gorakhpur',
    'Uttar Pradesh',
    '273209',
    26.7450,
    83.2200,
    NOW(),
    NOW()
);

INSERT INTO government_registry (id, employee_id, department_name, department_category, designation, official_email, official_phone, authorization_code, hierarchy_level, office_latitude, office_longitude, duty_radius_km, status, created_at, updated_at)
VALUES (
    gen_random_uuid(),
    'UP-GOV-GKP-AC',
    'Road Accident & Highway Collision Department',
    'ACCIDENT',
    'Traffic & Highway Safety Coordinator',
    'official.accident.gkp@dms.gov.in',
    '+919876543253',
    'GOV-GKP-AC01',
    'DISTRICT_OFFICER',
    26.7450,
    83.2200,
    35.0,
    'ACTIVE',
    NOW(),
    NOW()
);

INSERT INTO government_official_profiles (user_id, department_name, department_category, designation, employee_id, official_phone, hierarchy_level, status, duty_radius_km, duty_office_latitude, duty_office_longitude, is_verified, government_profile_id, jurisdiction_code)
VALUES (
    '30000000-0000-0000-0000-000000000003',
    'Road Accident & Highway Collision Department',
    'ACCIDENT',
    'Traffic & Highway Safety Coordinator',
    'UP-GOV-GKP-AC',
    '+919876543253',
    'DISTRICT_OFFICER',
    'AVAILABLE',
    35.0,
    26.7450,
    83.2200,
    true,
    'GOV-UP-GOV-GKP-AC',
    'GORAKHPUR'
);

-- 4. MEDICAL_RESPONSE Official
INSERT INTO users (id, email, mobile_number, password, role, profile_completed, created_at, updated_at)
VALUES (
    '30000000-0000-0000-0000-000000000004',
    'official.medical.gkp@dms.gov.in',
    '+919876543254',
    crypt('Official@123', gen_salt('bf', 10)),
    'GOVERNMENT_OFFICIAL',
    true,
    NOW(),
    NOW()
);

INSERT INTO user_profiles (user_id, name, address_line, city, district, state, pincode, latitude, longitude, created_at, updated_at)
VALUES (
    '30000000-0000-0000-0000-000000000004',
    'Dr. Neha Srivastava',
    'Sahajanwa Community Emergency Health Unit',
    'Sahajanwa',
    'Gorakhpur',
    'Uttar Pradesh',
    '273209',
    26.7510,
    83.2050,
    NOW(),
    NOW()
);

INSERT INTO government_registry (id, employee_id, department_name, department_category, designation, official_email, official_phone, authorization_code, hierarchy_level, office_latitude, office_longitude, duty_radius_km, status, created_at, updated_at)
VALUES (
    gen_random_uuid(),
    'UP-GOV-GKP-MR',
    'Emergency Medical Response Unit',
    'MEDICAL_RESPONSE',
    'Emergency Medical Response Lead',
    'official.medical.gkp@dms.gov.in',
    '+919876543254',
    'GOV-GKP-MR01',
    'DISTRICT_OFFICER',
    26.7510,
    83.2050,
    35.0,
    'ACTIVE',
    NOW(),
    NOW()
);

INSERT INTO government_official_profiles (user_id, department_name, department_category, designation, employee_id, official_phone, hierarchy_level, status, duty_radius_km, duty_office_latitude, duty_office_longitude, is_verified, government_profile_id, jurisdiction_code)
VALUES (
    '30000000-0000-0000-0000-000000000004',
    'Emergency Medical Response Unit',
    'MEDICAL_RESPONSE',
    'Emergency Medical Response Lead',
    'UP-GOV-GKP-MR',
    '+919876543254',
    'DISTRICT_OFFICER',
    'AVAILABLE',
    35.0,
    26.7510,
    83.2050,
    true,
    'GOV-UP-GOV-GKP-MR',
    'GORAKHPUR'
);


-- ------------------------------------------------------------------------------
-- E. 5 HOSPITAL USERS (Registered in userDb & linked to hospital_db facilities)
-- Password for all: Hospital@123
-- ------------------------------------------------------------------------------

-- 1. Sahjanwa Hospital
INSERT INTO users (id, email, mobile_number, password, role, profile_completed, created_at, updated_at)
VALUES (
    '40000000-0000-0000-0000-000000000001',
    'hospital.sahajanwa@dms.gov.in',
    '+919876543231',
    crypt('Hospital@123', gen_salt('bf', 10)),
    'HOSPITAL',
    true,
    NOW(),
    NOW()
);

INSERT INTO hospital_registry (id, hospital_name, hospital_email, hospital_phone, authorization_code, employee_id, status, created_at, updated_at)
VALUES (
    gen_random_uuid(),
    'DMS Test - Sahjanwa Hospital',
    'hospital.sahajanwa@dms.gov.in',
    '+919876543231',
    'HOSP-GKP-SHJ01',
    'HOSP-EMP-SHJ01',
    'CONSUMED',
    NOW(),
    NOW()
);

-- 2. BRD Medical College
INSERT INTO users (id, email, mobile_number, password, role, profile_completed, created_at, updated_at)
VALUES (
    '40000000-0000-0000-0000-000000000002',
    'hospital.brd@dms.gov.in',
    '+919876543232',
    crypt('Hospital@123', gen_salt('bf', 10)),
    'HOSPITAL',
    true,
    NOW(),
    NOW()
);

INSERT INTO hospital_registry (id, hospital_name, hospital_email, hospital_phone, authorization_code, employee_id, status, created_at, updated_at)
VALUES (
    gen_random_uuid(),
    'DMS Test - BRD Medical College',
    'hospital.brd@dms.gov.in',
    '+919876543232',
    'HOSP-GKP-BRD01',
    'HOSP-EMP-BRD01',
    'CONSUMED',
    NOW(),
    NOW()
);

-- 3. City General Hospital
INSERT INTO users (id, email, mobile_number, password, role, profile_completed, created_at, updated_at)
VALUES (
    '40000000-0000-0000-0000-000000000003',
    'hospital.city@dms.gov.in',
    '+919876543233',
    crypt('Hospital@123', gen_salt('bf', 10)),
    'HOSPITAL',
    true,
    NOW(),
    NOW()
);

INSERT INTO hospital_registry (id, hospital_name, hospital_email, hospital_phone, authorization_code, employee_id, status, created_at, updated_at)
VALUES (
    gen_random_uuid(),
    'DMS Test - City General Hospital',
    'hospital.city@dms.gov.in',
    '+919876543233',
    'HOSP-GKP-CTY01',
    'HOSP-EMP-CTY01',
    'CONSUMED',
    NOW(),
    NOW()
);

-- 4. Rustampur Trauma Centre
INSERT INTO users (id, email, mobile_number, password, role, profile_completed, created_at, updated_at)
VALUES (
    '40000000-0000-0000-0000-000000000004',
    'hospital.rustampur@dms.gov.in',
    '+919876543234',
    crypt('Hospital@123', gen_salt('bf', 10)),
    'HOSPITAL',
    true,
    NOW(),
    NOW()
);

INSERT INTO hospital_registry (id, hospital_name, hospital_email, hospital_phone, authorization_code, employee_id, status, created_at, updated_at)
VALUES (
    gen_random_uuid(),
    'DMS Test - Rustampur Trauma Centre',
    'hospital.rustampur@dms.gov.in',
    '+919876543234',
    'HOSP-GKP-RST01',
    'HOSP-EMP-RST01',
    'CONSUMED',
    NOW(),
    NOW()
);

-- 5. Gorakhnath Community Hospital
INSERT INTO users (id, email, mobile_number, password, role, profile_completed, created_at, updated_at)
VALUES (
    '40000000-0000-0000-0000-000000000005',
    'hospital.gorakhnath@dms.gov.in',
    '+919876543235',
    crypt('Hospital@123', gen_salt('bf', 10)),
    'HOSPITAL',
    true,
    NOW(),
    NOW()
);

INSERT INTO hospital_registry (id, hospital_name, hospital_email, hospital_phone, authorization_code, employee_id, status, created_at, updated_at)
VALUES (
    gen_random_uuid(),
    'DMS Test - Gorakhnath Community Hospital',
    'hospital.gorakhnath@dms.gov.in',
    '+919876543235',
    'HOSP-GKP-GKN01',
    'HOSP-EMP-GKN01',
    'CONSUMED',
    NOW(),
    NOW()
);
