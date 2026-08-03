package com.dms.userService.user.controller;

import com.dms.userService.user.dto.request.RegisterRequest;
import com.dms.userService.user.dto.response.RegisterResponse;
import com.dms.userService.user.dto.response.UserResponse;
import com.dms.userService.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

//    @PostMapping
//    public ResponseEntity<RegisterResponse> createUser(@Valid @RequestBody RegisterRequest request) {
//        RegisterResponse user = userService.createUser(request);
//        return new ResponseEntity<>(user, HttpStatus.CREATED);
//    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable UUID userId) {
        return ResponseEntity.ok(userService.getUserById(userId));
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<UserResponse> getUserByEmail(@PathVariable String email) {
        return ResponseEntity.ok(userService.getUserByEmail(email)); // Fixed: 200 OK
    }

    @GetMapping("/mobile/{mobileNumber}")
    public ResponseEntity<UserResponse> getUserByMobile(@PathVariable String mobileNumber) {
        return ResponseEntity.ok(userService.getUserByMobile(mobileNumber)); // Fixed: 200 OK
    }

    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }
// i think this should be in Userprofile controller
//    @PatchMapping("/{userId}/role")
//    public ResponseEntity<UserResponse> updateUserRole(
//            @PathVariable UUID userId,
//            @RequestParam String role) {
//        return ResponseEntity.ok(userService.updateUserRole(userId, role));
//    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID userId) {
        userService.deleteUser(userId);
        return ResponseEntity.noContent().build(); // 204 No Content is standard for DELETE
    }
}