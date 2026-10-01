package com.eventdriven.auth.exception;

import org.springframework.http.HttpStatus;

public class UserNotFoundException extends AuthServiceException{
    public UserNotFoundException() {
        super(
                HttpStatus.NOT_FOUND,
                "USER_NOT_FOUND",
                "User not found. Please register."
        );
    }
}
