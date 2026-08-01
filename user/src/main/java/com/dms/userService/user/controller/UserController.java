package com.dms.userService.user.controller;


import com.dms.userService.user.dto.request.UserRequest;
import com.dms.userService.user.dto.response.UserResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/users")
public class UserController {

    @PostMapping
    public ResponseEntity<UserResponse> createUser(@RequestBody UserRequest request) {
        return null;
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable UUID userId) {
        return null;
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<UserResponse> getUserByEmail(@PathVariable String email) {
        return null;
    }

    @GetMapping("/mobile/{mobileNumber}")
    public ResponseEntity<UserResponse> getUserByMobile(@PathVariable String mobileNumber) {
        return null;
    }

    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return null;
    }

    @PutMapping("/{userId}/role")
    public ResponseEntity<UserResponse> updateUserRole(
            @PathVariable UUID userId,
            @RequestParam String role) {
        return null;
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID userId) {
        return null;
    }
}