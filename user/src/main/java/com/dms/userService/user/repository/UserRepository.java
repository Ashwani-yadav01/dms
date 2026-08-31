package com.dms.userService.user.repository;

import com.dms.userService.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    Optional<User> findByMobileNumber(String mobileNumber);
    Boolean existsByEmail(String email);
    Boolean existsByMobileNumber(String mobileNumber);
    @Query(value = """
        SELECT u.email FROM users u
        JOIN user_profiles up ON u.id = up.user_id
        WHERE up.latitude IS NOT NULL 
          AND up.longitude IS NOT NULL
          AND (
            6371 * acos(
              cos(radians(:latitude)) * cos(radians(up.latitude)) *
              cos(radians(up.longitude) - radians(:longitude)) +
              sin(radians(:latitude)) * sin(radians(up.latitude))
            )
          ) <= :radiusKm
    """, nativeQuery = true)
    List<String> findEmailsWithinRadius(
            @Param("latitude") Double latitude,
            @Param("longitude") Double longitude,
            @Param("radiusKm") Double radiusKm
    );
}
