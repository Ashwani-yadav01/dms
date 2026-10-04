package com.dms.userService.user.service.impl;

import com.dms.userService.user.dto.request.RescueTeamProfileRequest;
import com.dms.userService.user.dto.response.RescueTeamProfileResponse;
import com.dms.userService.user.entity.RescueTeamProfile;
import com.dms.userService.user.entity.User;
import com.dms.userService.user.repository.RescueTeamProfileRepository;
import com.dms.userService.user.repository.UserRepository;
import com.dms.userService.user.service.RescueDepartmentDirectory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.modelmapper.ModelMapper;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RescueTeamProfileServiceImplTest {
    @Mock RescueTeamProfileRepository repository;
    @Mock UserRepository userRepository;
    @Mock ModelMapper mapper;
    @Mock RescueDepartmentDirectory departmentDirectory;
    @InjectMocks RescueTeamProfileServiceImpl service;

    @Test void profileCreationPersistsValidatedTeamFieldsAndCannotSelfVerify() {
        UUID userId = UUID.randomUUID(); RescueTeamProfileRequest request = new RescueTeamProfileRequest();
        request.setName("Team Lead"); request.setAddressLine("Station road"); request.setCity("Lucknow");
        request.setState("UP"); request.setDistrict("Lucknow"); request.setPincode("226001");
        request.setLatitude(26.8); request.setLongitude(80.9); request.setTeamName("Unit Alpha");
        request.setTeamCode("alpha-1"); request.setDepartmentId(UUID.randomUUID());
        request.setTeamLeadName("Lead"); request.setTeamSize(5);
        User user = new User(); RescueTeamProfileResponse response = new RescueTeamProfileResponse();
        when(userRepository.findById(userId)).thenReturn(java.util.Optional.of(user));
        when(repository.existsById(userId)).thenReturn(false);
        when(repository.existsByTeamCode("alpha-1")).thenReturn(false);
        when(departmentDirectory.exists(any())).thenReturn(true);
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(mapper.map(any(RescueTeamProfile.class), eq(RescueTeamProfileResponse.class))).thenReturn(response);

        assertSame(response, service.createProfile(userId, request));
        ArgumentCaptor<RescueTeamProfile> saved = ArgumentCaptor.forClass(RescueTeamProfile.class);
        verify(repository).save(saved.capture());
        assertEquals("ALPHA-1", saved.getValue().getTeamCode());
        assertFalse(saved.getValue().getIsVerified());
    }
}
