package com.dms.userService.user.repository;

import com.dms.userService.user.dto.response.NGOProfileResponse;
import com.dms.userService.user.entity.NGOProfile;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface NGOProfileRepository extends JpaRepository<NGOProfile, UUID> {
    Optional<NGOProfile> findByRegistrationNumber(String registrationNumber);

    boolean existsByRegistrationNumber(String registrationNumber);

}
