package com.eventdriven.auth.exception;


import org.springframework.http.HttpStatus;

public class PasswordResetTokenNotFoundException extends AuthServiceException{
    public PasswordResetTokenNotFoundException() {
        super(
                HttpStatus.NOT_FOUND,
                "PASSWORD_RESET_TOKEN_NOT_FOUND_EXCEPTION",
                "Password reset token not found."
        );
    }
}
