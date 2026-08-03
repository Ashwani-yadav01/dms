package com.dms.userService.user.service.impl;

import com.dms.userService.user.dto.response.GovernmentOfficialProfileResponse;
import com.dms.userService.user.entity.DepartmentCategory;
import com.dms.userService.user.entity.GovernmentOfficialProfile;
import com.dms.userService.user.entity.HierarchyLevel;
import com.dms.userService.user.entity.OfficialStatus;
import com.dms.userService.user.exception.ResourceNotFoundException;
import com.dms.userService.user.exception.UserNotFoundException;
import com.dms.userService.user.repository.GovernmentOfficialProfileRepository;
import com.dms.userService.user.service.GovernmentOfficialDomainService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GovernmentOfficialDomainServiceImpl implements GovernmentOfficialDomainService {

    private final GovernmentOfficialProfileRepository officialProfileRepository;
    private final ModelMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public GovernmentOfficialProfileResponse getOfficialByEmployeeId(String employeeId) {
        GovernmentOfficialProfile profile = officialProfileRepository.findByEmployeeId(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Official not found with employee ID: " + employeeId));

        return mapToResponse(profile);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GovernmentOfficialProfileResponse> getOfficialsByDepartmentName(String departmentName) {
        String normalizedDept = departmentName.trim().toUpperCase();
        return officialProfileRepository.findByDepartmentName(normalizedDept)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<GovernmentOfficialProfileResponse> getOfficialsByHierarchy(HierarchyLevel hierarchyLevel) {
        return officialProfileRepository.findByHierarchyLevel(hierarchyLevel)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public GovernmentOfficialProfileResponse getSupervisor(UUID userId) {
        GovernmentOfficialProfile profile = officialProfileRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Official profile not found for user id: " + userId));

        if (profile.getReportsTo() == null) {
            throw new ResourceNotFoundException("No supervisor assigned to official with id: " + userId);
        }

        return mapToResponse(profile.getReportsTo());
    }

    @Override
    @Transactional(readOnly = true)
    public List<GovernmentOfficialProfileResponse> getSubordinates(UUID userId) {
        if (!officialProfileRepository.existsById(userId)) {
            throw new UserNotFoundException("Official profile not found for user id: " + userId);
        }

        return officialProfileRepository.findByReportsToId(userId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getDepartmentSuggestions() {
        return officialProfileRepository.findDistinctDepartmentNames();
    }

    @Override
    @Transactional
    public GovernmentOfficialProfileResponse updateOfficialStatus(UUID userId, OfficialStatus status) {
        GovernmentOfficialProfile profile = officialProfileRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Official profile not found for user id: " + userId));

        profile.setStatus(status);
        GovernmentOfficialProfile updatedProfile = officialProfileRepository.save(profile);
        return mapToResponse(updatedProfile);
    }

    @Override
    @Transactional
    public GovernmentOfficialProfileResponse verifyOfficial(UUID userId, boolean isVerified) {
        GovernmentOfficialProfile profile = officialProfileRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Official profile not found for user id: " + userId));

        profile.setIsVerified(isVerified);
        GovernmentOfficialProfile updatedProfile = officialProfileRepository.save(profile);
        return mapToResponse(updatedProfile);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GovernmentOfficialProfileResponse> findEligibleOfficials(
            OfficialStatus status,
            DepartmentCategory category,
            HierarchyLevel level) {

        return officialProfileRepository.findEligibleOfficialsForAllocation(status, category, level)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private GovernmentOfficialProfileResponse mapToResponse(GovernmentOfficialProfile profile) {
        GovernmentOfficialProfileResponse response = mapper.map(profile, GovernmentOfficialProfileResponse.class);
        if (profile.getReportsTo() != null) {
            response.setReportsToUserId(profile.getReportsTo().getId());
        }
        return response;
    }
}