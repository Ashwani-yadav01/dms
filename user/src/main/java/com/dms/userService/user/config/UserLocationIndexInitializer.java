package com.dms.userService.user.config;

import com.dms.userService.user.entity.UserProfile;
import com.dms.userService.user.repository.UserProfileRepository;
import com.dms.userService.user.service.UserLocationRedisGeoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/** Warms the Redis location index from PostgreSQL when the User service starts. */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserLocationIndexInitializer implements CommandLineRunner {
    private final UserProfileRepository userProfileRepository;
    private final UserLocationRedisGeoService userLocationGeoService;

    @Override
    public void run(String... args) {
        int indexed = 0;
        for (UserProfile profile : userProfileRepository.findAll()) {
            if (profile.getLatitude() != null && profile.getLongitude() != null) {
                userLocationGeoService.registerOrUpdateUserLocation(
                        profile.getId(), profile.getLatitude(), profile.getLongitude());
                indexed++;
            }
        }
        log.info("Initialized users:locations Redis GEO index from PostgreSQL: {} profiles indexed", indexed);
    }
}
