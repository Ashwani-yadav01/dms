package com.dms.userService.user.service.impl;

import com.dms.userService.user.dto.request.GovernmentOfficialProfileRequest;
import com.dms.userService.user.dto.response.GovernmentOfficialProfileResponse;
import com.dms.userService.user.entity.*;
import com.dms.userService.user.exception.BadRequestException;
import com.dms.userService.user.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.modelmapper.ModelMapper;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GovernmentOfficialProfileServiceImplTest {
    @Mock GovernmentOfficialProfileRepository profileRepository;
    @Mock UserRepository userRepository;
    @Mock GovernmentRegistryRepository registryRepository;
    @Mock ModelMapper mapper;
    @InjectMocks GovernmentOfficialProfileServiceImpl service;

    @Test void activeRegistryMatchCreatesVerifiedProfileWithServerGeneratedIdentifier() {
        UUID userId = UUID.randomUUID(); User user = new User(); user.setRole(Role.GOVERNMENT_OFFICIAL);
        GovernmentOfficialProfileRequest request = request(); GovernmentRegistry registry = registry();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(profileRepository.existsById(userId)).thenReturn(false);
        when(profileRepository.existsByEmployeeId(request.getEmployeeId())).thenReturn(false);
        when(profileRepository.existsByGovernmentProfileId(anyString())).thenReturn(false);
        when(registryRepository.findByEmployeeId(request.getEmployeeId())).thenReturn(Optional.of(registry));
        when(profileRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(registryRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        GovernmentOfficialProfileResponse response = service.createProfile(userId, request);
        assertTrue(response.getIsVerified());
        assertTrue(response.getGovernmentProfileId().matches("GOV-[0-9]{6}-[A-Z2-9]{6}"));
        assertEquals(response.getGovernmentProfileId(), registry.getGovernmentProfileId());
    }

    @Test void profileCreationRejectsWrongCodeOrInactiveRegistry() {
        UUID userId = UUID.randomUUID(); User user = new User(); user.setRole(Role.GOVERNMENT_OFFICIAL);
        GovernmentOfficialProfileRequest request = request(); GovernmentRegistry registry = registry();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(profileRepository.existsById(userId)).thenReturn(false);
        when(profileRepository.existsByEmployeeId(request.getEmployeeId())).thenReturn(false);
        when(profileRepository.existsByGovernmentProfileId(anyString())).thenReturn(false);
        when(registryRepository.findByEmployeeId(request.getEmployeeId())).thenReturn(Optional.of(registry));
        request.setAuthorizationCode("BAD-CODE");
        assertEquals("Government official verification failed.",
                assertThrows(BadRequestException.class, () -> service.createProfile(userId, request)).getMessage());
    }

    private GovernmentOfficialProfileRequest request() {
        GovernmentOfficialProfileRequest r = new GovernmentOfficialProfileRequest();
        r.setName("Test Officer"); r.setAddressLine("1 Main Road"); r.setCity("Lucknow"); r.setState("UP");
        r.setDistrict("Lucknow"); r.setPincode("226001"); r.setLatitude(26.8); r.setLongitude(80.9);
        r.setDepartmentName("Disaster Management"); r.setDepartmentCategory(DepartmentCategory.DISASTER_RESCUE);
        r.setDesignation("District Officer"); r.setEmployeeId("UP-GOV-18492"); r.setHierarchyLevel(HierarchyLevel.DISTRICT_OFFICER);
        r.setAuthorizationCode("GOV-A8F4K2"); return r;
    }
    private GovernmentRegistry registry() {
        GovernmentRegistry r = new GovernmentRegistry(); r.setEmployeeId("UP-GOV-18492");
        r.setDepartmentName("DISASTER MANAGEMENT"); r.setDesignation("District Officer");
        r.setHierarchyLevel(HierarchyLevel.DISTRICT_OFFICER); r.setAuthorizationCode("GOV-A8F4K2");
        r.setOfficeLatitude(26.8467); r.setOfficeLongitude(80.9462); r.setDutyRadiusKm(20.0);
        r.setStatus(GovernmentRegistryStatus.ACTIVE); return r;
    }
}
