package com.eventdriven.auth.exception;


import org.springframework.http.HttpStatus;

public class InvalidCredentialsException extends AuthServiceException {
    public InvalidCredentialsException() {
        super(
                HttpStatus.UNAUTHORIZED,
                "INVALID_CREDENTIALS",
                "Credentials are invalid."
                );
    }
}
