package com.dms.userService.user.exception;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

}