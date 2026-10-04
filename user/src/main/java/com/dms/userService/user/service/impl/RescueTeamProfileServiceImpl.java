package com.dms.userService.user.service.impl;

import com.dms.userService.user.dto.request.RescueTeamProfileRequest;
import com.dms.userService.user.dto.response.RescueTeamProfileResponse;
import com.dms.userService.user.entity.RescueTeamProfile;
import com.dms.userService.user.entity.User;
import com.dms.userService.user.exception.UserAlreadyExistsException;
import com.dms.userService.user.exception.UserNotFoundException;
import com.dms.userService.user.exception.BadRequestException;
import com.dms.userService.user.repository.RescueTeamProfileRepository;
import com.dms.userService.user.repository.UserRepository;
import com.dms.userService.user.service.RescueTeamProfileService;
import com.dms.userService.user.service.RescueDepartmentDirectory;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service @RequiredArgsConstructor
public class RescueTeamProfileServiceImpl implements RescueTeamProfileService {
    private final RescueTeamProfileRepository repository;
    private final UserRepository userRepository;
    private final ModelMapper mapper;
    private final RescueDepartmentDirectory departmentDirectory;

    public boolean existsById(UUID id) { return repository.existsById(id); }

    @Transactional public RescueTeamProfileResponse createProfile(UUID id, RescueTeamProfileRequest r) {
        User user = userRepository.findById(id).orElseThrow(() -> new UserNotFoundException("User not found"));
        if (repository.existsById(id)) throw new UserAlreadyExistsException("Profile already exists");
        if (repository.existsByTeamCode(r.getTeamCode())) throw new UserAlreadyExistsException("Team code already exists");
        if (!departmentDirectory.exists(r.getDepartmentId())) throw new BadRequestException("Rescue department does not exist.");
        RescueTeamProfile p = new RescueTeamProfile(); p.setUser(user); apply(p, r); p.setIsVerified(false);
        user.setProfileCompleted(true);
        return mapper.map(repository.save(p), RescueTeamProfileResponse.class);
    }

    @Transactional(readOnly = true) public RescueTeamProfileResponse getProfile(UUID id) {
        return repository.findById(id).map(p -> mapper.map(p, RescueTeamProfileResponse.class))
                .orElseThrow(() -> new UserNotFoundException("Rescue team profile not found"));
    }

    @Transactional public RescueTeamProfileResponse updateProfile(UUID id, RescueTeamProfileRequest r) {
        RescueTeamProfile p = repository.findById(id).orElseThrow(() -> new UserNotFoundException("Rescue team profile not found"));
        if (!p.getTeamCode().equalsIgnoreCase(r.getTeamCode()) && repository.existsByTeamCode(r.getTeamCode()))
            throw new UserAlreadyExistsException("Team code already exists");
        if (!departmentDirectory.exists(r.getDepartmentId())) throw new BadRequestException("Rescue department does not exist.");
        apply(p, r);
        return mapper.map(repository.save(p), RescueTeamProfileResponse.class);
    }

    @Transactional(readOnly = true) public RescueTeamProfileResponse getProfileForAdmin(UUID id, UUID actorId) {
        requireAdmin(actorId);
        return getProfile(id);
    }

    @Transactional public RescueTeamProfileResponse verifyProfile(UUID id, UUID actorId) {
        requireAdmin(actorId);
        RescueTeamProfile p = repository.findById(id).orElseThrow(() -> new UserNotFoundException("Rescue team profile not found"));
        p.setIsVerified(true);
        return mapper.map(repository.save(p), RescueTeamProfileResponse.class);
    }

    private void requireAdmin(UUID actorId) {
        User actor = userRepository.findById(actorId).orElseThrow(() -> new UserNotFoundException("User not found"));
        if (actor.getRole() != com.dms.userService.user.entity.Role.DISTRICT_ADMIN)
            throw new org.springframework.security.access.AccessDeniedException("You are not authorized to perform this action.");
    }

    private void apply(RescueTeamProfile p, RescueTeamProfileRequest r) {
        p.setName(r.getName()); p.setAddressLine(r.getAddressLine()); p.setCity(r.getCity()); p.setState(r.getState());
        p.setDistrict(r.getDistrict()); p.setPincode(r.getPincode()); p.setLatitude(r.getLatitude()); p.setLongitude(r.getLongitude());
        if (r.getProfilePhotoUrl() != null) p.setProfilePhotoUrl(r.getProfilePhotoUrl());
        p.setTeamName(r.getTeamName()); p.setTeamCode(r.getTeamCode().trim().toUpperCase());
        p.setDepartmentId(r.getDepartmentId()); p.setTeamLeadName(r.getTeamLeadName()); p.setTeamSize(r.getTeamSize());
    }
    @Transactional public void deleteProfile(UUID id) {
        if (!repository.existsById(id)) throw new UserNotFoundException("Rescue team profile not found");
        repository.deleteById(id);
    }
}
