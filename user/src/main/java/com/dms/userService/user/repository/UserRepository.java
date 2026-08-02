package com.dms.userService.user.repository;

import com.dms.userService.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    Optional<User> findByMobileNumber(String mobileNumber);
    Boolean existsByEmail(String email);
    Boolean existsByMobileNumber(String mobileNumber);
}
