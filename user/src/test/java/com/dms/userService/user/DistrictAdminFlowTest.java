package com.dms.userService.user;

import com.dms.userService.user.dto.request.DistrictAdminProfileRequest;
import com.dms.userService.user.dto.request.RegisterRequest;
import com.dms.userService.user.dto.response.DistrictAdminProfileResponse;
import com.dms.userService.user.dto.response.RegisterResponse;
import com.dms.userService.user.entity.DistrictAdminRegistry;
import com.dms.userService.user.entity.DistrictAdminRegistryStatus;
import com.dms.userService.user.entity.Role;
import com.dms.userService.user.exception.BadRequestException;
import com.dms.userService.user.repository.DistrictAdminProfileRepository;
import com.dms.userService.user.repository.DistrictAdminRegistryRepository;
import com.dms.userService.user.repository.UserRepository;
import com.dms.userService.user.service.AuthService;
import com.dms.userService.user.service.UserProfileFacadeService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class DistrictAdminFlowTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private DistrictAdminRegistryRepository registryRepository;

    @Autowired
    private DistrictAdminProfileRepository profileRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserProfileFacadeService profileFacadeService;

    private static final String TEST_EMAIL = "newdistrictadmin@test.gov.in";
    private static final String TEST_MOBILE = "+919112233445";
    private static final String TEST_CODE = "DIST-TESTCODE12";

    @BeforeEach
    @AfterEach
    void cleanUp() {
        userRepository.findByEmail(TEST_EMAIL).ifPresent(user -> {
            profileRepository.deleteById(user.getId());
            userRepository.deleteById(user.getId());
        });
        registryRepository.findByAuthorizationCode(TEST_CODE).ifPresent(reg -> registryRepository.delete(reg));
    }

    @Test
    void testDistrictAdminRegistrationAndProfileCompletion() {
        // 1. Seed active District Admin registry entry (simulating Super Admin generation)
        DistrictAdminRegistry reg = new DistrictAdminRegistry();
        reg.setEmployeeId("DMS-DIST-99998888");
        reg.setAuthorizationCode(TEST_CODE);
        reg.setAdminEmail(TEST_EMAIL);
        reg.setAdminPhone(TEST_MOBILE);
        reg.setDistrictName("Gorakhpur");
        reg.setStatus(DistrictAdminRegistryStatus.ACTIVE);
        registryRepository.save(reg);

        // 2. Attempt registration without code should fail
        RegisterRequest invalidReq = RegisterRequest.builder()
                .email(TEST_EMAIL)
                .mobileNumber(TEST_MOBILE)
                .password("Password@123")
                .role(Role.DISTRICT_ADMIN)
                .authorizationCode(null)
                .build();
        assertThrows(BadRequestException.class, () -> authService.register(invalidReq));

        // 3. Register with correct authorization code
        RegisterRequest validReq = RegisterRequest.builder()
                .email(TEST_EMAIL)
                .mobileNumber(TEST_MOBILE)
                .password("Password@123")
                .role(Role.DISTRICT_ADMIN)
                .authorizationCode(TEST_CODE)
                .build();

        RegisterResponse regResponse = authService.register(validReq);
        assertNotNull(regResponse);
        assertNotNull(regResponse.getAccessToken());
        assertEquals(Role.DISTRICT_ADMIN, regResponse.getRole());
        UUID userId = regResponse.getUserId();

        // 4. Verify code was consumed
        DistrictAdminRegistry updatedReg = registryRepository.findByAuthorizationCode(TEST_CODE).orElseThrow();
        assertEquals(DistrictAdminRegistryStatus.CONSUMED, updatedReg.getStatus());

        // 5. Complete District Admin Profile
        DistrictAdminProfileRequest profileReq = new DistrictAdminProfileRequest();
        profileReq.setName("Shri Rajesh Sharma, IAS");
        profileReq.setAddressLine("Collectorate Campus, Civil Lines");
        profileReq.setCity("Gorakhpur");
        profileReq.setState("Uttar Pradesh");
        profileReq.setDistrict("Gorakhpur");
        profileReq.setPincode("273001");
        profileReq.setLatitude(26.7606);
        profileReq.setLongitude(83.3732);
        profileReq.setDistrictName("Gorakhpur");
        profileReq.setHelplineNumber("1077");

        var createdProfile = profileFacadeService.createProfile(userId, profileReq);
        assertNotNull(createdProfile);
        assertTrue(createdProfile instanceof DistrictAdminProfileResponse);
        DistrictAdminProfileResponse response = (DistrictAdminProfileResponse) createdProfile;
        assertEquals("Gorakhpur", response.getDistrictName());
        assertEquals("1077", response.getHelplineNumber());
        assertEquals("DMS-DIST-99998888", response.getEmployeeId());
        assertEquals(TEST_CODE, response.getVerificationCode());

        // 6. Verify profile retrieval
        var fetchedProfile = profileFacadeService.getProfile(userId);
        assertTrue(fetchedProfile instanceof DistrictAdminProfileResponse);
        DistrictAdminProfileResponse fetchedResponse = (DistrictAdminProfileResponse) fetchedProfile;
        assertEquals("Shri Rajesh Sharma, IAS", fetchedProfile.getName());
        assertEquals("1077", fetchedResponse.getHelplineNumber());
        assertEquals("DMS-DIST-99998888", fetchedResponse.getEmployeeId());
        assertEquals(TEST_CODE, fetchedResponse.getVerificationCode());
    }
}
