package com.dms.userService.user.service.impl;

import com.dms.userService.user.entity.*;
import com.dms.userService.user.repository.*;
import com.dms.userService.user.service.UserLocationRedisGeoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Idempotent local & test environment data initializer for Gorakhpur & Sahajanwa region.
 * Provisions exactly:
 * - 1 Citizen (Sahajanwa)
 * - 1 Super Admin (Gorakhpur Apex)
 * - All 6 District Admins (Department Management, Medical Management, Medical Response, Fire Safety, Accident, Disaster Management)
 * - 4 Government Officials (Fire Safety, Disaster Management, Accident, Medical Response)
 * - 5 Hospitals (Sahjanwa Hospital, BRD Medical College, City General Hospital, Rustampur Trauma Centre, Gorakhnath Community Hospital)
 */
@Slf4j
@Component
@Profile("!prod")
@RequiredArgsConstructor
public class GorakhpurRegionDevDataInitializer implements ApplicationRunner {

    // Deterministic fixed UUIDs
    public static final UUID SUPER_ADMIN_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    public static final UUID CITIZEN_SAHJANWA_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");

    public static final UUID DISTRICT_ADMIN_ALL_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
    public static final UUID DISTRICT_ADMIN_MED_ID = UUID.fromString("20000000-0000-0000-0000-000000000002");
    public static final UUID DISTRICT_ADMIN_MR_ID  = UUID.fromString("20000000-0000-0000-0000-000000000003");
    public static final UUID DISTRICT_ADMIN_FS_ID  = UUID.fromString("20000000-0000-0000-0000-000000000004");
    public static final UUID DISTRICT_ADMIN_AC_ID  = UUID.fromString("20000000-0000-0000-0000-000000000005");
    public static final UUID DISTRICT_ADMIN_DM_ID  = UUID.fromString("20000000-0000-0000-0000-000000000006");

    public static final UUID GOV_OFFICIAL_FIRE_ID     = UUID.fromString("30000000-0000-0000-0000-000000000001");
    public static final UUID GOV_OFFICIAL_DISASTER_ID = UUID.fromString("30000000-0000-0000-0000-000000000002");
    public static final UUID GOV_OFFICIAL_ACCIDENT_ID = UUID.fromString("30000000-0000-0000-0000-000000000003");
    public static final UUID GOV_OFFICIAL_MEDICAL_ID  = UUID.fromString("30000000-0000-0000-0000-000000000004");

    public static final UUID HOSPITAL_SAHJANWA_USER_ID   = UUID.fromString("40000000-0000-0000-0000-000000000001");
    public static final UUID HOSPITAL_BRD_USER_ID        = UUID.fromString("40000000-0000-0000-0000-000000000002");
    public static final UUID HOSPITAL_CITY_USER_ID       = UUID.fromString("40000000-0000-0000-0000-000000000003");
    public static final UUID HOSPITAL_RUSTAMPUR_USER_ID   = UUID.fromString("40000000-0000-0000-0000-000000000004");
    public static final UUID HOSPITAL_GORAKHNATH_USER_ID = UUID.fromString("40000000-0000-0000-0000-000000000005");

    private final UserRepository userRepository;
    private final CitizenProfileRepository citizenProfileRepository;
    private final DistrictAdminProfileRepository districtAdminProfileRepository;
    private final DistrictAdminRegistryRepository districtAdminRegistryRepository;
    private final GovernmentOfficialProfileRepository governmentOfficialProfileRepository;
    private final GovernmentRegistryRepository governmentRegistryRepository;
    private final HospitalRegistryRepository hospitalRegistryRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserLocationRedisGeoService redisGeoService;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        log.info("Bootstrapping Sahjanwa & Gorakhpur region test fixtures...");

        seedSuperAdmin();
        seedCitizens();
        seedDistrictAdmins();
        seedGovernmentOfficials();
        seedHospitalUsers();

        log.info("Sahjanwa & Gorakhpur test fixtures successfully initialized.");
    }

    private void seedSuperAdmin() {
        User user = findOrCreateUser(SUPER_ADMIN_ID, "superadmin@dms.gov.in", "Admin@123", "+919876543200", Role.SUPER_ADMIN);
        registerGeo(user.getId(), 26.7606, 83.3732);
    }

    private void seedCitizens() {
        // Exactly 1 Citizen located in Sahajanwa
        seedCitizen(CITIZEN_SAHJANWA_ID, "citizen.sahajanwa@dms.gov.in", "Citizen@123", "+919876543210",
                "Ramesh Kumar", "Ward 5, Station Road", "Sahajanwa", "Gorakhpur",
                "Uttar Pradesh", "273209", 26.7483, 83.1931, Gender.MALE, "O_POSITIVE");
    }

    private void seedCitizen(UUID id, String email, String password, String mobile, String name,
                             String address, String city, String district, String state, String pincode,
                             double lat, double lon, Gender gender, String bloodGroup) {
        User user = findOrCreateUser(id, email, password, mobile, Role.CITIZEN);
        if (!citizenProfileRepository.existsById(user.getId())) {
            CitizenProfile profile = new CitizenProfile();
            profile.setUser(user);
            profile.setName(name);
            profile.setAddressLine(address);
            profile.setCity(city);
            profile.setDistrict(district);
            profile.setState(state);
            profile.setPincode(pincode);
            profile.setLatitude(lat);
            profile.setLongitude(lon);
            profile.setGender(gender);
            profile.setDateOfBirth(LocalDate.of(1992, 5, 14));
            profile.setBloodGroup(bloodGroup);
            profile.setOccupation("Private Sector Employee");
            profile.setEmergencyContactNumber("+919876543200");
            citizenProfileRepository.save(profile);
            registerGeo(user.getId(), lat, lon);
        }
    }

    private void seedDistrictAdmins() {
        // 1. Overall Command (DEPARTMENT_MANAGEMENT)
        seedDistrictAdmin(DISTRICT_ADMIN_ALL_ID, "districtadmin.gkp@dms.gov.in", "District@123", "+919876543220",
                "Gorakhpur District Disaster Management Authority", "Gorakhpur", "GORAKHPUR",
                "District Disaster Management Officer (DDMO)", "Disaster Management & District Administration",
                DepartmentCategory.DEPARTMENT_MANAGEMENT, "0551-220011", "DMS-DIST-GKP01", "DIST-GKP-ALL01",
                "DM Office, Civil Lines", "Gorakhpur", "273001", 26.7588, 83.3697);

        // 2. Healthcare Authority (MEDICAL_MANAGEMENT)
        seedDistrictAdmin(DISTRICT_ADMIN_MED_ID, "districtadmin.medical.gkp@dms.gov.in", "District@123", "+919876543221",
                "Chief Medical Officer (CMO) Gorakhpur", "Gorakhpur", "GORAKHPUR",
                "Chief Medical Officer", "District Health & Medical Administration",
                DepartmentCategory.MEDICAL_MANAGEMENT, "0551-220022", "DMS-DIST-GKPMED", "DIST-GKP-MED01",
                "CMO Office, District Hospital Road", "Gorakhpur", "273001", 26.7610, 83.3750);

        // 3. Medical Response & Triage (MEDICAL_RESPONSE)
        seedDistrictAdmin(DISTRICT_ADMIN_MR_ID, "districtadmin.medresp.gkp@dms.gov.in", "District@123", "+919876543222",
                "District Medical Emergency Response Controller", "Gorakhpur", "GORAKHPUR",
                "Emergency Medical Triage Director", "Emergency Casualty & Medical Response Command",
                DepartmentCategory.MEDICAL_RESPONSE, "0551-220033", "DMS-DIST-GKPMR", "DIST-GKP-MR01",
                "Trauma & Emergency Operations Center, Sahajanwa-Gorakhpur Highway", "Sahajanwa", "273209", 26.7520, 83.2800);

        // 4. Fire & Safety Command (FIRE_SAFETY)
        seedDistrictAdmin(DISTRICT_ADMIN_FS_ID, "districtadmin.fire.gkp@dms.gov.in", "District@123", "+919876543223",
                "District Fire & Safety Command Authority", "Gorakhpur", "GORAKHPUR",
                "Divisional Fire Marshal & Incident Commander", "Fire & Emergency Safety Services",
                DepartmentCategory.FIRE_SAFETY, "0551-220044", "DMS-DIST-GKPFS", "DIST-GKP-FS01",
                "District Fire Headquarters, Golghar", "Gorakhpur", "273001", 26.7580, 83.3730);

        // 5. Accident & Highway Collision Command (ACCIDENT)
        seedDistrictAdmin(DISTRICT_ADMIN_AC_ID, "districtadmin.accident.gkp@dms.gov.in", "District@123", "+919876543224",
                "Highway Transit & Collision Response Command", "Gorakhpur", "GORAKHPUR",
                "Chief Traffic & Extrication Commander", "Road Accident & Highway Collision Department",
                DepartmentCategory.ACCIDENT, "0551-220055", "DMS-DIST-GKPAC", "DIST-GKP-AC01",
                "NH-28 Highway Traffic Command Post", "Sahajanwa", "273209", 26.7460, 83.2150);

        // 6. Disaster Rescue & Flood Relief (DISASTER_MANAGEMENT)
        seedDistrictAdmin(DISTRICT_ADMIN_DM_ID, "districtadmin.disaster.gkp@dms.gov.in", "District@123", "+919876543225",
                "Disaster Rescue & Relief Authority Gorakhpur", "Gorakhpur", "GORAKHPUR",
                "Zonal Relief & Flood Operations Commander", "Disaster Rescue & Relief Department",
                DepartmentCategory.DISASTER_MANAGEMENT, "0551-220066", "DMS-DIST-GKPDM", "DIST-GKP-DM01",
                "DEOC Building, Collectorate", "Gorakhpur", "273001", 26.7550, 83.3700);
    }

    private void seedDistrictAdmin(UUID id, String email, String password, String mobile, String name,
                                   String districtName, String jurisdiction, String designation, String department,
                                   DepartmentCategory category, String helpline, String empId, String code,
                                   String address, String city, String pincode, double lat, double lon) {
        if (!districtAdminRegistryRepository.existsByAuthorizationCode(code)) {
            DistrictAdminRegistry registry = new DistrictAdminRegistry();
            registry.setAdminEmail(email);
            registry.setAdminPhone(mobile);
            registry.setDistrictName(districtName);
            registry.setDepartmentCategory(category.name());
            registry.setEmployeeId(empId);
            registry.setAuthorizationCode(code);
            registry.setStatus(DistrictAdminRegistryStatus.CONSUMED);
            districtAdminRegistryRepository.save(registry);
        }

        User user = findOrCreateUser(id, email, password, mobile, Role.DISTRICT_ADMIN);
        if (!districtAdminProfileRepository.existsById(user.getId())) {
            DistrictAdminProfile profile = new DistrictAdminProfile();
            profile.setUser(user);
            profile.setName(name);
            profile.setDistrictName(districtName);
            profile.setJurisdiction(jurisdiction);
            profile.setDesignation(designation);
            profile.setDepartment(department);
            profile.setDepartmentCategory(category);
            profile.setControlRoomHelpline(helpline);
            profile.setHelplineNumber(helpline);
            profile.setEmployeeId(empId);
            profile.setVerificationCode(code);
            profile.setAddressLine(address);
            profile.setCity(city);
            profile.setDistrict(districtName);
            profile.setState("Uttar Pradesh");
            profile.setPincode(pincode);
            profile.setLatitude(lat);
            profile.setLongitude(lon);
            districtAdminProfileRepository.save(profile);
            registerGeo(user.getId(), lat, lon);
        }
    }

    private void seedGovernmentOfficials() {
        // 1. Fire Safety Official (On-Scene Fire & Chemical Hazard Response)
        seedOfficial(GOV_OFFICIAL_FIRE_ID, "official.fire.gkp@dms.gov.in", "Official@123", "+919876543251",
                "Chief Fire Officer Suresh Chandra", "UP-GOV-GKP-FS", "GOV-GKP-FS01", "Fire and Emergency Services",
                DepartmentCategory.FIRE_SAFETY, "Divisional Fire Officer",
                HierarchyLevel.DISTRICT_OFFICER, 35.0, 26.7500, 83.2120, "Fire Post Sahajanwa-Gorakhpur, NH-28", "Sahajanwa");

        // 2. Disaster Management Official (On-Scene Flood & Structural Rescue)
        seedOfficial(GOV_OFFICIAL_DISASTER_ID, "official.disaster.gkp@dms.gov.in", "Official@123", "+919876543252",
                "Dr. Arvind Verma", "UP-GOV-GKP-DM", "GOV-GKP-DM01", "Disaster Rescue & Relief Department",
                DepartmentCategory.DISASTER_MANAGEMENT, "Zonal Emergency Response Officer",
                HierarchyLevel.DISTRICT_OFFICER, 35.0, 26.7490, 83.2080, "Sahajanwa Sector Emergency Post, Ami River Basin", "Sahajanwa");

        // 3. Accident Response Official (On-Scene Road Accident & Highway Extrication)
        seedOfficial(GOV_OFFICIAL_ACCIDENT_ID, "official.accident.gkp@dms.gov.in", "Official@123", "+919876543253",
                "Inspector Manoj Tiwari", "UP-GOV-GKP-AC", "GOV-GKP-AC01", "Road Accident & Highway Collision Department",
                DepartmentCategory.ACCIDENT, "Traffic & Highway Safety Coordinator",
                HierarchyLevel.DISTRICT_OFFICER, 35.0, 26.7450, 83.2200, "Highway Emergency Unit, NH-28 Sahajanwa", "Sahajanwa");

        // 4. Medical Response Official (On-Scene Casualty Care & Triage)
        seedOfficial(GOV_OFFICIAL_MEDICAL_ID, "official.medical.gkp@dms.gov.in", "Official@123", "+919876543254",
                "Dr. Neha Srivastava", "UP-GOV-GKP-MR", "GOV-GKP-MR01", "Emergency Medical Response Unit",
                DepartmentCategory.MEDICAL_RESPONSE, "Emergency Medical Response Lead",
                HierarchyLevel.DISTRICT_OFFICER, 35.0, 26.7510, 83.2050, "Sahajanwa Community Emergency Health Unit", "Sahajanwa");
    }

    private void seedOfficial(UUID id, String email, String password, String mobile, String name,
                              String empId, String code, String deptName, DepartmentCategory category,
                              String designation, HierarchyLevel hierarchy, double radiusKm,
                              double lat, double lon, String address, String city) {
        if (governmentRegistryRepository.findByEmployeeId(empId).isEmpty()) {
            GovernmentRegistry reg = new GovernmentRegistry();
            reg.setEmployeeId(empId);
            reg.setDepartmentName(deptName);
            reg.setDepartmentCategory(category.name());
            reg.setDesignation(designation);
            reg.setOfficialEmail(email);
            reg.setOfficialPhone(mobile);
            reg.setAuthorizationCode(code);
            reg.setHierarchyLevel(hierarchy);
            reg.setOfficeLatitude(lat);
            reg.setOfficeLongitude(lon);
            reg.setDutyRadiusKm(radiusKm);
            reg.setStatus(GovernmentRegistryStatus.ACTIVE);
            governmentRegistryRepository.save(reg);
        }

        User user = findOrCreateUser(id, email, password, mobile, Role.GOVERNMENT_OFFICIAL);
        if (!governmentOfficialProfileRepository.existsById(user.getId())) {
            GovernmentOfficialProfile profile = new GovernmentOfficialProfile();
            profile.setUser(user);
            profile.setName(name);
            profile.setEmployeeId(empId);
            profile.setDepartmentName(deptName);
            profile.setDepartmentCategory(category);
            profile.setDesignation(designation);
            profile.setOfficialPhone(mobile);
            profile.setHierarchyLevel(hierarchy);
            profile.setStatus(OfficialStatus.AVAILABLE);
            profile.setDutyRadiusKm(radiusKm);
            profile.setDutyOfficeLatitude(lat);
            profile.setDutyOfficeLongitude(lon);
            profile.setLatitude(lat);
            profile.setLongitude(lon);
            profile.setIsVerified(true);
            profile.setGovernmentProfileId("GOV-" + empId);
            profile.setJurisdictionCode("GORAKHPUR");
            profile.setAddressLine(address);
            profile.setCity(city);
            profile.setDistrict("Gorakhpur");
            profile.setState("Uttar Pradesh");
            profile.setPincode(city.equalsIgnoreCase("Sahajanwa") ? "273209" : "273001");
            governmentOfficialProfileRepository.save(profile);
            registerGeo(user.getId(), lat, lon);
        }
    }

    private void seedHospitalUsers() {
        // 1. Sahjanwa Hospital Admin
        seedHospitalUser(HOSPITAL_SAHJANWA_USER_ID, "hospital.sahajanwa@dms.gov.in", "Hospital@123",
                "+919876543231", "DMS Test - Sahjanwa Hospital", "HOSP-GKP-SHJ01");

        // 2. BRD Medical College Admin
        seedHospitalUser(HOSPITAL_BRD_USER_ID, "hospital.brd@dms.gov.in", "Hospital@123",
                "+919876543232", "DMS Test - BRD Medical College", "HOSP-GKP-BRD01");

        // 3. City General Hospital Admin
        seedHospitalUser(HOSPITAL_CITY_USER_ID, "hospital.city@dms.gov.in", "Hospital@123",
                "+919876543233", "DMS Test - City General Hospital", "HOSP-GKP-CTY01");

        // 4. Rustampur Trauma Centre Admin
        seedHospitalUser(HOSPITAL_RUSTAMPUR_USER_ID, "hospital.rustampur@dms.gov.in", "Hospital@123",
                "+919876543234", "DMS Test - Rustampur Trauma Centre", "HOSP-GKP-RST01");

        // 5. Gorakhnath Community Hospital Admin
        seedHospitalUser(HOSPITAL_GORAKHNATH_USER_ID, "hospital.gorakhnath@dms.gov.in", "Hospital@123",
                "+919876543235", "DMS Test - Gorakhnath Community Hospital", "HOSP-GKP-GKN01");
    }

    private void seedHospitalUser(UUID id, String email, String password, String mobile,
                                  String hospitalName, String code) {
        if (hospitalRegistryRepository.findByAuthorizationCode(code).isEmpty()) {
            HospitalRegistry reg = new HospitalRegistry();
            reg.setEmployeeId("HOSP-EMP-" + code);
            reg.setHospitalName(hospitalName);
            reg.setAuthorizationCode(code);
            reg.setHospitalEmail(email);
            reg.setHospitalPhone(mobile);
            reg.setStatus(HospitalRegistryStatus.CONSUMED);
            hospitalRegistryRepository.save(reg);
        }

        findOrCreateUser(id, email, password, mobile, Role.HOSPITAL);
    }

    private User findOrCreateUser(UUID deterministicId, String email, String rawPassword, String mobile, Role role) {
        return userRepository.findByEmail(email).orElseGet(() -> {
            User newUser = new User();
            newUser.setId(deterministicId);
            newUser.setEmail(email);
            newUser.setMobileNumber(mobile);
            newUser.setPassword(passwordEncoder.encode(rawPassword));
            newUser.setRole(role);
            newUser.setProfileCompleted(true);
            return userRepository.save(newUser);
        });
    }

    private void registerGeo(UUID id, double lat, double lon) {
        try {
            if (redisGeoService != null) {
                redisGeoService.registerOrUpdateUserLocation(id, lat, lon);
            }
        } catch (Exception e) {
            log.debug("Redis GEO skipped for user {} (Redis may be offline): {}", id, e.getMessage());
        }
    }
}
