package com.dms.userService.user.repository;

import com.dms.userService.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    Optional<User> findByMobileNumber(String mobileNumber);
    Boolean existsByEmail(String email);
    Boolean existsByMobileNumber(String mobileNumber);
    @Query("select u.email from User u where u.id in :userIds")
    List<String> findEmailsByIdIn(@Param("userIds") Collection<UUID> userIds);
}
