package com.dms.userService.user.service.impl;

import com.dms.userService.user.entity.Role;
import com.dms.userService.user.entity.User;
import com.dms.userService.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class SuperAdminAccountInitializer implements ApplicationRunner {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${dms.bootstrap.super-admin.email:super@email.com}")
    private String email;
    @Value("${dms.bootstrap.super-admin.password:super@1000}")
    private String password;
    @Value("${dms.bootstrap.super-admin.mobile:+919999000000}")
    private String mobile;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        User existing = userRepository.findByEmail(email).orElse(null);
        if (existing != null) {
            if (existing.getRole() != Role.SUPER_ADMIN)
                throw new IllegalStateException("Configured super-admin email belongs to a non-admin account.");
            return;
        }
        if (userRepository.existsByMobileNumber(mobile))
            throw new IllegalStateException("Configured super-admin mobile number is already in use.");

        User admin = new User();
        admin.setEmail(email);
        admin.setMobileNumber(mobile);
        admin.setPassword(passwordEncoder.encode(password));
        admin.setRole(Role.SUPER_ADMIN);
        admin.setProfileCompleted(true);
        userRepository.save(admin);
    }
}
