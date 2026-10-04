package com.dms.userService.user.service.impl;


import com.dms.userService.user.dto.response.UserResponse;
import com.dms.userService.user.entity.User;
import com.dms.userService.user.exception.UserNotFoundException;
import com.dms.userService.user.repository.UserProfileRepository;
import com.dms.userService.user.repository.UserRepository;
import com.dms.userService.user.service.UserService;
import com.dms.userService.user.service.UserLocationRedisGeoService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.ArrayList;
import java.util.UUID;
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final ModelMapper mapper;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserLocationRedisGeoService userLocationGeoService;
//    @Override
//    public RegisterResponse createUser( RegisterRequest request) {
//        if (userRepository.existsByEmail(request.getEmail())) {
//            throw new UserAlreadyExistsException(
//                    "User with email " + request.getEmail() + " already exists.");
//        }
//
//        if (userRepository.existsByMobileNumber(request.getMobileNumber())) {
//            throw new UserAlreadyExistsException(
//                    "User with mobile number " + request.getMobileNumber() + " already exists.");
//        }
//
//        User user = mapper.map(request, User.class);
//        user.setProfileCompleted(false);
//
//        user = userRepository.save(user);
//
//        return mapper.map(user,RegisterResponse.class);
//    }

    @Override
    public UserResponse getUserById(UUID userId) {
        User user=userRepository.findById(userId).orElseThrow(()->new UserNotFoundException("User does not exists"+userId));
        return mapper.map(user,UserResponse.class);
    }

    @Override
    public UserResponse getUserByEmail(String email) {
        User user=userRepository.findByEmail(email).orElseThrow(()->new UserNotFoundException("User does not exists"+email));
        return mapper.map(user,UserResponse.class);
    }

    @Override
    public UserResponse getUserByMobile(String mobileNumber) {
        User user=userRepository.findByMobileNumber(mobileNumber).orElseThrow(()->new UserNotFoundException("User not found "));
        return mapper.map(user,UserResponse.class);
    }

    @Override
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream().map(user -> mapper.map(user,UserResponse.class)).toList();
    }

    @Override
    public List<String> findEmailsWithinRadius(Double lat, Double lon, Double radiusKm) {
        List<UUID> nearbyUserIds = userLocationGeoService.findUserIdsWithinRadius(lat, lon, radiusKm);
        if (nearbyUserIds.isEmpty()) return List.of();

        // Keep each PostgreSQL IN query bounded while avoiding a request per recipient.
        List<String> emails = new ArrayList<>();
        final int batchSize = 500;
        for (int start = 0; start < nearbyUserIds.size(); start += batchSize) {
            int end = Math.min(start + batchSize, nearbyUserIds.size());
            emails.addAll(userRepository.findEmailsByIdIn(nearbyUserIds.subList(start, end)));
        }
        return emails;
    }

//    @Override
//    public UserResponse updateUserRole(UUID userId, String role) {
//        User user=userRepository.findById(userId).orElseThrow(()->new UserNotFoundException("User does not exists"+userId));
//
//        user.setRole(Role.valueOf(role));
//        return mapper.map(user,UserResponse.class);
//    }

    @Override
    public void deleteUser(UUID userId) {
        userRepository.deleteById(userId);
        userLocationGeoService.removeUserLocation(userId);
        return ;
    }
}
