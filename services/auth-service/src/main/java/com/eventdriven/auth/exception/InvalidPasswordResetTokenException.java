package com.eventdriven.auth.exception;

import org.springframework.http.HttpStatus;

public class InvalidPasswordResetTokenException extends AuthServiceException{
    public InvalidPasswordResetTokenException() {
        super(
                HttpStatus.BAD_REQUEST,
                "INVALID_PASSWORD_RESET_TOKEN_EXCEPTION",
                "Invalid Password Reset Token"
        );

    }
}
