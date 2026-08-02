package com.dms.userService.user.controller;


import com.dms.userService.user.dto.request.RegisterRequest;
import com.dms.userService.user.dto.request.UserRequest;
import com.dms.userService.user.dto.response.RegisterResponse;
import com.dms.userService.user.dto.response.UserResponse;
import com.dms.userService.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @PostMapping
    public ResponseEntity<RegisterResponse> createUser(@RequestBody RegisterRequest request) {
        RegisterResponse user = userService.createUser(request);
        return new ResponseEntity<>(user, HttpStatus.CREATED);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable UUID userId) {
        UserResponse  user=userService.getUserById(userId);
        return  new ResponseEntity<>(user, HttpStatus.OK);
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<UserResponse> getUserByEmail(@PathVariable String email) {
        UserResponse userResponse=userService.getUserByEmail(email);
        return new ResponseEntity<>(userResponse,HttpStatus.FOUND);
    }

    @GetMapping("/mobile/{mobileNumber}")
    public ResponseEntity<UserResponse> getUserByMobile(@PathVariable String mobileNumber) {
        UserResponse userResponse=userService.getUserByMobile(mobileNumber);
        return new ResponseEntity<>(userResponse,HttpStatus.FOUND);
    }

    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return new ResponseEntity<>(userService.getAllUsers(),HttpStatus.OK);
    }

    @PutMapping("/{userId}/role")
    public ResponseEntity<UserResponse> updateUserRole(
            @PathVariable UUID userId,
            @RequestParam String role) {
       UserResponse userResponse=userService.updateUserRole(userId,role);
       return new ResponseEntity<>(userResponse,HttpStatus.OK);
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<String> deleteUser(@PathVariable UUID userId) {
        return new ResponseEntity<>(userService.deleteUser(userId),HttpStatus.OK);
    }
}