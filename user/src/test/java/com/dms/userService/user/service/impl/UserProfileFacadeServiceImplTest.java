package com.dms.userService.user.service.impl;

import com.dms.userService.user.dto.request.CitizenProfileRequest;
import com.dms.userService.user.entity.Role;
import com.dms.userService.user.entity.User;
import com.dms.userService.user.exception.BadRequestException;
import com.dms.userService.user.repository.UserProfileRepository;
import com.dms.userService.user.repository.UserRepository;
import com.dms.userService.user.service.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserProfileFacadeServiceImplTest {
    @Mock UserProfileRepository userProfileRepository;
    @Mock UserRepository userRepository;
    @Mock CitizenProfileService citizenProfileService;
    @Mock VolunteerProfileService volunteerProfileService;
    @Mock NGOProfileService ngoProfileService;
    @Mock GovernmentOfficialProfileService governmentOfficialProfileService;
    @Mock RescueTeamProfileService rescueTeamProfileService;
    @InjectMocks UserProfileFacadeServiceImpl facade;

    @Test void rejectsProfileTypeThatDoesNotMatchPersistedRole() {
        UUID id = UUID.randomUUID(); User user = new User(); user.setRole(Role.RESCUE_TEAM);
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        assertThrows(BadRequestException.class, () -> facade.createProfile(id, new CitizenProfileRequest()));
        verifyNoInteractions(userProfileRepository, citizenProfileService, rescueTeamProfileService);
    }
}
